package com.articfox.hr.usuario;

import java.util.UUID;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final FindByIndexNameSessionRepository<? extends Session> sesiones;

    public UsuarioService(UsuarioRepository usuarios, FindByIndexNameSessionRepository<? extends Session> sesiones) {
        this.usuarios = usuarios;
        this.sesiones = sesiones;
    }

    /**
     * Baja de usuario (DERCAS 9.2): desactiva la cuenta y borra todas sus sesiones usando el
     * índice de principal de Spring Session. El filtro de sesión cubre además cualquier
     * petición que llegue mientras tanto.
     */
    @Transactional
    public void desactivar(UUID usuarioId) {
        usuarios.findById(usuarioId).ifPresent(Usuario::desactivar);
        sesiones.findByPrincipalName(usuarioId.toString()).keySet().forEach(sesiones::deleteById);
    }
}
