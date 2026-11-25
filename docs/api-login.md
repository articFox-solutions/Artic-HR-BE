# Contrato de API: Login y Página de Inicio (Incremento 1)

Contrato entre el backend y el frontend Angular, según el DERCAS 1.05. Con el backend corriendo, la misma información se puede explorar y probar en **http://localhost:8080/swagger-ui.html**. Todas las rutas empiezan con `/api` y se sirven desde el **mismo origen** que el frontend: en local con el proxy de Angular y en producción con Caddy. Por eso no hay CORS.

## Reglas generales

- **Sesión.** Vive en una cookie `SESSION` con `HttpOnly`, `Secure` y `SameSite=Lax`. Angular no la lee ni la guarda: el navegador la envía solo. Nunca guardes token, rol ni datos sensibles en `localStorage` o `sessionStorage` (DERCAS 9.6).
- **CSRF.** El backend entrega la cookie `XSRF-TOKEN`. El `HttpClient` de Angular la copia al encabezado `X-XSRF-TOKEN` en cada `POST`, `PUT`, `PATCH` y `DELETE` con URL relativa. Para que funcione:
  1. Llama `GET /api/auth/csrf` al iniciar la app, antes del login.
  2. Usa URLs relativas (`/api/...`), nunca `http://localhost:8080/api/...`.
  3. Después del login y del logout el token cambia. Angular toma el nuevo de la cookie sin hacer nada.
- **Expiración.** La sesión cierra tras 30 minutos sin peticiones o 8 horas después del login, aunque haya actividad.
- **Caché.** Las respuestas incluyen `Cache-Control: no-store`, para que el botón atrás no muestre páginas internas (RF-04).

## Formato de errores

