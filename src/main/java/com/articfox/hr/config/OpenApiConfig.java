package com.articfox.hr.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Datos generales del documento OpenAPI que muestra Swagger UI. Los endpoints se describen con
 * anotaciones en cada controlador. El contrato completo, con la tabla de errores, está en
 * docs/api-login.md.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    /** Nombre del esquema de seguridad que usan los endpoints que requieren sesión. */
    public static final String SESION = "sesion";

    private static final String DESCRIPCION = """
            API del Portal Dinámico de HR, Incremento 1: login y página de inicio (DERCAS 1.05).

            **Cómo probarla desde aquí**

            1. Ejecuta `GET /api/auth/csrf`. Deja la cookie `XSRF-TOKEN`, que Swagger UI envía \
            sola en el encabezado `X-XSRF-TOKEN`.
            2. Ejecuta `POST /api/auth/login` con una cuenta demo. El navegador guarda la cookie \
            `SESSION` y desde ahí los demás endpoints responden.
            3. `POST /api/auth/logout` cierra la sesión.

            Todas las cuentas demo usan la contraseña `ArticHR-Demo-2026!`, por ejemplo \
            `ana.morales@artichr.test` (ADM).

            **Errores.** Siguen Problem Details (RFC 9457) con un campo `codigo` estable. Angular \
            debe decidir por `codigo` y `status`, nunca por el texto de `detail`.
            """;

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Artic-HR API")
                        .version("Incremento 1")
                        .description(DESCRIPCION))
                .components(new Components().addSecuritySchemes(SESION, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("SESSION")
                        .description("Cookie de sesión HttpOnly. La crea el login y el navegador "
                                + "la envía sola: no hace falta escribirla aquí.")))
                .addSecurityItem(new SecurityRequirement().addList(SESION));
    }
}
