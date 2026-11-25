package com.articfox.hr.auth;

import com.articfox.hr.usuario.Rol;
import java.io.Serializable;
import java.util.UUID;
import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * Lo único que se guarda del usuario en la sesión. El nombre del principal es el id del usuario:
 * Spring Session lo indexa en PRINCIPAL_NAME para poder borrar todas sus sesiones.
 */
public record SesionUsuario(UUID usuarioId, Rol rol) implements AuthenticatedPrincipal, Serializable {

    @Override
    public String getName() {
        return usuarioId.toString();
    }
}
