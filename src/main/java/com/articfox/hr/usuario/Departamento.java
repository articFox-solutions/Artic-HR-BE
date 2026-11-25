package com.articfox.hr.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "departamento")
public class Departamento {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    protected Departamento() {
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
