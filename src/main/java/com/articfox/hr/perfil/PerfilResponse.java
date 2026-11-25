package com.articfox.hr.perfil;

/** Datos de la página de inicio (RF-05). */
public record PerfilResponse(
        String nombre,
        String correo,
        String puesto,
        String departamento,
        RolResponse rol,
        boolean debeCambiarContrasena) {

    public record RolResponse(String codigo, String nombre) {
    }
}
