package com.articfox.hr.perfil;

import com.articfox.hr.common.ErrorApi;
import com.articfox.hr.usuario.Colaborador;
import com.articfox.hr.usuario.Usuario;
import com.articfox.hr.usuario.UsuarioRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilService {

    private final UsuarioRepository usuarios;

    public PerfilService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public PerfilResponse obtener(UUID usuarioId) {
        Usuario usuario = usuarios.findConPerfilById(usuarioId)
                .orElseThrow(() -> new ErrorApi(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                        "Inicia sesión para continuar."));
        Colaborador colaborador = usuario.getColaborador();
        return new PerfilResponse(
                colaborador.getNombre(),
                colaborador.getCorreoEmpresarial(),
                colaborador.getPuesto(),
                colaborador.getDepartamento().getNombre(),
                new PerfilResponse.RolResponse(usuario.getRol().name(), usuario.getRol().getNombre()),
                usuario.isDebeCambiarContrasena());
    }
}
