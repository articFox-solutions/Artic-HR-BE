package com.articfox.hr.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.articfox.hr.TestcontainersConfiguration;
import com.articfox.hr.usuario.UsuarioService;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Criterios de aceptación del DERCAS 1.05 (sección 7), de punta a punta: HTTP real, filtros de
 * seguridad, Spring Session y PostgreSQL 18.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginIntegrationTest {

    private static final String CONTRASENA = "Contrasena-Correcta-2026";
    private static final AtomicInteger SIGUIENTE_IP = new AtomicInteger(10);
    private static String hashContrasena;

    @LocalServerPort
    int puerto;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    LimiteFallosPorIp limitePorIp;

    @Autowired
    UsuarioService usuarioService;

    private UUID departamentoId;

    @BeforeEach
    void prepararDatos() {
        jdbc.execute("TRUNCATE auditoria_login, usuario, colaborador, departamento, "
                + "spring_session_attributes, spring_session CASCADE");
        limitePorIp.reiniciar();
        if (hashContrasena == null) {
            hashContrasena = passwordEncoder.encode(CONTRASENA);
        }
        departamentoId = UUID.randomUUID();
        jdbc.update("INSERT INTO departamento (id, nombre) VALUES (?, 'Recursos Humanos')", departamentoId);
    }

    // RF-01, RF-02, RF-05, RF-07, RNF-02 y RNF-06
    @Test
    void loginValidoCreaSesionSeguraYMuestraElPerfil() {
        UUID usuarioId = crearUsuario("ana.morales@artichr.test", "ADM");
        ClienteHttp cliente = nuevoCliente();

        HttpResponse<String> login = cliente.login("Ana.Morales@ArticHR.test", CONTRASENA);

        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(texto(login, "$.nombre")).isEqualTo("Persona ana.morales@artichr.test");
        assertThat(texto(login, "$.puesto")).isEqualTo("Puesto de prueba");
        assertThat(texto(login, "$.departamento")).isEqualTo("Recursos Humanos");
        assertThat(texto(login, "$.rol.codigo")).isEqualTo("ADM");
        assertThat(texto(login, "$.rol.nombre")).isEqualTo("Administrador de HR");

        String cookieSesion = setCookie(login, "SESSION");
        assertThat(cookieSesion).contains("HttpOnly").contains("Secure").contains("SameSite=Lax");

        String idSesion = jdbc.queryForObject("SELECT session_id FROM spring_session WHERE principal_name = ?",
                String.class, usuarioId.toString());
        assertThat(idSesion).hasSize(43);

        HttpResponse<String> me = cliente.get("/api/me");
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.headers().firstValue("Cache-Control")).hasValueSatisfying(v -> assertThat(v).contains("no-store"));

        HttpResponse<String> menu = cliente.get("/api/menu");
        assertThat(menu.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<List<String>>read(menu.body(), "$.opciones[*].id")).containsExactly("inicio");

        assertThat(motivosAuditados()).containsExactly("EXITOSO");
        assertThat(jdbc.queryForObject("SELECT ip FROM auditoria_login", String.class)).startsWith("203.0.113.");
    }

    // RF-03: correo inexistente y contraseña incorrecta responden lo mismo
    @Test
    void correoInexistenteYContrasenaIncorrectaDanElMismoMensaje() {
        crearUsuario("luis.herrera@artichr.test", "AHR");

        HttpResponse<String> inexistente = nuevoCliente().login("nadie@artichr.test", CONTRASENA);
        HttpResponse<String> incorrecta = nuevoCliente().login("luis.herrera@artichr.test", "otra-contrasena-123");

        for (HttpResponse<String> respuesta : List.of(inexistente, incorrecta)) {
            assertThat(respuesta.statusCode()).isEqualTo(401);
            assertThat(texto(respuesta, "$.codigo")).isEqualTo("CREDENCIALES_INVALIDAS");
            assertThat(texto(respuesta, "$.detail")).isEqualTo(LoginService.MENSAJE_GENERICO);
        }
        assertThat(motivosAuditados()).containsExactlyInAnyOrder("USUARIO_NO_EXISTE", "CONTRASENA_INCORRECTA");
    }

    @Test
    void usuarioOColaboradorDesactivadoRecibeElMensajeGenerico() {
        crearUsuario("inactivo@artichr.test", "CA", false, "ACTIVO", false);
        crearUsuario("baja@artichr.test", "CA", true, "INACTIVO", false);

        HttpResponse<String> usuarioInactivo = nuevoCliente().login("inactivo@artichr.test", CONTRASENA);
        HttpResponse<String> colaboradorInactivo = nuevoCliente().login("baja@artichr.test", CONTRASENA);

        for (HttpResponse<String> respuesta : List.of(usuarioInactivo, colaboradorInactivo)) {
            assertThat(respuesta.statusCode()).isEqualTo(401);
            assertThat(texto(respuesta, "$.detail")).isEqualTo(LoginService.MENSAJE_GENERICO);
        }
        assertThat(motivosAuditados()).containsExactlyInAnyOrder("USUARIO_INACTIVO", "COLABORADOR_INACTIVO");
    }

    @Test
    void cincoFallosBloqueanLaCuentaQuinceMinutos() {
        UUID usuarioId = crearUsuario("maria.cifuentes@artichr.test", "AHR");
        ClienteHttp cliente = nuevoCliente();
        for (int i = 0; i < 5; i++) {
            assertThat(cliente.login("maria.cifuentes@artichr.test", "incorrecta-" + i).statusCode()).isEqualTo(401);
        }

        HttpResponse<String> conContrasenaCorrecta = cliente.login("maria.cifuentes@artichr.test", CONTRASENA);

        assertThat(conContrasenaCorrecta.statusCode()).isEqualTo(401);
        assertThat(texto(conContrasenaCorrecta, "$.detail")).isEqualTo(LoginService.MENSAJE_GENERICO);
        Instant bloqueadoHasta = jdbc.queryForObject("SELECT bloqueado_hasta FROM usuario WHERE id = ?",
                Timestamp.class, usuarioId).toInstant();
        assertThat(bloqueadoHasta).isBetween(Instant.now().plus(14, ChronoUnit.MINUTES),
                Instant.now().plus(16, ChronoUnit.MINUTES));
        assertThat(motivosAuditados()).endsWith("CUENTA_BLOQUEADA");

        // Pasados los 15 minutos vuelve a entrar
        jdbc.update("UPDATE usuario SET bloqueado_hasta = now() - interval '1 second' WHERE id = ?", usuarioId);
        assertThat(nuevoCliente().login("maria.cifuentes@artichr.test", CONTRASENA).statusCode()).isEqualTo(200);
    }

    @Test
    void excesoDeFallosDesdeUnaIpLaRechazaSinBloquearLaCuenta() {
        UUID usuarioId = crearUsuario("diego.perez@artichr.test", "AHR");
        ClienteHttp atacante = nuevoCliente();
        for (int i = 0; i < 20; i++) {
            atacante.login("no-existe-" + i + "@artichr.test", "lo-que-sea-123");
        }

        HttpResponse<String> desdeIpLimitada = atacante.login("diego.perez@artichr.test", CONTRASENA);
        HttpResponse<String> desdeOtraIp = nuevoCliente().login("diego.perez@artichr.test", CONTRASENA);

        assertThat(desdeIpLimitada.statusCode()).isEqualTo(429);
        assertThat(texto(desdeIpLimitada, "$.codigo")).isEqualTo("DEMASIADOS_INTENTOS");
        assertThat(desdeOtraIp.statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT intentos_fallidos FROM usuario WHERE id = ?", Integer.class, usuarioId))
                .isZero();
    }

    // RF-08
    @Test
    void contrasenaTemporalExigeCambioAntesDeEntrar() {
        crearUsuario("sofia.ramirez@artichr.test", "CA", true, "ACTIVO", true);
        ClienteHttp cliente = nuevoCliente();

        HttpResponse<String> login = cliente.login("sofia.ramirez@artichr.test", CONTRASENA);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<Boolean>read(login.body(), "$.debeCambiarContrasena")).isTrue();

        HttpResponse<String> menu = cliente.get("/api/menu");
        assertThat(menu.statusCode()).isEqualTo(403);
        assertThat(texto(menu, "$.codigo")).isEqualTo("CAMBIO_CONTRASENA_REQUERIDO");
        assertThat(cliente.get("/api/me").statusCode()).isEqualTo(200);

        HttpResponse<String> corta = cambiarContrasena(cliente, CONTRASENA, "corta");
        assertThat(corta.statusCode()).isEqualTo(400);
        assertThat(texto(corta, "$.errores.contrasenaNueva")).contains("12");

        HttpResponse<String> actualIncorrecta = cambiarContrasena(cliente, "no-es-la-actual", "Nueva-Contrasena-2026");
        assertThat(texto(actualIncorrecta, "$.codigo")).isEqualTo("CONTRASENA_ACTUAL_INCORRECTA");

        String sesionAntes = cliente.cookie("SESSION");
        assertThat(cambiarContrasena(cliente, CONTRASENA, "Nueva-Contrasena-2026").statusCode()).isEqualTo(204);
        assertThat(cliente.cookie("SESSION")).isNotEqualTo(sesionAntes);

        assertThat(cliente.get("/api/menu").statusCode()).isEqualTo(200);
        assertThat(nuevoCliente().login("sofia.ramirez@artichr.test", "Nueva-Contrasena-2026").statusCode())
                .isEqualTo(200);
    }

    // RF-06
    @Test
    void sinSesionResponde401() {
        HttpResponse<String> me = nuevoCliente().get("/api/me");

        assertThat(me.statusCode()).isEqualTo(401);
        assertThat(texto(me, "$.codigo")).isEqualTo("NO_AUTENTICADO");
    }

    @Test
    void treintaMinutosDeInactividadCierranLaSesion() {
        UUID usuarioId = crearUsuario("andrea.solis@artichr.test", "AHR");
        ClienteHttp cliente = nuevoCliente();
        cliente.login("andrea.solis@artichr.test", CONTRASENA);

        long treintaYUnMinutos = 31L * 60 * 1000;
        jdbc.update("UPDATE spring_session SET last_access_time = last_access_time - ?, expiry_time = expiry_time - ? "
                + "WHERE principal_name = ?", treintaYUnMinutos, treintaYUnMinutos, usuarioId.toString());

        assertThat(cliente.get("/api/me").statusCode()).isEqualTo(401);
    }

    @Test
    void ochoHorasDesdeElLoginCierranLaSesionAunqueHayaActividad() {
        UUID usuarioId = crearUsuario("roberto.aguilar@artichr.test", "AHR");
        ClienteHttp cliente = nuevoCliente();
        cliente.login("roberto.aguilar@artichr.test", CONTRASENA);

        long ochoHorasYUnMinuto = (8L * 60 + 1) * 60 * 1000;
        jdbc.update("UPDATE spring_session SET creation_time = creation_time - ? WHERE principal_name = ?",
                ochoHorasYUnMinuto, usuarioId.toString());

        HttpResponse<String> me = cliente.get("/api/me");
        assertThat(me.statusCode()).isEqualTo(401);
        assertThat(texto(me, "$.codigo")).isEqualTo("SESION_EXPIRADA");
        assertThat(sesionesDe(usuarioId)).isZero();
    }

    @Test
    void usuarioDesactivadoConSesionRecibe401EnLaSiguientePeticion() {
        UUID usuarioId = crearUsuario("paola.mendez@artichr.test", "AHR");
        ClienteHttp cliente = nuevoCliente();
        cliente.login("paola.mendez@artichr.test", CONTRASENA);

        jdbc.update("UPDATE usuario SET activo = false WHERE id = ?", usuarioId);

        HttpResponse<String> me = cliente.get("/api/me");
        assertThat(me.statusCode()).isEqualTo(401);
        assertThat(texto(me, "$.codigo")).isEqualTo("SESION_INVALIDA");
    }

    // DERCAS 9.2: la baja borra todas las sesiones por índice de principal
    @Test
    void bajaDeUsuarioBorraTodasSusSesiones() {
        UUID usuarioId = crearUsuario("hector.juarez@artichr.test", "AHR");
        ClienteHttp navegador = nuevoCliente();
        ClienteHttp celular = nuevoCliente();
        navegador.login("hector.juarez@artichr.test", CONTRASENA);
        celular.login("hector.juarez@artichr.test", CONTRASENA);
        assertThat(sesionesDe(usuarioId)).isEqualTo(2);

        usuarioService.desactivar(usuarioId);

        assertThat(sesionesDe(usuarioId)).isZero();
        assertThat(navegador.get("/api/me").statusCode()).isEqualTo(401);
        assertThat(celular.get("/api/me").statusCode()).isEqualTo(401);
    }

    // RF-04
    @Test
    void logoutInvalidaLaSesionYBorraLaCookie() {
        crearUsuario("javier.monterroso@artichr.test", "AHR");
        ClienteHttp cliente = nuevoCliente();
        cliente.login("javier.monterroso@artichr.test", CONTRASENA);
        String sesionAnterior = cliente.cookie("SESSION");

        HttpResponse<String> logout = cliente.post("/api/auth/logout", "");

        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(setCookie(logout, "SESSION")).containsIgnoringCase("Max-Age=0");
        assertThat(cliente.cookie("SESSION")).isNull();

        // Aunque alguien conserve la cookie vieja, ya no sirve
        cliente.ponerCookie("SESSION", sesionAnterior);
        assertThat(cliente.get("/api/me").statusCode()).isEqualTo(401);
    }

    @Test
    void elLoginCreaUnIdDeSesionNuevo() {
        crearUsuario("gabriela.estrada@artichr.test", "AHR");
        ClienteHttp cliente = nuevoCliente();
        cliente.login("gabriela.estrada@artichr.test", CONTRASENA);
        String primera = cliente.cookie("SESSION");

        cliente.login("gabriela.estrada@artichr.test", CONTRASENA);

        assertThat(cliente.cookie("SESSION")).isNotNull().isNotEqualTo(primera);
    }

    @Test
    void peticionDeCambioSinTokenCsrfResponde403() {
        crearUsuario("kevin.mazariegos@artichr.test", "CA");

        ClienteHttp sinCsrf = nuevoCliente().sinEncabezadoCsrf();
        HttpResponse<String> login = sinCsrf.login("kevin.mazariegos@artichr.test", CONTRASENA);
        assertThat(login.statusCode()).isEqualTo(403);
        assertThat(texto(login, "$.codigo")).isEqualTo("CSRF_INVALIDO");

        ClienteHttp conSesion = nuevoCliente();
        conSesion.login("kevin.mazariegos@artichr.test", CONTRASENA);
        HttpResponse<String> logoutSinCsrf = conSesion.sinEncabezadoCsrf().post("/api/auth/logout", "");
        assertThat(logoutSinCsrf.statusCode()).isEqualTo(403);
    }

    // RF-01: validación de formato
    @Test
    void correoConFormatoInvalidoResponde400PorCampo() {
        HttpResponse<String> respuesta = nuevoCliente().login("no-es-un-correo", "algo");

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(texto(respuesta, "$.codigo")).isEqualTo("VALIDACION");
        assertThat(texto(respuesta, "$.errores.correo")).isEqualTo("Ingresa un correo válido.");
    }

    @Test
    void healthEsPublico() {
        HttpResponse<String> health = nuevoCliente().get("/api/health");

        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(texto(health, "$.status")).isEqualTo("UP");
    }

    private ClienteHttp nuevoCliente() {
        return new ClienteHttp(puerto, "203.0.113." + SIGUIENTE_IP.getAndIncrement());
    }

    private HttpResponse<String> cambiarContrasena(ClienteHttp cliente, String actual, String nueva) {
        return cliente.post("/api/auth/cambiar-contrasena",
                "{\"contrasenaActual\":\"" + actual + "\",\"contrasenaNueva\":\"" + nueva + "\"}");
    }

    private UUID crearUsuario(String correo, String rol) {
        return crearUsuario(correo, rol, true, "ACTIVO", false);
    }

    private UUID crearUsuario(String correo, String rol, boolean activo, String estadoColaborador,
            boolean debeCambiar) {
        UUID colaboradorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO colaborador (id, codigo_empleado, nombre, correo_empresarial, puesto, departamento_id, estado)
                VALUES (?, ?, ?, ?, 'Puesto de prueba', ?, ?)
                """, colaboradorId, "EMP-" + colaboradorId.toString().substring(0, 8), "Persona " + correo, correo,
                departamentoId, estadoColaborador);
        jdbc.update("""
                INSERT INTO usuario (id, colaborador_id, rol, activo, password_hash, debe_cambiar_contrasena)
                VALUES (?, ?, ?, ?, ?, ?)
                """, usuarioId, colaboradorId, rol, activo, hashContrasena, debeCambiar);
        return usuarioId;
    }

    private List<String> motivosAuditados() {
        return jdbc.queryForList("SELECT motivo FROM auditoria_login ORDER BY creado_en", String.class);
    }

    private int sesionesDe(UUID usuarioId) {
        return jdbc.queryForObject("SELECT count(*) FROM spring_session WHERE principal_name = ?", Integer.class,
                usuarioId.toString());
    }

    private static String texto(HttpResponse<String> respuesta, String ruta) {
        return JsonPath.read(respuesta.body(), ruta);
    }

    private static String setCookie(HttpResponse<String> respuesta, String nombre) {
        return respuesta.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith(nombre + "="))
                .reduce((primera, ultima) -> ultima)
                .orElseThrow(() -> new AssertionError("No llegó la cookie " + nombre));
    }
}
