package com.articfox.hr.perfil;

import com.articfox.hr.auth.SesionUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Perfil", description = "Datos del usuario con sesión")
@RestController
class PerfilController {

    private final PerfilService perfilService;

    PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    /** Angular lo llama al cargar: 200 indica sesión válida y 401 indica ir al login. */
    @Operation(summary = "Perfil del usuario (RF-05)",
            description = "Angular lo llama al cargar la app: 200 indica sesión válida y 401 indica ir al login.")
    @ApiResponse(responseCode = "200", description = "Sesión válida")
    @ApiResponse(responseCode = "401", description = "NO_AUTENTICADO, SESION_EXPIRADA o SESION_INVALIDA",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/api/me")
    PerfilResponse me(@AuthenticationPrincipal SesionUsuario usuario) {
        return perfilService.obtener(usuario.usuarioId());
    }
}
