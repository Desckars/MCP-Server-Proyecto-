#!/usr/bin/env powershell
# ========================================
#   CHATBOT IA - Claude + MCP O3
#   Quick Start Script
# ========================================

Write-Host "`n" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CHATBOT IA - Claude + MCP O3" -ForegroundColor Cyan
Write-Host "  Inicio Rapido" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Check Java
Write-Host "`n[1/5] Verificando Java..." -ForegroundColor Yellow
$javaCheck = java -version 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "[1/5] Java detectado correctamente" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Java no encontrado. Instala Java 21+." -ForegroundColor Red
    exit 1
}

# Check Config File
Write-Host "[2/5] Verificando configuracion..." -ForegroundColor Yellow
$configFile = "src/main/resources/config.properties"
if (Test-Path $configFile) {
    Write-Host "[2/5] Configuracion encontrada" -ForegroundColor Green
} else {
    Write-Host "[ERROR] config.properties no encontrado" -ForegroundColor Red
    exit 1
}

# Check API Key
Write-Host "[3/5] Verificando API Key..." -ForegroundColor Yellow
$apiKeySet = $false
if ($env:CLAUDE_API_KEY) {
    Write-Host "[3/5] API Key cargado desde variable de entorno" -ForegroundColor Green
    $apiKeySet = $true
} else {
    $configContent = Get-Content $configFile
    if ($configContent -match "anthropic\.api-key\.encrypted") {
        Write-Host "[3/5] API Key encriptado encontrado" -ForegroundColor Green
        $apiKeySet = $true
    } elseif ($configContent -match "anthropic\.api-key\s*=\s*sk-") {
        Write-Host "[3/5] API Key en texto plano detectado (será encriptado)" -ForegroundColor Yellow
        $apiKeySet = $true
    }
}

if (-not $apiKeySet) {
    Write-Host "`n⚠️  API Key NO configurada" -ForegroundColor Yellow
    Write-Host "    Se abrira la ventana de configuracion al iniciar" -ForegroundColor Yellow
    Write-Host "    para que ingreses tu API Key de forma segura.`n" -ForegroundColor Yellow
}

# Compile
Write-Host "[4/5] Compilando proyecto..." -ForegroundColor Yellow
mvn clean compile package -q -DskipTests
if ($LASTEXITCODE -eq 0) {
    Write-Host "[4/5] Compilacion exitosa" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Compilacion fallida" -ForegroundColor Red
    exit 1
}

# Check MCP
Write-Host "[5/5] Verificando MCP O3..." -ForegroundColor Yellow
$mcpPath = "../../mcp_o3"
if (Test-Path $mcpPath) {
    Write-Host "[5/5] MCP O3 encontrado y listo" -ForegroundColor Green
} else {
    Write-Host "[WARNING] MCP O3 no encontrado (seguir de todos modos)" -ForegroundColor Yellow
}

Write-Host "`n[INFO] Usando JAR con dependencias: chatbot-ia-executable.jar" -ForegroundColor Cyan

# Menu
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  SELECCIONA MODO DE EJECUCION:" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "`n  [1] Interfaz Grafica (Recomendado)" -ForegroundColor Green
Write-Host "  [2] Modo Consola" -ForegroundColor Yellow
Write-Host "  [3] Configurar API Key (Manual)" -ForegroundColor Yellow
Write-Host "  [4] Cancelar" -ForegroundColor Red
Write-Host "`n========================================" -ForegroundColor Cyan

$choice = Read-Host "Opcion (1-4)"

switch ($choice) {
    "1" {
        Write-Host "`n========================================" -ForegroundColor Cyan
        Write-Host "  INICIANDO INTERFAZ GRAFICA" -ForegroundColor Cyan
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "`n[NOTA] Se abrira la ventana de configuracion" -ForegroundColor Yellow
        Write-Host "       para que ingreses tu API Key.`n" -ForegroundColor Yellow
        
        Write-Host "Ejecutando: java -jar target\chatbot-ia-executable.jar`n" -ForegroundColor Cyan
        java -jar target/chatbot-ia-executable.jar
    }
    "2" {
        Write-Host "`n========================================" -ForegroundColor Cyan
        Write-Host "  INICIANDO MODO CONSOLA" -ForegroundColor Cyan
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "[EN DESARROLLO] Próximamente..." -ForegroundColor Yellow
    }
    "3" {
        Write-Host "`n========================================" -ForegroundColor Cyan
        Write-Host "  CONFIGURAR API KEY" -ForegroundColor Cyan
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "`n[NOTA] Edita el archivo config.properties manualmente:" -ForegroundColor Yellow
        Write-Host "       $configFile" -ForegroundColor Cyan
        
        if (Test-Path "configure.bat") {
            Write-Host "`n[Ejecutando] configure.bat" -ForegroundColor Cyan
            & ".\configure.bat"
        } else {
            Write-Host "[ERROR] configure.bat no encontrado" -ForegroundColor Red
        }
    }
    "4" {
        Write-Host "`nSesion cancelada." -ForegroundColor Yellow
        exit 0
    }
    default {
        Write-Host "`nOpcion no valida." -ForegroundColor Red
        exit 1
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Sesion finalizada" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
