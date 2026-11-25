package com.articfox.hr.auth;

import com.articfox.hr.common.ErrorApi;
import com.articfox.hr.perfil.PerfilResponse;
import com.articfox.hr.perfil.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticación", description = "Login, logout, CSRF y cambio de contraseña")
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final LoginService loginService;
    private final CambioContrasenaService cambioContrasenaService;
    private final PerfilService perfilService;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;

    AuthController(LoginService loginService, CambioContrasenaService cambioContrasenaService,
            PerfilService perfilService, SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository) {
        this.loginService = loginService;
        this.cambioContrasenaService = cambioContrasenaService;
        this.perfilService = perfilService;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    /** Entrega la cookie XSRF-TOKEN. Angular la llama una vez al iniciar, antes del login. */
    @Operation(summary = "Obtener token CSRF",
            description = "Público. Deja la cookie XSRF-TOKEN. Llámalo al iniciar la app, antes del login.")
    @ApiResponse(responseCode = "204", description = "Cookie XSRF-TOKEN entregada")
    @SecurityRequirements
    @GetMapping("/csrf")
    ResponseEntity<Void> csrf(@Parameter(hidden = true) CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Iniciar sesión (RF-01, RF-02, RF-03)", description = """
            Público, pero requiere CSRF. Crea la sesión y devuelve el perfil. Si \
            `debeCambiarContrasena` es `true`, Angular debe llevar al cambio de contraseña. El \
            mensaje de error es el mismo si el correo no existe, la contraseña es incorrecta, la \
            cuenta está bloqueada o el usuario está desactivado.""")
    @ApiResponse(responseCode = "200", description = "Sesión creada")
    @ApiResponse(responseCode = "400", description = "VALIDACION", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "CREDENCIALES_INVALIDAS", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "CSRF_INVALIDO", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "429", description = "DEMASIADOS_INTENTOS", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @SecurityRequirements
    @PostMapping("/login")
    ResponseEntity<PerfilResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
            HttpServletResponse response) {
        ResultadoLogin resultado = loginService.autenticar(body.correo(), body.contrasena(), request.getRemoteAddr(),
                request.getHeader("User-Agent"));
        return switch (resultado.tipo()) {
            case LIMITE_IP -> throw new ErrorApi(HttpStatus.TOO_MANY_REQUESTS, "DEMASIADOS_INTENTOS",
                    LoginService.MENSAJE_GENERICO);
            case FALLIDO -> throw new ErrorApi(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
                    LoginService.MENSAJE_GENERICO);
            case EXITOSO -> {
                iniciarSesion(resultado.sesion(), request, response);
                yield ResponseEntity.ok(perfilService.obtener(resultado.sesion().usuarioId()));
            }
        };
    }

    @Operation(summary = "Cerrar sesión (RF-04)",
            description = "Requiere CSRF. Invalida la sesión y borra la cookie. Responde 204 aunque no hubiera sesión.")
    @ApiResponse(responseCode = "204", description = "Sesión cerrada")
    @ApiResponse(responseCode = "403", description = "CSRF_INVALIDO", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @SecurityRequirements
    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        SecurityContextHolder.clearContext();
        rotarTokenCsrf(request, response);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cambiar contraseña (RF-08)", description = """
            Requiere sesión y CSRF. Lo puede usar quien tiene contraseña temporal. La nueva debe \
            tener entre 12 y 128 caracteres. Cambia el identificador de sesión.""")
    @ApiResponse(responseCode = "204", description = "Contraseña cambiada")
    @ApiResponse(responseCode = "400",
            description = "VALIDACION, CONTRASENA_ACTUAL_INCORRECTA o CONTRASENA_REPETIDA", content = @Content(
                    mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "CSRF_INVALIDO", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/cambiar-contrasena")
    ResponseEntity<Void> cambiarContrasena(@AuthenticationPrincipal SesionUsuario usuario,
            @Valid @RequestBody CambioContrasenaRequest body, HttpServletRequest request) {
        cambioContrasenaService.cambiar(usuario.usuarioId(), body.contrasenaActual(), body.contrasenaNueva());
        request.changeSessionId();
        return ResponseEntity.noContent().build();
    }

    /**
     * Crea una sesión nueva en lugar de reutilizar la anterior, para evitar fijación de sesión.
     * También rota el token CSRF, que ya no debe servir con la nueva identidad.
     */
    private void iniciarSesion(SesionUsuario usuario, HttpServletRequest request, HttpServletResponse response) {
        HttpSession anterior = request.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        request.getSession(true);

        var autenticacion = UsernamePasswordAuthenticationToken.authenticated(usuario, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name())));
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);
        securityContextRepository.saveContext(contexto, request, response);
        rotarTokenCsrf(request, response);
    }

    private void rotarTokenCsrf(HttpServletRequest request, HttpServletResponse response) {
        csrfTokenRepository.saveToken(csrfTokenRepository.generateToken(request), request, response);
    }
}