Todos los errores usan Problem Details (RFC 9457) con `Content-Type: application/problem+json` y un campo `codigo` estable. **Decide en Angular por `codigo` y `status`, nunca por el texto de `detail`.**

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Correo o contraseña incorrectos. Si continúa, contacta a IT.",
  "codigo": "CREDENCIALES_INVALIDAS"
}
```

| Status | `codigo` | Cuándo | Qué hace Angular |
| --- | --- | --- | --- |
| 400 | `VALIDACION` | Formato inválido. Trae `errores` por campo | Muestra cada error junto a su campo |
| 400 | `CONTRASENA_ACTUAL_INCORRECTA` | Cambio de contraseña con la actual mal | Muestra `detail` |
| 400 | `CONTRASENA_REPETIDA` | La nueva es igual a la actual | Muestra `detail` |
| 401 | `CREDENCIALES_INVALIDAS` | Cualquier fallo de login, incluido el bloqueo | Muestra `detail` tal cual (RF-03) |
| 401 | `NO_AUTENTICADO` | Petición sin sesión | Redirige a `/login` (RF-06) |
| 401 | `SESION_EXPIRADA` | Pasaron 8 horas desde el login | Redirige a `/login` |
| 401 | `SESION_INVALIDA` | El usuario fue desactivado | Redirige a `/login` |
| 403 | `CAMBIO_CONTRASENA_REQUERIDO` | Tiene contraseña temporal | Redirige a `/cambiar-contrasena` |
| 403 | `CSRF_INVALIDO` | Falta `X-XSRF-TOKEN` o no coincide | Llama `GET /api/auth/csrf` y reintenta una vez |
| 429 | `DEMASIADOS_INTENTOS` | Demasiados fallos desde la misma IP | Muestra `detail` |

Un interceptor de `HttpClient` puede resolver casi toda la tabla: cualquier 401 redirige a `/login` y `CAMBIO_CONTRASENA_REQUERIDO` redirige al cambio de contraseña.

## Endpoints

### `GET /api/auth/csrf`

Público. Entrega la cookie `XSRF-TOKEN`. Responde `204` sin cuerpo.

### `POST /api/auth/login`

Público, pero requiere CSRF. RF-01, RF-02 y RF-03.

```json
{ "correo": "ana.morales@artichr.test", "contrasena": "ArticHR-Demo-2026!" }
```

`200`: crea la sesión y devuelve el perfil, con el mismo formato que `GET /api/me`. Si `debeCambiarContrasena` es `true`, lleva al usuario a cambiar su contraseña antes de entrar al portal (RF-08).

Errores: `400 VALIDACION`, `401 CREDENCIALES_INVALIDAS` y `429 DEMASIADOS_INTENTOS`. El mensaje es el mismo si el correo no existe, la contraseña es incorrecta, la cuenta está bloqueada o el usuario está desactivado. Esto es intencional: así no se revela qué cuentas existen.

### `GET /api/me`

Requiere sesión. RF-05. Angular lo llama al cargar: `200` significa sesión válida y `401` significa ir al login.

```json
{
  "nombre": "Ana Lucía Morales",
  "correo": "ana.morales@artichr.test",
  "puesto": "Gerente de Recursos Humanos",
  "departamento": "Recursos Humanos",
  "rol": { "codigo": "ADM", "nombre": "Administrador de HR" },
  "debeCambiarContrasena": false
}
```

### `GET /api/menu`

Requiere sesión. RF-07. En el Incremento 1 todos los roles reciben solo Inicio. `clave` es la llave de Transloco y `etiqueta` el texto en español por defecto.

```json
{
  "opciones": [
    { "id": "inicio", "clave": "menu.inicio", "etiqueta": "Inicio", "ruta": "/inicio", "icono": "home" }
  ]
}
```

### `POST /api/auth/cambiar-contrasena`

Requiere sesión y CSRF. RF-08. Es lo único, junto con `GET /api/me` y el logout, que puede usar alguien con contraseña temporal.

```json
{ "contrasenaActual": "ArticHR-Demo-2026!", "contrasenaNueva": "Mi-Nueva-Clave-2026" }
```

La nueva debe tener entre 12 y 128 caracteres. Responde `204` y cambia el identificador de sesión.

Errores: `400 VALIDACION` (en `errores.contrasenaNueva`), `400 CONTRASENA_ACTUAL_INCORRECTA` y `400 CONTRASENA_REPETIDA`.

### `POST /api/auth/logout`

Requiere CSRF. RF-04. Invalida la sesión y borra la cookie. Responde `204` aunque no hubiera sesión, así que siempre se puede llamar.

### `GET /api/health`

Público. Responde `{"status":"UP"}`.

## Desarrollo local del frontend

Sigue el **Inicio rápido** del [README](../README.md): `scripts/instalar` una vez y `scripts/iniciar` cada vez. Con `-Temporal` (`--temporal` en macOS o Linux) la base es desechable y no hace falta `.env`.

Ambas opciones dejan el backend en `http://localhost:8080` con 25 usuarios demo. En Angular, configura el proxy para que `/api` vaya al backend y la app use el mismo origen. Por ejemplo, en `proxy.conf.json`:

```json
{
  "/api": { "target": "http://localhost:8080", "secure": false }
}
```

Y en `angular.json`, en las opciones de `serve`: `"proxyConfig": "proxy.conf.json"`.

### Cuentas demo

La contraseña de todas es `ArticHR-Demo-2026!`. Solo existen con el perfil `local` o con `test-run`.

| Correo | Rol | Caso |
| --- | --- | --- |
| `ana.morales@artichr.test` | ADM | Acceso normal |
| `luis.herrera@artichr.test` | AHR | Acceso normal |
| `daniela.fuentes@artichr.test` | CA | Acceso normal |
| `sofia.ramirez@artichr.test` | CA | Contraseña temporal: debe cambiarla |
| `pedro.castillo@artichr.test` | CA | Usuario desactivado: el login siempre falla |

Para probar el bloqueo, falla 5 veces con cualquier cuenta: queda bloqueada 15 minutos. Reiniciar el backend no la desbloquea, porque el bloqueo se guarda en la base.
