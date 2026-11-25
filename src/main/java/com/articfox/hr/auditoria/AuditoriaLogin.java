package com.articfox.hr.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auditoria_login")
public class AuditoriaLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "correo_intentado", nullable = false)
    private String correoIntentado;

    @Column(nullable = false)
    private boolean exitoso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoLogin motivo;

    @Column(nullable = false)
    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected AuditoriaLogin() {
    }

    public AuditoriaLogin(UUID usuarioId, String correoIntentado, MotivoLogin motivo, String ip, String userAgent,
            Instant creadoEn) {
        this.usuarioId = usuarioId;
        this.correoIntentado = correoIntentado;
        this.exitoso = motivo == MotivoLogin.EXITOSO;
        this.motivo = motivo;
        this.ip = ip;
        this.userAgent = userAgent;
        this.creadoEn = creadoEn;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getCorreoIntentado() {
        return correoIntentado;
    }

    public boolean isExitoso() {
        return exitoso;
    }

    public MotivoLogin getMotivo() {
        return motivo;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
