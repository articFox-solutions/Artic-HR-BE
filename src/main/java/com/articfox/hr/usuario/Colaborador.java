package com.articfox.hr.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "colaborador")
public class Colaborador {

    @Id
    private UUID id;

    @Column(name = "codigo_empleado", nullable = false)
    private String codigoEmpleado;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "correo_empresarial", nullable = false)
    private String correoEmpresarial;

    @Column(nullable = false)
    private String puesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "departamento_id")
    private Departamento departamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoColaborador estado;

    protected Colaborador() {
    }

    public UUID getId() {
        return id;
    }

    public String getCodigoEmpleado() {
        return codigoEmpleado;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreoEmpresarial() {
        return correoEmpresarial;
    }

    public String getPuesto() {
        return puesto;
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public EstadoColaborador getEstado() {
        return estado;
    }
}
