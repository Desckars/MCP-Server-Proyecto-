@echo off
cls
echo ========================================
echo   CHATBOT IA - Spring Boot Edition
echo   Claude + MCP O3
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

echo [1/6] Java detectado correctamente
echo.

REM Verificar archivo application.yml
if not exist "src\main\resources\application.yml" (
    echo [ADVERTENCIA] application.yml no encontrado
    echo Se usaran configuraciones por defecto
)

echo [2/6] Configuracion encontrada
echo.

REM Verificar API Key (desde variable de entorno o archivo)
set API_KEY_CONFIGURED=0

if not "%CLAUDE_API_KEY%"=="" (
    echo [3/6] API Key cargado desde variable de entorno CLAUDE_API_KEY
    set API_KEY_CONFIGURED=1
) else if exist "src\main\resources\config.properties" (
    findstr /C:"sk-ant-api03-" "src\main\resources\config.properties" >nul 2>&1
    if not errorlevel 1 (
        echo [3/6] API Key configurada ^(texto plano - se encriptara automaticamente^)
        set API_KEY_CONFIGURED=1
    ) else (
        findstr /C:"anthropic.api-key.encrypted=" "src\main\resources\config.properties" | findstr /V /C:"anthropic.api-key.encrypted=$" >nul 2>&1
        if not errorlevel 1 (
            echo [3/6] API Key configurada ^(encriptada^)
            set API_KEY_CONFIGURED=1
        ) else (
            echo [3/6] API Key NO configurada
            echo.
            echo     Se puede configurar de 3 formas:
            echo     1. Variable de entorno CLAUDE_API_KEY
            echo     2. application.yml con anthropic.api-key
            echo     3. config.properties con anthropic.api-key
            echo.
            set API_KEY_CONFIGURED=0
        )
    )
) else (
    echo [3/6] API Key NO configurada
    echo.
    echo     Se puede configurar de 3 formas:
    echo     1. Variable de entorno CLAUDE_API_KEY
    echo     2. application.yml con anthropic.api-key
    echo     3. config.properties con anthropic.api-key
    echo.
    set API_KEY_CONFIGURED=0
)

echo.

REM Compilar
echo [4/6] Compilando proyecto...
echo.
call mvn clean package -DskipTests -q
if errorlevel 1 (
    echo [ERROR] Fallo la compilacion
    echo.
    echo Intenta manualmente: mvn clean package
    pause
    exit /b 1
)

echo [5/6] Compilacion exitosa
echo.

REM Verificar JAR compilado
if not exist "target\chatbot-ia-*.jar" (
    echo [ERROR] JAR ejecutable no encontrado en target\
    echo.
    echo Ejecuta: mvn clean package
    pause
    exit /b 1
)

REM Encontrar el JAR
for /f "tokens=*" %%A in ('dir /b target\chatbot-ia-*.jar') do set JAR_FILE=%%A

echo [INFO] JAR compilado: %JAR_FILE%
echo.

echo [6/6] Todo listo
echo.
echo ========================================
echo   SELECCIONA MODO DE EJECUCION:
echo ========================================
echo.
echo   [1] Iniciar Aplicación (Interfaz GUI - recomendada)
echo   [2] Interfaz Web - http://localhost:8080
echo   [3] Consola Web - http://localhost:8080/console
echo   [4] Configurar API Key (Manual)
echo   [5] Cancelar
echo.
echo ========================================

set /p opcion="Opcion (1-5): "

if "%opcion%"=="1" (
    echo.
    echo ========================================
    echo   INICIANDO APLICACION (GUI)
    echo ========================================
    echo.

    echo Ejecutando: java -jar target\%JAR_FILE% --gui
    echo Abre la ventana de la aplicación (GUI)
    echo.

    java -jar "target\%JAR_FILE%" --gui

) else if "%opcion%"=="2" (
    echo.
    echo ========================================
    echo   INICIANDO INTERFAZ WEB
    echo ========================================
    echo.
    
    if "%API_KEY_CONFIGURED%"=="0" (
        echo [NOTA] Si no hay API Key puede aparecer un aviso en la UI web.
        echo.
        timeout /t 2 >nul
    )
    
    echo Ejecutando: java -jar target\%JAR_FILE%
    echo Abre tu navegador en: http://localhost:8080
    echo.
    
    java -jar "target\%JAR_FILE%"

) else if "%opcion%"=="3" (
    echo.
    echo ========================================
    echo   CONFIGURACION MANUAL DE API KEY
    echo ========================================
    echo.
    echo Este proceso encriptara tu API Key de forma segura.
    echo.
    echo Formato esperado: sk-ant-api03-XXXXXXXXXXXXXXXXXXXX
    echo.
    
    set "API_KEY_INPUT="
    set /p API_KEY_INPUT="Ingresa tu API Key: "
    
    if not defined API_KEY_INPUT (
        echo.
        echo [ERROR] API Key no puede estar vacio
        echo        Presione cualquier tecla para volver al menu...
        pause >nul
        cls
        goto :eof
    )
    
    if "%API_KEY_INPUT%"=="" (
        echo.
        echo [ERROR] API Key no puede estar vacio
        echo        Presione cualquier tecla para volver al menu...
        pause >nul
        cls
        goto :eof
    )
    
    echo %API_KEY_INPUT% | findstr /C:"sk-ant-api03-" >nul
    if errorlevel 1 (
        echo.
        echo [ERROR] Formato invalido
        echo        El API Key debe comenzar con: sk-ant-api03-
        echo.
        echo        Ejemplo: sk-ant-api03-ABC123...XYZ789
        echo.
        echo        Presione cualquier tecla para volver al menu...
        pause >nul
        cls
        goto :eof
    )
    
    echo.
    echo Guardando en application.yml...
    
    REM Crear backup
    if exist "src\main\resources\application.yml" (
        copy "src\main\resources\application.yml" "src\main\resources\application.yml.backup" >nul 2>&1
    )
    
    REM Actualizar API Key en application.yml
    if exist "src\main\resources\application.yml" (
        powershell -Command "(Get-Content 'src\main\resources\application.yml') -replace 'api-key: .*$', 'api-key: %API_KEY_INPUT%' | Set-Content 'src\main\resources\application.yml'"
    ) else (
        REM Si no existe, crear uno nuevo
        echo anthropic: > "src\main\resources\application.yml"
        echo   api-key: %API_KEY_INPUT% >> "src\main\resources\application.yml"
        echo   model: claude-sonnet-4-20250514 >> "src\main\resources\application.yml"
        echo   max-tokens: 4096 >> "src\main\resources\application.yml"
    )
    
    echo.
    echo [OK] API Key guardado
    echo      La aplicacion usara automaticamente esta configuracion
    echo.
    pause
    
    REM Volver al menu
    cls
    goto :eof
    
) else if "%opcion%"=="4" (
    echo.
    echo Cancelado por el usuario
    exit /b 0
    
) else (
    echo.
    echo [ERROR] Opcion invalida
    pause
    exit /b 1
)

echo.
echo ========================================
echo   Sesion finalizada
echo ========================================
echo.
pause
