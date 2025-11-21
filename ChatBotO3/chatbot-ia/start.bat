@echo off
cls
echo ========================================
echo   CHATBOT IA - Claude + MCP O3
echo   Inicio Rapido
echo ========================================
echo.

REM Verificar Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java no encontrado
    echo Instala Java 21: https://adoptium.net/
    pause
    exit /b 1
)

echo [1/5] Java detectado correctamente
echo.

REM Verificar config.properties
if not exist "src\main\resources\config.properties" (
    echo [ERROR] config.properties no encontrado
    pause
    exit /b 1
)

echo [2/5] Configuracion encontrada
echo.

REM Verificar API Key
findstr /C:"sk-ant-api03-" "src\main\resources\config.properties" >nul
if errorlevel 1 (
    echo [ADVERTENCIA] API Key no configurada correctamente
    echo Edita: src\main\resources\config.properties
    echo.
    set /p continuar="Continuar de todos modos? (S/N): "
    if /i not "%continuar%"=="S" exit /b 0
)

echo [3/5] API Key configurada
echo.

REM Compilar
echo [4/5] Compilando proyecto...
echo.
call mvn clean compile -q
if errorlevel 1 (
    echo [ERROR] Fallo la compilacion
    echo Ejecuta manualmente: mvn clean compile
    pause
    exit /b 1
)

echo [5/5] Compilacion exitosa
echo.

REM Verificar MCP (embebido en resources)
if not exist "src\main\resources\mcp\mcp_o3-0.0.4-SNAPSHOT.jar" (
    echo [ADVERTENCIA] MCP O3 no encontrado en resources
    echo Copia el JAR a: src\main\resources\mcp\
    echo O compila con: cd ..\mcp_o3 ^&^& mvn clean package
    timeout /t 3 >nul
) else (
    echo [INFO] MCP O3 encontrado 
)

echo.
echo ========================================
echo   SELECCIONA MODO DE EJECUCION:
echo ========================================
echo.
echo   [1] Interfaz Grafica (Recomendado)
echo   [2] Modo Consola
echo   [3] Cancelar
echo.
echo ========================================

set /p opcion="Opcion (1-3): "

if "%opcion%"=="1" (
    echo.
    echo Iniciando interfaz grafica...
    echo.
    call mvn exec:java
) else if "%opcion%"=="2" (
    echo.
    echo Iniciando modo consola...
    echo.
    call mvn exec:java -Dexec.args="--console"
) else if "%opcion%"=="3" (
    echo Cancelado
    exit /b 0
) else (
    echo Opcion invalida
    pause
    goto :eof
)

echo.
echo ========================================
echo   Sesion finalizada
echo ========================================
pause
