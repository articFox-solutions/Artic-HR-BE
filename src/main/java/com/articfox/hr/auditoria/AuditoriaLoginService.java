package com.articfox.hr.auditoria;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registra cada intento de login con fecha e IP (DERCAS RNF-06). */
@Service
public class AuditoriaLoginService {

    private static final int MAX_CORREO = 254;
    private static final int MAX_IP = 45;
    private static final int MAX_USER_AGENT = 512;

    private final AuditoriaLoginRepository repositorio;
    private final Clock clock;

    public AuditoriaLoginService(AuditoriaLoginRepository repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    @Transactional
    public void registrar(UUID usuarioId, String correo, MotivoLogin motivo, String ip, String userAgent) {
        repositorio.save(new AuditoriaLogin(usuarioId, recortar(correo, MAX_CORREO), motivo, recortar(ip, MAX_IP),
                recortar(userAgent, MAX_USER_AGENT), clock.instant()));
    }

    private static String recortar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
