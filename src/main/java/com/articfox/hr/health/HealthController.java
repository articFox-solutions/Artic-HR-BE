package com.articfox.hr.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Salud", description = "Estado del servicio")
@RestController
@RequestMapping("/api")
class HealthController {

    @Operation(summary = "Estado del backend", description = "Público. Responde UP si la API está arriba.")
    @SecurityRequirements
    @GetMapping("/health")
    HealthResponse health() {
        return new HealthResponse("UP");
    }

    record HealthResponse(String status) {
    }
}
