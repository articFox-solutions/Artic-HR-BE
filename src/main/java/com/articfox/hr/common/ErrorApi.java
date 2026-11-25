package com.articfox.hr.common;

import org.springframework.http.HttpStatus;

/** Error esperado del negocio, con el estado HTTP y el código que verá el cliente. */
public class ErrorApi extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ErrorApi(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
