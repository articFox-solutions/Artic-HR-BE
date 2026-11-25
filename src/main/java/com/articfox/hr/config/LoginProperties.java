package com.articfox.hr.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Reglas de bloqueo y límite por IP (DERCAS RNF-01 y 9.3), configurables en application.properties. */
@ConfigurationProperties("artichr.login")
public record LoginProperties(int maxIntentos, Duration duracionBloqueo, LimiteIp limiteIp) {

    public record LimiteIp(int maxFallos, Duration ventana) {
    }
}
