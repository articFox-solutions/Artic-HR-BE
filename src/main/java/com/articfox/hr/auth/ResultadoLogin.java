package com.articfox.hr.auth;

public record ResultadoLogin(Tipo tipo, SesionUsuario sesion) {

    public enum Tipo {
        EXITOSO,
        FALLIDO,
        LIMITE_IP
    }

    static ResultadoLogin exitoso(SesionUsuario sesion) {
        return new ResultadoLogin(Tipo.EXITOSO, sesion);
    }

    static ResultadoLogin fallido() {
        return new ResultadoLogin(Tipo.FALLIDO, null);
    }

    static ResultadoLogin limiteIp() {
        return new ResultadoLogin(Tipo.LIMITE_IP, null);
    }
}
