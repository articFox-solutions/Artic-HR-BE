package com.articfox.hr.auth;

import com.articfox.hr.config.LoginProperties;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Limita los logins fallidos por IP en una ventana deslizante (DERCAS RNF-01). Rechaza a la IP
 * sin tocar el contador de la cuenta, así un atacante no puede bloquear cuentas ajenas.
 *
 * <p>Vive en memoria: sirve para una sola instancia del backend. Con varias instancias habría que
 * moverlo a una base compartida, como Redis o PostgreSQL.
 */
@Component
public class LimiteFallosPorIp {

    private final int maxFallos;
    private final java.time.Duration ventana;
    private final Map<String, Deque<Instant>> fallos = new HashMap<>();

    public LimiteFallosPorIp(LoginProperties properties) {
        this.maxFallos = properties.limiteIp().maxFallos();
        this.ventana = properties.limiteIp().ventana();
    }

    public synchronized boolean estaLimitada(String ip, Instant ahora) {
        Deque<Instant> intentos = fallos.get(ip);
        if (intentos == null) {
            return false;
        }
        descartarVencidos(intentos, ahora);
        if (intentos.isEmpty()) {
            fallos.remove(ip);
            return false;
        }
        return intentos.size() >= maxFallos;
    }

    public synchronized void registrarFallo(String ip, Instant ahora) {
        Deque<Instant> intentos = fallos.computeIfAbsent(ip, clave -> new ArrayDeque<>());
        descartarVencidos(intentos, ahora);
        intentos.addLast(ahora);
    }

    /** Solo para pruebas. */
    public synchronized void reiniciar() {
        fallos.clear();
    }

    private void descartarVencidos(Deque<Instant> intentos, Instant ahora) {
        Instant limite = ahora.minus(ventana);
        while (!intentos.isEmpty() && !intentos.peekFirst().isAfter(limite)) {
            intentos.removeFirst();
        }
    }
}
