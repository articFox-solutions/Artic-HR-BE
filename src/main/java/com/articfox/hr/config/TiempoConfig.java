package com.articfox.hr.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class TiempoConfig {

    /** Inyectar el reloj permite probar bloqueos y expiraciones sin esperar. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
