package com.articfox.hr.auth;

import com.articfox.hr.common.ProblemaHttp;
import com.articfox.hr.config.SesionProperties;
import com.articfox.hr.usuario.EstadoAcceso;
import com.articfox.hr.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Revisa en cada petición autenticada lo que Spring Session no cubre (RNF-02 y DERCAS 9.2):
 * <ul>
 *   <li>Tope absoluto de 8 horas desde el login, aunque haya actividad.</li>
 *   <li>Usuario o colaborador desactivado: 401 en la siguiente petición.</li>
 *   <li>Contraseña temporal: solo deja usar lo necesario para cambiarla (RF-08).</li>
 * </ul>
 * Se crea dentro de SecurityConfig y no como bean, para que Spring Boot no lo registre también
 * como filtro de servlet y corra dos veces.
 */
public class SesionValidaFilter extends OncePerRequestFilter {

    private static final Set<String> PERMITIDAS_CON_CONTRASENA_TEMPORAL = Set.of(
            "GET /api/me",
            "GET /api/auth/csrf",
            "POST /api/auth/cambiar-contrasena",
            "POST /api/auth/logout",
            "GET /api/health");

    private final UsuarioRepository usuarios;
    private final Duration duracionMaxima;
    private final Clock clock;

    public SesionValidaFilter(UsuarioRepository usuarios, SesionProperties properties, Clock clock) {
        this.usuarios = usuarios;
        this.duracionMaxima = properties.duracionMaxima();
        this.clock = clock;
    }

    /** Solo revisa la API. Swagger UI y sus archivos estáticos no dependen del estado de la sesión. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return !ruta.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        HttpSession sesion = request.getSession(false);
        if (autenticacion == null || !(autenticacion.getPrincipal() instanceof SesionUsuario usuario)
                || sesion == null) {
            chain.doFilter(request, response);
            return;
        }

        Instant inicio = Instant.ofEpochMilli(sesion.getCreationTime());
        if (!inicio.plus(duracionMaxima).isAfter(clock.instant())) {
            cerrar(sesion);
            ProblemaHttp.escribir(response, HttpStatus.UNAUTHORIZED, "SESION_EXPIRADA",
                    "Tu sesión expiró. Inicia sesión otra vez.");
            return;
        }

        Optional<EstadoAcceso> estado = usuarios.findEstadoAcceso(usuario.usuarioId());
        if (estado.isEmpty() || !estado.get().permiteAcceso()) {
            cerrar(sesion);
            ProblemaHttp.escribir(response, HttpStatus.UNAUTHORIZED, "SESION_INVALIDA",
                    "Tu sesión ya no es válida. Inicia sesión otra vez.");
            return;
        }

        if (estado.get().debeCambiarContrasena() && !permitidaConContrasenaTemporal(request)) {
            ProblemaHttp.escribir(response, HttpStatus.FORBIDDEN, "CAMBIO_CONTRASENA_REQUERIDO",
                    "Debes cambiar tu contraseña temporal antes de continuar.");
            return;
        }

        chain.doFilter(request, response);
    }

    private static boolean permitidaConContrasenaTemporal(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return PERMITIDAS_CON_CONTRASENA_TEMPORAL.contains(request.getMethod() + " " + ruta);
    }

    private static void cerrar(HttpSession sesion) {
        sesion.invalidate();
        SecurityContextHolder.clearContext();
    }
}
