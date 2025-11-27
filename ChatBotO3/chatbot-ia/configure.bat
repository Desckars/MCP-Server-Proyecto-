@echo off
cls
echo ========================================
echo   CONFIGURADOR DE API KEY
echo   Sistema de Encriptacion Automatica
echo ========================================
echo.

REM Verificar que existe el proyecto
if not exist "src\main\resources" (
    echo [ERROR] Directorio src\main\resources no encontrado
    echo Ejecuta este script desde la raiz del proyecto
    pause
    exit /b 1
)

echo Este script te ayudara a configurar tu API Key de Claude
echo de forma segura. El API Key sera encriptado automaticamente.
echo.
echo ========================================
echo   OPCIONES:
echo ========================================
echo.
echo   [1] Configurar Nuevo API Key
echo   [2] Ver Estado Actual
echo   [3] Eliminar Configuracion
echo   [4] Salir
echo.
echo ========================================
set /p opcion="Selecciona una opcion (1-4): "
echo.

if "%opcion%"=="1" goto :configurar
if "%opcion%"=="2" goto :ver_estado
if "%opcion%"=="3" goto :eliminar
if "%opcion%"=="4" goto :salir
echo Opcion invalida
pause
exit /b 1

:configurar
cls
echo ========================================
echo   CONFIGURAR API KEY
echo ========================================
echo.

REM Solicitar API Key
set /p API_KEY="Ingresa tu API Key de Anthropic: "

if "%API_KEY%"=="" (
    echo.
    echo [ERROR] API Key no puede estar vacio
    pause
    exit /b 1
)

REM Validar formato
echo %API_KEY% | findstr /C:"sk-ant-api03-" >nul
if errorlevel 1 (
    echo.
    echo [ERROR] Formato invalido
    echo El API Key debe comenzar con: sk-ant-api03-
    echo.
    echo Ejemplo: sk-ant-api03-ABC123...XYZ789
    pause
    exit /b 1
)

echo.
echo Validando formato... OK
echo.

REM Verificar si existe config.properties
if not exist "src\main\resources\config.properties" (
    echo Creando config.properties...
    echo # Anthropic Claude API Configuration > "src\main\resources\config.properties"
    echo anthropic.api-key=%API_KEY% >> "src\main\resources\config.properties"
    echo anthropic.model=claude-sonnet-4-20250514 >> "src\main\resources\config.properties"
    echo anthropic.max-tokens=4096 >> "src\main\resources\config.properties"
    echo. >> "src\main\resources\config.properties"
    echo # MCP O3 Server Configuration >> "src\main\resources\config.properties"
    echo mcp.o3.enabled=true >> "src\main\resources\config.properties"
    echo mcp.o3.jar-path=mcp/mcp_o3-0.0.4-SNAPSHOT.jar >> "src\main\resources\config.properties"
    echo mcp.o3.working-directory=%CD%\mcp_o3 >> "src\main\resources\config.properties"
) else (
    echo Actualizando config.properties existente...
    
    REM Crear backup
    copy "src\main\resources\config.properties" "src\main\resources\config.properties.backup" >nul 2>&1
    
    REM Actualizar usando PowerShell (mas confiable que batch puro)
    powershell -Command "$content = Get-Content 'src\main\resources\config.properties'; $updated = $false; $newContent = @(); foreach($line in $content) { if($line -match '^anthropic\.api-key=') { $newContent += 'anthropic.api-key=%API_KEY%'; $updated = $true } elseif($line -match '^anthropic\.api-key\.encrypted=') { $newContent += '# anthropic.api-key.encrypted= # REEMPLAZADO' } else { $newContent += $line } }; if(-not $updated) { $newContent += 'anthropic.api-key=%API_KEY%' }; $newContent | Set-Content 'src\main\resources\config.properties'"
)

echo.
echo ========================================
echo   CONFIGURACION GUARDADA
echo ========================================
echo.
echo [OK] API Key guardado en: src\main\resources\config.properties
echo [OK] Estado: En texto plano (se encriptara al iniciar)
echo.
echo Proximos pasos:
echo   1. Ejecuta: start.bat
echo   2. El sistema detectara el API Key
echo   3. Lo encriptara automaticamente
echo   4. Eliminara la version en texto plano
echo.
echo Tu API Key quedara protegido con encriptacion AES-256
echo.
pause
exit /b 0

