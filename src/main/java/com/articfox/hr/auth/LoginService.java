package com.articfox.hr.auth;

import com.articfox.hr.auditoria.AuditoriaLoginService;
import com.articfox.hr.auditoria.MotivoLogin;
import com.articfox.hr.config.LoginProperties;
import com.articfox.hr.usuario.EstadoColaborador;
import com.articfox.hr.usuario.Usuario;
import com.articfox.hr.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Valida credenciales y aplica las reglas de acceso del RF-02. */
@Service
public class LoginService {

    public static final String MENSAJE_GENERICO = "Correo o contraseña incorrectos. Si continúa, contacta a IT.";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaLoginService auditoria;
    private final LimiteFallosPorIp limitePorIp;
    private final LoginProperties properties;
    private final Clock clock;

    /**
     * Hash señuelo: se verifica contra él cuando el correo no existe o la cuenta está bloqueada,
     * para que esas respuestas tarden lo mismo que una contraseña incorrecta y no revelen cuentas.
     */
    private final String hashSenuelo;

    public LoginService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, AuditoriaLoginService auditoria,
            LimiteFallosPorIp limitePorIp, LoginProperties properties, Clock clock) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.auditoria = auditoria;
        this.limitePorIp = limitePorIp;
        this.properties = properties;
        this.clock = clock;
        this.hashSenuelo = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public ResultadoLogin autenticar(String correo, String contrasena, String ip, String userAgent) {
        Instant ahora = clock.instant();
        String correoNormalizado = correo.trim().toLowerCase(Locale.ROOT);

        if (limitePorIp.estaLimitada(ip, ahora)) {
            auditoria.registrar(null, correoNormalizado, MotivoLogin.LIMITE_IP, ip, userAgent);
            return ResultadoLogin.limiteIp();
        }

        Optional<Usuario> encontrado = usuarios.findByCorreo(correoNormalizado);
        if (encontrado.isEmpty()) {
            passwordEncoder.matches(contrasena, hashSenuelo);
            return fallo(null, correoNormalizado, MotivoLogin.USUARIO_NO_EXISTE, ip, userAgent, ahora);
        }

        Usuario usuario = encontrado.get();
        if (usuario.estaBloqueado(ahora)) {
            passwordEncoder.matches(contrasena, hashSenuelo);
            return fallo(usuario, correoNormalizado, MotivoLogin.CUENTA_BLOQUEADA, ip, userAgent, ahora);
        }

        // La contraseña se verifica antes que el estado, para no revelar qué cuentas están inactivas
        if (!passwordEncoder.matches(contrasena, usuario.getPasswordHash())) {
            usuario.registrarFallo(ahora, properties.maxIntentos(), properties.duracionBloqueo());
            return fallo(usuario, correoNormalizado, MotivoLogin.CONTRASENA_INCORRECTA, ip, userAgent, ahora);
        }
        if (!usuario.isActivo()) {
            return fallo(usuario, correoNormalizado, MotivoLogin.USUARIO_INACTIVO, ip, userAgent, ahora);
        }
        if (usuario.getColaborador().getEstado() != EstadoColaborador.ACTIVO) {
            return fallo(usuario, correoNormalizado, MotivoLogin.COLABORADOR_INACTIVO, ip, userAgent, ahora);
        }

        usuario.registrarAccesoExitoso(ahora);
        auditoria.registrar(usuario.getId(), correoNormalizado, MotivoLogin.EXITOSO, ip, userAgent);
        return ResultadoLogin.exitoso(new SesionUsuario(usuario.getId(), usuario.getRol()));
    }

    private ResultadoLogin fallo(Usuario usuario, String correo, MotivoLogin motivo, String ip, String userAgent,
            Instant ahora) {
        limitePorIp.registrarFallo(ip, ahora);
        auditoria.registrar(usuario == null ? null : usuario.getId(), correo, motivo, ip, userAgent);
        return ResultadoLogin.fallido();
    }
}
