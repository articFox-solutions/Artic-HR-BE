package com.articfox.hr.auditoria;

/** Motivo interno de cada intento. Solo se guarda en auditoría: al cliente siempre se le da el mensaje genérico. */
public enum MotivoLogin {
    EXITOSO,
    USUARIO_NO_EXISTE,
    CONTRASENA_INCORRECTA,
    CUENTA_BLOQUEADA,
    USUARIO_INACTIVO,
    COLABORADOR_INACTIVO,
    LIMITE_IP
}
