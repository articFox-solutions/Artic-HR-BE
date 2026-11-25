package com.articfox.hr.config;

import com.articfox.hr.auth.SesionValidaFilter;
import com.articfox.hr.common.ProblemaHttp;
import com.articfox.hr.usuario.UsuarioRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    /** Argon2id con 19 MiB, 2 iteraciones y paralelismo 1 (DERCAS 9.3, recomendación OWASP). */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Cookie XSRF-TOKEN legible por JavaScript, que es lo que espera HttpClient de Angular
     * (DERCAS 9.8). El token no es secreto frente al propio sitio: protege porque otro sitio no
     * puede leerlo.
     */
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repositorio = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repositorio.setCookiePath("/");
        repositorio.setCookieCustomizer(cookie -> cookie.secure(true).sameSite("Lax"));
        return repositorio;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfTokenRepository csrfTokenRepository,
            SecurityContextRepository securityContextRepository, UsuarioRepository usuarios,
            SesionProperties sesionProperties, Clock clock) throws Exception {
        http
                .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                // El login es un endpoint JSON propio, no el formulario de Spring Security
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/health", "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Swagger: solo responde si springdoc está encendido (perfil local)
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs",
                                "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                // Una API responde 401 o 403 en JSON. La redirección al login la hace Angular (RF-06).
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((request, response, ex) -> ProblemaHttp.escribir(response,
                                HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", "Inicia sesión para continuar."))
                        .accessDeniedHandler((request, response, ex) -> {
                            if (ex instanceof CsrfException) {
                                ProblemaHttp.escribir(response, HttpStatus.FORBIDDEN, "CSRF_INVALIDO",
                                        "La petición no tiene un token CSRF válido. Recarga la página.");
                            } else {
                                ProblemaHttp.escribir(response, HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                                        "No tienes permiso para esta acción.");
                            }
                        }))
                .addFilterBefore(new SesionValidaFilter(usuarios, sesionProperties, clock), AuthorizationFilter.class);
        return http.build();
    }
}
