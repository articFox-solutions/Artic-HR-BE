package com.articfox.hr.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Cuenta de acceso de un colaborador. Las reglas de bloqueo viven aquí para probarlas sin Spring. */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colaborador_id")
    private Colaborador colaborador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "debe_cambiar_contrasena", nullable = false)
    private boolean debeCambiarContrasena;

    @Column(name = "ultimo_acceso")
    private Instant ultimoAcceso;

    protected Usuario() {
    }

    public boolean estaBloqueado(Instant ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    /** Cuenta un fallo y bloquea la cuenta al llegar al máximo. El contador reinicia al bloquear. */
    public void registrarFallo(Instant ahora, int maxIntentos, Duration duracionBloqueo) {
        intentosFallidos++;
        if (intentosFallidos >= maxIntentos) {
            bloqueadoHasta = ahora.plus(duracionBloqueo);
            intentosFallidos = 0;
        }
    }

    public void registrarAccesoExitoso(Instant ahora) {
        intentosFallidos = 0;
        bloqueadoHasta = null;
        ultimoAcceso = ahora;
    }

    public void cambiarContrasena(String nuevoHash) {
        passwordHash = nuevoHash;
        debeCambiarContrasena = false;
    }

    public void desactivar() {
        activo = false;
    }

    public UUID getId() {
        return id;
    }

    public Colaborador getColaborador() {
        return colaborador;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public boolean isDebeCambiarContrasena() {
        return debeCambiarContrasena;
    }

    public Instant getUltimoAcceso() {
        return ultimoAcceso;
    }
}
