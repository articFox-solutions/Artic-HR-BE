package com.articfox.hr.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambioContrasenaRequest(
        @Schema(example = "ArticHR-Demo-2026!")
        @NotBlank(message = "Ingresa tu contraseña actual.")
        @Size(max = 128, message = "La contraseña es demasiado larga.")
        String contrasenaActual,

        /* RNF-01: mínimo 12 caracteres. El máximo evita hashes costosos con entradas enormes. */
        @Schema(example = "Mi-Nueva-Clave-2026", minLength = 12, maxLength = 128)
        @NotBlank(message = "Ingresa tu nueva contraseña.")
        @Size(min = 12, max = 128, message = "La nueva contraseña debe tener entre 12 y 128 caracteres.")
        String contrasenaNueva) {
}
