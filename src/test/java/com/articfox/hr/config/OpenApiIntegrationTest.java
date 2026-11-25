package com.articfox.hr.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.articfox.hr.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/** Swagger UI solo existe cuando se enciende springdoc, como hacen el perfil local y test-run. */
class OpenApiIntegrationTest {

    private static HttpResponse<String> get(int puerto, String ruta) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Nested
    @Import(TestcontainersConfiguration.class)
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
            properties = {"springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
    class Encendido {

        @LocalServerPort
        int puerto;

        @Test
        void documentaLosEndpointsSinPedirSesion() throws Exception {
            HttpResponse<String> docs = get(puerto, "/v3/api-docs");

            assertThat(docs.statusCode()).isEqualTo(200);
            Map<String, Object> rutas = JsonPath.read(docs.body(), "$.paths");
            assertThat(rutas).containsKeys("/api/auth/csrf", "/api/auth/login", "/api/auth/logout",
                    "/api/auth/cambiar-contrasena", "/api/me", "/api/menu", "/api/health");
            // El CsrfToken que inyecta Spring no debe aparecer como parámetro para llenar
            Map<String, Object> operacionCsrf = JsonPath.read(docs.body(), "$.paths['/api/auth/csrf'].get");
            assertThat(operacionCsrf).doesNotContainKey("parameters");
            // Login es público; /api/me usa la cookie de sesión
            List<Object> seguridadLogin = JsonPath.read(docs.body(), "$.paths['/api/auth/login'].post.security");
            assertThat(seguridadLogin).isEmpty();
            assertThat(JsonPath.<String>read(docs.body(), "$.components.securitySchemes.sesion.name"))
                    .isEqualTo("SESSION");
        }

        @Test
        void sirveSwaggerUi() throws Exception {
            HttpResponse<String> pagina = get(puerto, "/swagger-ui/index.html");

            assertThat(pagina.statusCode()).isEqualTo(200);
            assertThat(pagina.body()).contains("swagger-ui");
        }
    }

    @Nested
    @Import(TestcontainersConfiguration.class)
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    class ApagadoPorDefecto {

        @LocalServerPort
        int puerto;

        @Test
        void noPublicaLaDocumentacion() throws Exception {
            assertThat(get(puerto, "/v3/api-docs").statusCode()).isEqualTo(404);
            assertThat(get(puerto, "/swagger-ui/index.html").statusCode()).isEqualTo(404);
        }
    }
}