:ver_estado
cls
echo ========================================
echo   ESTADO DE CONFIGURACION
echo ========================================
echo.

if not exist "src\main\resources\config.properties" (
    echo [ESTADO] Sin configurar
    echo.
    echo No existe el archivo config.properties
    echo Ejecuta la Opcion 1 para configurar
    echo.
    pause
    exit /b 0
)

echo Analizando config.properties...
echo.

REM Verificar API Key en texto plano
findstr /C:"sk-ant-api03-" "src\main\resources\config.properties" >nul 2>&1
if not errorlevel 1 (
    echo [ESTADO] Configurado ^(texto plano^)
    echo.
    echo Se detecta un API Key en texto plano.
    echo.
    echo [RECOMENDACION] Ejecuta el chatbot para que se
    echo                 encripte automaticamente.
    echo.
    for /f "tokens=2 delims==" %%a in ('findstr /C:"anthropic.api-key=" "src\main\resources\config.properties"') do (
        set KEY=%%a
    )
    echo API Key: %KEY:~0,15%...
    echo.
) else (
    REM Verificar API Key encriptado
    findstr /C:"anthropic.api-key.encrypted=" "src\main\resources\config.properties" | findstr /V /C:"anthropic.api-key.encrypted=$" >nul 2>&1
    if not errorlevel 1 (
        echo [ESTADO] Configurado ^(encriptado^)
        echo.
        echo Tu API Key esta protegido con encriptacion AES-256
        echo.
        for /f "tokens=2 delims==" %%a in ('findstr /C:"anthropic.api-key.encrypted=" "src\main\resources\config.properties"') do (
            set ENCRYPTED=%%a
        )
        echo Encriptado: %ENCRYPTED:~0,40%...
        echo.
    ) else (
        echo [ESTADO] Sin configurar
        echo.
        echo No se detecta un API Key configurado
        echo Ejecuta la Opcion 1 para configurar
        echo.
    )
)

REM Mostrar modelo y max tokens
for /f "tokens=2 delims==" %%a in ('findstr /C:"anthropic.model=" "src\main\resources\config.properties"') do (
    echo Modelo: %%a
)

for /f "tokens=2 delims==" %%a in ('findstr /C:"anthropic.max-tokens=" "src\main\resources\config.properties"') do (
    echo Max Tokens: %%a
)

echo.
pause
exit /b 0

:eliminar
cls
echo ========================================
echo   ELIMINAR CONFIGURACION
echo ========================================
echo.
echo [ADVERTENCIA] Esta accion eliminara tu API Key configurado
echo.
set /p confirmar="Estas seguro? (S/N): "

if /i not "%confirmar%"=="S" (
    echo.
    echo Operacion cancelada
    pause
    exit /b 0
)

if not exist "src\main\resources\config.properties" (
    echo.
    echo No hay configuracion para eliminar
    pause
    exit /b 0
)

echo.
echo Creando backup...
copy "src\main\resources\config.properties" "src\main\resources\config.properties.backup" >nul 2>&1

echo Eliminando API Key...

REM Usar PowerShell para limpiar las lineas de API Key
powershell -Command "$content = Get-Content 'src\main\resources\config.properties'; $newContent = @(); foreach($line in $content) { if($line -match '^anthropic\.api-key=') { $newContent += 'anthropic.api-key=' } elseif($line -match '^anthropic\.api-key\.encrypted=') { $newContent += '# anthropic.api-key.encrypted= # ELIMINADO' } else { $newContent += $line } }; $newContent | Set-Content 'src\main\resources\config.properties'"

echo.
echo [OK] API Key eliminado
echo [OK] Backup guardado en: config.properties.backup
echo.
echo Para configurar uno nuevo, ejecuta la Opcion 1
echo.
pause
exit /b 0

:salir
echo.
echo Saliendo...
exit /b 0