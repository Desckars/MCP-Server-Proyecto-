@echo off
cls
echo ========================================
echo   ENCRIPTADOR DE API KEY
echo ========================================
echo.

REM Verificar que existe el JAR compilado
if not exist "target\chatbot-ia-2.0-SNAPSHOT.jar" (
    echo [ERROR] Proyecto no compilado
    echo Ejecuta primero: mvn clean package
    pause
    exit /b 1
)

echo Este script te ayudara a encriptar tu API Key de forma segura.
echo.
echo PASO 1: Ingresa tu API Key de Claude
echo.
set /p API_KEY="API Key (sk-ant-api03-...): "

if "%API_KEY%"=="" (
    echo Error: API Key no puede estar vacio
    pause
    exit /b 1
)

echo.
echo PASO 2: Crea una contraseña maestra
echo         (Necesitaras esta contraseña cada vez que inicies el chatbot)
echo.
set /p PASSWORD="Contraseña maestra: "

if "%PASSWORD%"=="" (
    echo Error: Contraseña no puede estar vacia
    pause
    exit /b 1
)

echo.
echo Encriptando...
echo.

java -cp "target\chatbot-ia-2.0-SNAPSHOT.jar" com.chatbot.security.EncryptionUtil encrypt "%API_KEY%" "%PASSWORD%"

if errorlevel 1 (
    echo.
    echo [ERROR] Fallo la encriptacion
    pause
    exit /b 1
)

echo.
echo ========================================
echo   ENCRIPTACION EXITOSA
echo ========================================
echo.
echo Ahora debes:
echo 1. Copiar el valor encriptado (la linea larga)
echo 2. Editar src\main\resources\config.properties
echo 3. Pegar en: anthropic.api-key.encrypted=
echo 4. Comentar cualquier API key en texto plano
echo.
echo Al iniciar el chatbot, se te pedira la contraseña.
echo.
pause