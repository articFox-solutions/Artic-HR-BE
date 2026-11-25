# Artic-HR-BE

Backend del Portal Dinámico de HR. El Incremento 1 cubre el login y la página de inicio según el DERCAS 1.05.

**Stack:** Spring Boot 4.2, Java 21, Spring Security 7, Spring Session JDBC, Spring Data JPA, Flyway y PostgreSQL 18.

## Requisitos

Instala esto antes de empezar:

| Herramienta | Para qué | Windows | macOS |
| --- | --- | --- | --- |
| JDK 21 | Compilar y correr el backend | `winget install EclipseAdoptium.Temurin.21.JDK` | `brew install --cask temurin@21` |
| Docker Desktop | PostgreSQL y las pruebas | `winget install Docker.DockerDesktop` | `brew install --cask docker` |
| Git | Clonar el repositorio | `winget install Git.Git` | `xcode-select --install` |

Maven no hace falta instalarlo: el proyecto trae su Wrapper (`mvnw`). La carpeta `.mvn/` incluye un `settings.xml` propio para que este proyecto descargue solo de Maven Central, sin importar la configuración global de cada máquina.

## Inicio rápido

Abre Docker Desktop y, desde la raíz del repositorio, corre dos scripts. El primero se corre una sola vez: revisa los requisitos, crea `.env` con una contraseña aleatoria y compila. El segundo arranca el backend cada vez que lo necesites.

**Windows (PowerShell):**

```powershell
powershell -ExecutionPolicy Bypass -File scripts\instalar.ps1
powershell -ExecutionPolicy Bypass -File scripts\iniciar.ps1
```

**macOS o Linux:**

```bash
./scripts/instalar.sh
./scripts/iniciar.sh
```

El backend queda en `http://localhost:8080`, con PostgreSQL en Docker y 25 usuarios demo. Se detiene con `Ctrl+C`. Los datos se conservan entre reinicios.

Opciones de `iniciar`:

| Windows | macOS o Linux | Efecto |
| --- | --- | --- |
| `-Puerto 8081` | `--puerto 8081` | Usa otro puerto si el 8080 está ocupado |
| `-Temporal` | `--temporal` | Base desechable que se borra al detener. No necesita `.env` |

Sin scripts, lo mismo es `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"`.

## Swagger UI

Con el backend corriendo, abre **http://localhost:8080/swagger-ui.html**. Ahí están todos los endpoints, sus ejemplos y sus errores, y se pueden probar desde el navegador:

1. Ejecuta `GET /api/auth/csrf`.
2. Ejecuta `POST /api/auth/login`. El ejemplo ya trae la cuenta `ana.morales@artichr.test`.
3. Prueba `GET /api/me` o `GET /api/menu`. La cookie de sesión viaja sola.

El documento OpenAPI está en `/v3/api-docs`, por ejemplo para generar clientes. Swagger solo se enciende con el perfil `local` y con `test-run`. En producción está apagado, para no publicar el mapa de la API.

## Problemas comunes

- **"Docker no responde".** Abre Docker Desktop y espera a que diga que está corriendo.
- **"El puerto 8080 ya está en uso".** Detén la otra instancia, por ejemplo la de IntelliJ, o usa `-Puerto 8081`. Si cambias el puerto, ajusta también el `target` del proxy de Angular.
- **El puerto 5432 está ocupado por otro PostgreSQL.** Cambia `POSTGRES_PORT` en `.env`, por ejemplo a 5433.
- **PowerShell dice que la ejecución de scripts está deshabilitada.** Usa el comando completo con `-ExecutionPolicy Bypass`, como aparece arriba.
- **Empezar con la base limpia.** `docker compose down -v` borra el contenedor y sus datos. El siguiente arranque vuelve a cargar los usuarios demo.

## Pruebas

```powershell
.\mvnw.cmd test
```

Las pruebas de integración usan Testcontainers con PostgreSQL 18 real y cubren cada criterio de aceptación del DERCAS.

## Documentación

- [Contrato de API para el frontend](docs/api-login.md)

## Decisiones principales

- **Sesión en servidor con Spring Session JDBC, no JWT.** Permite invalidar sesiones al instante, en el logout o al dar de baja a un usuario, y el navegador nunca maneja tokens.
- **ID de sesión de 256 bits.** El generador por defecto usa UUID, con 122 bits aleatorios. Por eso la columna `SESSION_ID` se amplió a `VARCHAR(64)` en `V2__spring_session.sql`.
- **Tope de 8 horas en un filtro propio.** Spring Session solo maneja la inactividad. `SesionValidaFilter` revisa además el tope absoluto, el estado del usuario y la contraseña temporal.
- **Mensaje de login único.** Correo inexistente, contraseña incorrecta, bloqueo y usuario desactivado responden igual y tardan lo mismo. El motivo real queda en `auditoria_login`.
- **Límite de fallos por IP en memoria.** Sirve mientras haya una sola instancia del backend. Con varias, habría que moverlo a almacenamiento compartido.
