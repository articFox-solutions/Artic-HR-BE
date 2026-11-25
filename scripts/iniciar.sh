#!/usr/bin/env bash
# Arranca el backend de Artic-HR en macOS o Linux con PostgreSQL en Docker y los usuarios demo.
# Por defecto usa compose.yaml y .env: los datos se conservan entre reinicios.
#
# Uso, desde la raíz del repositorio:
#   ./scripts/iniciar.sh                 puerto 8080, base persistente
#   ./scripts/iniciar.sh --temporal      base desechable, no necesita .env
#   ./scripts/iniciar.sh --puerto 8081   otro puerto
set -euo pipefail
cd "$(dirname "$0")/.."

puerto=8080
temporal=false
while [ $# -gt 0 ]; do
  case "$1" in
    --puerto) puerto="$2"; shift 2 ;;
    --temporal) temporal=true; shift ;;
    *) echo "Opción desconocida: $1"; exit 1 ;;
  esac
done

error() { printf '\033[31mX  %s\033[0m\n' "$1"; exit 1; }

if [ "$temporal" = false ] && [ ! -f .env ]; then
  error 'Falta .env. Corre primero: ./scripts/instalar.sh'
fi
docker info >/dev/null 2>&1 || error 'Docker no responde. Abre Docker Desktop y espera a que diga "running".'
if (echo >"/dev/tcp/127.0.0.1/$puerto") 2>/dev/null; then
  error "El puerto $puerto ya está en uso. Detén lo que lo ocupa o usa otro: --puerto 8081"
fi

base="http://localhost:$puerto"
printf '\n\033[36mArtic-HR backend\033[0m\n'
echo "  API         $base/api/health"
echo "  Swagger UI  $base/swagger-ui.html"
echo '  Cuentas     ana.morales@artichr.test (ADM), luis.herrera@artichr.test (AHR),'
echo '              daniela.fuentes@artichr.test (CA)'
echo '  Contraseña  ArticHR-Demo-2026!'
[ "$temporal" = true ] && printf '  \033[33mBase        temporal: los datos se borran al detenerlo\033[0m\n'
echo '  Detener     Ctrl+C'
echo

argumentos="-Dspring-boot.run.arguments=--server.port=$puerto"
if [ "$temporal" = true ]; then
  exec ./mvnw -B spring-boot:test-run "$argumentos"
else
  exec ./mvnw -B spring-boot:run -Dspring-boot.run.profiles=local "$argumentos"
fi
