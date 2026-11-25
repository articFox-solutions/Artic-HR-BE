package com.articfox.hr.usuario;

/** Roles del Incremento 1 (DERCAS 1.05, sección 5). */
public enum Rol {
    CA("Colaborador Administrativo"),
    AHR("Agente de Recursos Humanos"),
    ADM("Administrador de HR");

    private final String nombre;

    Rol(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
