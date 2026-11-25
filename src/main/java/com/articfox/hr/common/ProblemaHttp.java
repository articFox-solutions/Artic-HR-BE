package com.articfox.hr.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/**
 * Formato único de error de la API: Problem Details (RFC 9457) con una propiedad extra
 * {@code codigo}, estable para que Angular decida qué hacer sin leer el mensaje.
 */
public final class ProblemaHttp {

    public static final String TIPO_CONTENIDO = "application/problem+json";

    private ProblemaHttp() {
    }

    public static ProblemDetail crear(HttpStatus status, String codigo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(status.getReasonPhrase());
        problema.setProperty("codigo", codigo);
        return problema;
    }

    public static ResponseEntity<ProblemDetail> respuesta(HttpStatus status, String codigo, String detalle) {
        return ResponseEntity.status(status).body(crear(status, codigo, detalle));
    }

    /** Para filtros y manejadores de seguridad, que corren antes de Spring MVC. */
    public static void escribir(jakarta.servlet.http.HttpServletResponse response, HttpStatus status, String codigo,
            String detalle) throws IOException {
        response.setStatus(status.value());
        response.setContentType(TIPO_CONTENIDO);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + json(status.getReasonPhrase())
                + "\",\"status\":" + status.value() + ",\"detail\":\"" + json(detalle) + "\",\"codigo\":\""
                + json(codigo) + "\"}");
    }

    private static String json(String valor) {
        StringBuilder salida = new StringBuilder(valor.length());
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '"' -> salida.append("\\\"");
                case '\\' -> salida.append("\\\\");
                case '\n' -> salida.append("\\n");
                case '\r' -> salida.append("\\r");
                case '\t' -> salida.append("\\t");
                default -> {
                    if (c < 0x20) {
                        salida.append(String.format("\\u%04x", (int) c));
                    } else {
                        salida.append(c);
                    }
                }
            }
        }
        return salida.toString();
    }
}
