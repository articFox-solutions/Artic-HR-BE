package com.articfox.hr.menu;

import com.articfox.hr.auth.SesionUsuario;
import com.articfox.hr.usuario.Rol;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** RF-07: el servidor decide qué opciones ve cada rol. En el Incremento 1 todos ven solo Inicio. */
@Tag(name = "Menú", description = "Opciones de navegación según el rol")
@RestController
class MenuController {

    private static final OpcionMenu INICIO = new OpcionMenu("inicio", "menu.inicio", "Inicio", "/inicio", "home");

    private static final Map<Rol, List<OpcionMenu>> OPCIONES_POR_ROL = Map.of(
            Rol.CA, List.of(INICIO),
            Rol.AHR, List.of(INICIO),
            Rol.ADM, List.of(INICIO));

    @Operation(summary = "Menú del usuario (RF-07)",
            description = "El servidor decide qué opciones ve cada rol. En el Incremento 1 todos ven solo Inicio.")
    @ApiResponse(responseCode = "200", description = "Opciones del menú")
    @ApiResponse(responseCode = "401", description = "Sin sesión", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "CAMBIO_CONTRASENA_REQUERIDO", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/api/menu")
    MenuResponse menu(@AuthenticationPrincipal SesionUsuario usuario) {
        return new MenuResponse(OPCIONES_POR_ROL.getOrDefault(usuario.rol(), List.of()));
    }

    /**
     * {@code clave} es la llave de traducción para Transloco y {@code etiqueta} el texto en
     * español por defecto.
     */
    record OpcionMenu(
            @Schema(example = "inicio") String id,
            @Schema(description = "Llave de Transloco", example = "menu.inicio") String clave,
            @Schema(description = "Texto en español por defecto", example = "Inicio") String etiqueta,
            @Schema(example = "/inicio") String ruta,
            @Schema(example = "home") String icono) {
    }

    record MenuResponse(List<OpcionMenu> opciones) {
    }
}
