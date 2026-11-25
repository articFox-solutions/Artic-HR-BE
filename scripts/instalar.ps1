<#
.SYNOPSIS
    Prepara el backend de Artic-HR en Windows. Se corre una sola vez.

.DESCRIPTION
    1. Revisa que estén JDK 21 y Docker.
    2. Crea .env con una contraseña aleatoria para PostgreSQL, si no existe.
    3. Descarga las dependencias y compila el proyecto.

    Uso, desde la raíz del repositorio:
        powershell -ExecutionPolicy Bypass -File scripts\instalar.ps1
#>
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz

function Write-Paso($texto) { Write-Host "`n==> $texto" -ForegroundColor Cyan }
function Write-Ok($texto) { Write-Host "    OK  $texto" -ForegroundColor Green }
function Write-Falla($texto) { Write-Host "    X   $texto" -ForegroundColor Red }

$faltantes = 0

# --- 1. Java 21 -------------------------------------------------------------
Write-Paso 'Revisando JDK 21'
# mvnw usa JAVA_HOME si existe; si no, el java del PATH
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java' }
$versionJava = $null
try {
    $salida = cmd /c "`"$java`" -version 2>&1"
    if ("$salida" -match 'version "(\d+)') { $versionJava = [int]$Matches[1] }
} catch { }

if ($versionJava -ge 21) {
    Write-Ok "Java $versionJava ($java)"
} else {
    $faltantes++
    if ($versionJava) { Write-Falla "Se encontró Java $versionJava en $java y se necesita 21 o mayor." }
    else { Write-Falla 'No se encontró Java.' }
    Write-Host '        Instálalo con:  winget install EclipseAdoptium.Temurin.21.JDK'
    Write-Host '        Luego abre una terminal nueva. Si tienes JAVA_HOME, haz que apunte al JDK 21.'
}

# --- 2. Docker --------------------------------------------------------------
Write-Paso 'Revisando Docker'
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    $faltantes++
    Write-Falla 'No se encontró Docker.'
    Write-Host '        Instala Docker Desktop:  winget install Docker.DockerDesktop'
} else {
    cmd /c 'docker info >nul 2>&1'
    if ($LASTEXITCODE -eq 0) {
        Write-Ok 'Docker está corriendo'
    } else {
        $faltantes++
        Write-Falla 'Docker está instalado pero no responde. Abre Docker Desktop y espera a que diga "running".'
    }
}

if ($faltantes -gt 0) {
    Write-Host "`nFaltan $faltantes requisito(s). Resuélvelos y vuelve a correr este script." -ForegroundColor Yellow
    exit 1
}

# --- 3. Archivo .env --------------------------------------------------------
Write-Paso 'Preparando .env'
if (Test-Path '.env') {
    Write-Ok '.env ya existe; no se modifica'
} else {
    $caracteres = [char[]]'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789'
    $bytes = New-Object byte[] 24
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $clave = -join ($bytes | ForEach-Object { $caracteres[$_ % $caracteres.Length] })
    $contenido = (Get-Content '.env.example' -Encoding UTF8) -replace '^POSTGRES_PASSWORD=.*$', "POSTGRES_PASSWORD=$clave"
    # UTF-8 sin BOM, para que Docker Compose lo lea bien
    [System.IO.File]::WriteAllLines((Join-Path $raiz '.env'), $contenido, (New-Object System.Text.UTF8Encoding $false))
    Write-Ok '.env creado con una contraseña aleatoria para PostgreSQL'
}

$puertoDb = ((Get-Content '.env') | Where-Object { $_ -match '^POSTGRES_PORT=(\d+)' } | ForEach-Object { $Matches[1] })
if (-not $puertoDb) { $puertoDb = 5432 }
$ocupado = Get-NetTCPConnection -LocalPort $puertoDb -State Listen -ErrorAction SilentlyContinue
$contenedor = cmd /c 'docker compose ps -q postgres 2>nul'
if ($ocupado -and -not $contenedor) {
    Write-Host "    !   El puerto $puertoDb ya está en uso, quizá por otro PostgreSQL." -ForegroundColor Yellow
    Write-Host '        Cambia POSTGRES_PORT en .env, por ejemplo a 5433.'
}

# --- 4. Dependencias y compilación -----------------------------------------
Write-Paso 'Descargando dependencias y compilando (la primera vez tarda unos minutos)'
& .\mvnw.cmd -B -q -DskipTests test-compile
if ($LASTEXITCODE -ne 0) {
    Write-Falla 'La compilación falló. Revisa el error de arriba.'
    exit 1
}
Write-Ok 'Proyecto compilado'

Write-Host "`nListo. Para arrancar el backend:" -ForegroundColor Green
Write-Host '    powershell -ExecutionPolicy Bypass -File scripts\iniciar.ps1'
