package com.articfox.hr.auth;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cliente de pruebas que se comporta como el navegador con Angular: guarda las cookies que
 * manda el servidor y copia XSRF-TOKEN al encabezado X-XSRF-TOKEN en cada petición.
 * Se manejan a mano porque el CookieManager de Java no envía cookies Secure por http.
 */
final class ClienteHttp {

    private final HttpClient http = HttpClient.newHttpClient();
    private final String base;
    private final String ip;
    private final Map<String, String> cookies = new LinkedHashMap<>();
    private boolean enviarCsrf = true;

    /** La IP se simula con X-Forwarded-For, como lo haría Caddy. */
    ClienteHttp(int puerto, String ip) {
        this.base = "http://localhost:" + puerto;
        this.ip = ip;
    }

    ClienteHttp sinEncabezadoCsrf() {
        enviarCsrf = false;
        return this;
    }

    HttpResponse<String> get(String ruta) {
        return enviar(HttpRequest.newBuilder(URI.create(base + ruta)).GET());
    }

    HttpResponse<String> post(String ruta, String json) {
        return enviar(HttpRequest.newBuilder(URI.create(base + ruta))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)));
    }

    HttpResponse<String> obtenerCsrf() {
        return get("/api/auth/csrf");
    }

    HttpResponse<String> login(String correo, String contrasena) {
        if (!cookies.containsKey("XSRF-TOKEN")) {
            obtenerCsrf();
        }
        return post("/api/auth/login", "{\"correo\":\"" + correo + "\",\"contrasena\":\"" + contrasena + "\"}");
    }

    String cookie(String nombre) {
        return cookies.get(nombre);
    }

    void ponerCookie(String nombre, String valor) {
        cookies.put(nombre, valor);
    }

    private HttpResponse<String> enviar(HttpRequest.Builder peticion) {
        peticion.header("X-Forwarded-For", ip);
        if (!cookies.isEmpty()) {
            peticion.header("Cookie", cookies.entrySet().stream()
                    .map(c -> c.getKey() + "=" + c.getValue())
                    .collect(Collectors.joining("; ")));
        }
        if (enviarCsrf && cookies.containsKey("XSRF-TOKEN")) {
            peticion.header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"));
        }
        try {
            HttpResponse<String> respuesta = http.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
            respuesta.headers().allValues("Set-Cookie").forEach(this::guardar);
            return respuesta;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private void guardar(String setCookie) {
        String par = setCookie.split(";", 2)[0];
        int igual = par.indexOf('=');
        String nombre = par.substring(0, igual).trim();
        String valor = par.substring(igual + 1).trim();
        if (valor.isEmpty() || setCookie.toLowerCase(Locale.ROOT).contains("max-age=0")) {
            cookies.remove(nombre);
        } else {
            cookies.put(nombre, valor);
        }
    }
}
