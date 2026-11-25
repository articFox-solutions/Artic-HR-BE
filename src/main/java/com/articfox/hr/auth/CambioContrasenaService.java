package com.articfox.hr.auth;

import com.articfox.hr.common.ErrorApi;
import com.articfox.hr.usuario.Usuario;
import com.articfox.hr.usuario.UsuarioRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cambio de contraseña, obligatorio cuando IT asignó una temporal (RF-08 y DERCAS 9.7). */
@Service
public class CambioContrasenaService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public CambioContrasenaService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void cambiar(UUID usuarioId, String actual, String nueva) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ErrorApi(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                        "Inicia sesión para continuar."));
        if (!passwordEncoder.matches(actual, usuario.getPasswordHash())) {
            throw new ErrorApi(HttpStatus.BAD_REQUEST, "CONTRASENA_ACTUAL_INCORRECTA",
                    "La contraseña actual no es correcta.");
        }
        if (actual.equals(nueva)) {
            throw new ErrorApi(HttpStatus.BAD_REQUEST, "CONTRASENA_REPETIDA",
                    "La nueva contraseña debe ser distinta de la actual.");
        }
        usuario.cambiarContrasena(passwordEncoder.encode(nueva));
    }
}
