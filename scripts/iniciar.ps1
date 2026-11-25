<#
.SYNOPSIS
    Arranca el backend de Artic-HR en Windows con PostgreSQL en Docker y los usuarios demo.

.DESCRIPTION
    Por defecto usa compose.yaml y .env: los datos se conservan entre reinicios.
    Con -Temporal usa un PostgreSQL desechable y no necesita .env.

    Uso, desde la raíz del repositorio:
        powershell -ExecutionPolicy Bypass -File scripts\iniciar.ps1
        powershell -ExecutionPolicy Bypass -File scripts\iniciar.ps1 -Temporal
        powershell -ExecutionPolicy Bypass -File scripts\iniciar.ps1 -Puerto 8081

.PARAMETER Puerto
    Puerto HTTP del backend. Por defecto 8080, el que espera el proxy de Angular.

.PARAMETER Temporal
    Base de datos desechable: se borra al detener el backend.
#>
param(
    [int]$Puerto = 8080,
    [switch]$Temporal
)
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz

function Stop-ConError($texto) {
    Write-Host "X  $texto" -ForegroundColor Red
    exit 1
}

if (-not $Temporal -and -not (Test-Path '.env')) {
    Stop-ConError 'Falta .env. Corre primero: powershell -ExecutionPolicy Bypass -File scripts\instalar.ps1'
}

cmd /c 'docker info >nul 2>&1'
if ($LASTEXITCODE -ne 0) {
    Stop-ConError 'Docker no responde. Abre Docker Desktop y espera a que diga "running".'
}

if (Get-NetTCPConnection -LocalPort $Puerto -State Listen -ErrorAction SilentlyContinue) {
    Stop-ConError "El puerto $Puerto ya está en uso. Detén lo que lo ocupa o usa otro: -Puerto 8081"
}

$base = "http://localhost:$Puerto"
Write-Host ''
Write-Host 'Artic-HR backend' -ForegroundColor Cyan
Write-Host "  API         $base/api/health"
Write-Host "  Swagger UI  $base/swagger-ui.html"
Write-Host '  Cuentas     ana.morales@artichr.test (ADM), luis.herrera@artichr.test (AHR),'
Write-Host '              daniela.fuentes@artichr.test (CA)'
Write-Host '  Contraseña  ArticHR-Demo-2026!'
if ($Temporal) { Write-Host '  Base        temporal: los datos se borran al detenerlo' -ForegroundColor Yellow }
Write-Host '  Detener     Ctrl+C'
Write-Host ''

$argumentos = "-Dspring-boot.run.arguments=--server.port=$Puerto"
if ($Temporal) {
    & .\mvnw.cmd -B spring-boot:test-run $argumentos
} else {
    & .\mvnw.cmd -B spring-boot:run '-Dspring-boot.run.profiles=local' $argumentos
}
exit $LASTEXITCODE
