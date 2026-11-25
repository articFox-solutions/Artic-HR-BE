#!/usr/bin/env bash
# Prepara el backend de Artic-HR en macOS o Linux. Se corre una sola vez.
#   1. Revisa que estén JDK 21 y Docker.
#   2. Crea .env con una contraseña aleatoria para PostgreSQL, si no existe.
#   3. Descarga las dependencias y compila el proyecto.
#
# Uso, desde la raíz del repositorio:  ./scripts/instalar.sh
set -euo pipefail
cd "$(dirname "$0")/.."

paso()  { printf '\n\033[36m==> %s\033[0m\n' "$1"; }
ok()    { printf '    \033[32mOK\033[0m  %s\n' "$1"; }
falla() { printf '    \033[31mX\033[0m   %s\n' "$1"; }

faltantes=0

# --- 1. Java 21 -------------------------------------------------------------
paso 'Revisando JDK 21'
# mvnw usa JAVA_HOME si existe; si no, el java del PATH
java_cmd="${JAVA_HOME:+$JAVA_HOME/bin/}java"
version_java=$("$java_cmd" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -n 1 || true)
if [ -n "$version_java" ] && [ "$version_java" -ge 21 ]; then
  ok "Java $version_java ($java_cmd)"
else
  faltantes=$((faltantes + 1))
  if [ -n "$version_java" ]; then falla "Se encontró Java $version_java y se necesita 21 o mayor."
  else falla 'No se encontró Java.'; fi
  echo '        macOS:  brew install --cask temurin@21'
  echo '        Linux:  https://adoptium.net/installation/'
fi

# --- 2. Docker --------------------------------------------------------------
paso 'Revisando Docker'
if ! command -v docker >/dev/null 2>&1; then
  faltantes=$((faltantes + 1))
  falla 'No se encontró Docker. Instala Docker Desktop: https://docs.docker.com/get-docker/'
elif docker info >/dev/null 2>&1; then
  ok 'Docker está corriendo'
else
  faltantes=$((faltantes + 1))
  falla 'Docker está instalado pero no responde. Abre Docker Desktop y espera a que diga "running".'
fi

if [ "$faltantes" -gt 0 ]; then
  printf '\n\033[33mFaltan %s requisito(s). Resuélvelos y vuelve a correr este script.\033[0m\n' "$faltantes"
  exit 1
fi

# --- 3. Archivo .env --------------------------------------------------------
paso 'Preparando .env'
if [ -f .env ]; then
  ok '.env ya existe; no se modifica'
else
  clave=$(LC_ALL=C tr -dc 'A-HJ-NP-Za-km-z2-9' </dev/urandom | head -c 24 || true)
  sed "s/^POSTGRES_PASSWORD=.*/POSTGRES_PASSWORD=$clave/" .env.example > .env
  chmod 600 .env
  ok '.env creado con una contraseña aleatoria para PostgreSQL'
fi

puerto_db=$(sed -n 's/^POSTGRES_PORT=\([0-9]*\).*/\1/p' .env)
puerto_db=${puerto_db:-5432}
if (echo >"/dev/tcp/127.0.0.1/$puerto_db") 2>/dev/null && [ -z "$(docker compose ps -q postgres 2>/dev/null)" ]; then
  printf '    \033[33m!\033[0m   El puerto %s ya está en uso, quizá por otro PostgreSQL.\n' "$puerto_db"
  echo '        Cambia POSTGRES_PORT en .env, por ejemplo a 5433.'
fi

# --- 4. Dependencias y compilación -----------------------------------------
paso 'Descargando dependencias y compilando (la primera vez tarda unos minutos)'
./mvnw -B -q -DskipTests test-compile
ok 'Proyecto compilado'

printf '\n\033[32mListo. Para arrancar el backend:\033[0m\n'
echo '    ./scripts/iniciar.sh'
