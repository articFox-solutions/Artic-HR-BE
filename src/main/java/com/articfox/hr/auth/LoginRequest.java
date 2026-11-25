package com.articfox.hr.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** RF-01: correo y contraseña con validación de formato. */
public record LoginRequest(
        @Schema(example = "ana.morales@artichr.test")
        @NotBlank(message = "Ingresa tu correo.")
        @Email(message = "Ingresa un correo válido.")
        @Size(max = 254, message = "El correo es demasiado largo.")
        String correo,

        @Schema(example = "ArticHR-Demo-2026!")
        @NotBlank(message = "Ingresa tu contraseña.")
        @Size(max = 128, message = "La contraseña es demasiado larga.")
        String contrasena) {
}
