package com.articfox.hr.usuario;

/** Lo mínimo que se revisa en cada petición autenticada. */
public record EstadoAcceso(boolean activo, EstadoColaborador estadoColaborador, boolean debeCambiarContrasena) {

    public boolean permiteAcceso() {
        return activo && estadoColaborador == EstadoColaborador.ACTIVO;
    }
}
