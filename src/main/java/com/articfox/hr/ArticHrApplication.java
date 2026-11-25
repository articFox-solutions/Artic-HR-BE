package com.articfox.hr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/*
 * Se excluye el usuario en memoria que Spring Boot crea por defecto: el login propio valida
 * contra la base de datos y no usa UserDetailsService.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class ArticHrApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArticHrApplication.class, args);
	}

}
