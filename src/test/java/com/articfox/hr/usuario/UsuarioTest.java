package com.articfox.hr.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Reglas de bloqueo sin Spring ni base de datos: son lógica pura del dominio. */
class UsuarioTest {

    private static final Instant AHORA = Instant.parse("2026-11-24T15:00:00Z");
    private static final Duration QUINCE_MINUTOS = Duration.ofMinutes(15);

    @Test
    void bloqueaAlQuintoFalloYReiniciaElContador() {
        Usuario usuario = new Usuario();
        for (int i = 0; i < 4; i++) {
            usuario.registrarFallo(AHORA, 5, QUINCE_MINUTOS);
        }
        assertThat(usuario.estaBloqueado(AHORA)).isFalse();

        usuario.registrarFallo(AHORA, 5, QUINCE_MINUTOS);

        assertThat(usuario.estaBloqueado(AHORA)).isTrue();
        assertThat(usuario.getBloqueadoHasta()).isEqualTo(AHORA.plus(QUINCE_MINUTOS));
        assertThat(usuario.getIntentosFallidos()).isZero();
    }

    @Test
    void elBloqueoTerminaALos15Minutos() {
        Usuario usuario = new Usuario();
        for (int i = 0; i < 5; i++) {
            usuario.registrarFallo(AHORA, 5, QUINCE_MINUTOS);
        }

        assertThat(usuario.estaBloqueado(AHORA.plus(QUINCE_MINUTOS).minusSeconds(1))).isTrue();
        assertThat(usuario.estaBloqueado(AHORA.plus(QUINCE_MINUTOS))).isFalse();
    }

    @Test
    void unAccesoExitosoLimpiaFallosYBloqueo() {
        Usuario usuario = new Usuario();
        usuario.registrarFallo(AHORA, 5, QUINCE_MINUTOS);

        usuario.registrarAccesoExitoso(AHORA);

        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getBloqueadoHasta()).isNull();
        assertThat(usuario.getUltimoAcceso()).isEqualTo(AHORA);
    }
}
