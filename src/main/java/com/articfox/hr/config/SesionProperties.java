package com.articfox.hr.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Tope absoluto de la sesión desde el login (DERCAS RNF-02). La inactividad la maneja Spring Session. */
@ConfigurationProperties("artichr.sesion")
public record SesionProperties(Duration duracionMaxima) {
}
