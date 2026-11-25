package com.articfox.hr;

import java.util.stream.Stream;
import org.springframework.boot.SpringApplication;

/**
 * Arranca el backend con un PostgreSQL temporal de Testcontainers y los usuarios demo, sin
 * compose.yaml ni .env. Útil para quien solo necesita la API para trabajar el frontend.
 * También enciende Swagger UI. Desde IntelliJ: ejecuta este main. Los datos se pierden al detenerlo.
 */
public class TestArticHrApplication {

	public static void main(String[] args) {
		SpringApplication.from(ArticHrApplication::main)
				.with(TestcontainersConfiguration.class)
				.run(Stream.concat(Stream.of("--spring.flyway.locations=classpath:db/migration,classpath:db/dev",
						"--springdoc.api-docs.enabled=true", "--springdoc.swagger-ui.enabled=true"),
						Stream.of(args)).toArray(String[]::new));
	}

}
