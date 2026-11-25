package com.articfox.hr.config;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.SessionIdGenerator;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

@Configuration(proxyBeanMethods = false)
class SesionConfig {

    /** Usa el generador de 256 bits para crear y rotar los identificadores de sesión. */
    @Bean
    SessionRepositoryCustomizer<JdbcIndexedSessionRepository> generadorIdSesion() {
        return repositorio -> repositorio.setSessionIdGenerator(new GeneradorIdSesion());
    }

    /**
     * Identificador opaco de 256 bits (DERCAS 9.2). El generador por defecto usa un UUID, que
     * solo tiene 122 bits aleatorios.
     */
    static final class GeneradorIdSesion implements SessionIdGenerator {

        private static final SecureRandom ALEATORIO = new SecureRandom();
        private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

        @Override
        public String generate() {
            byte[] bytes = new byte[32];
            ALEATORIO.nextBytes(bytes);
            return BASE64_URL.encodeToString(bytes);
        }
    }
}
