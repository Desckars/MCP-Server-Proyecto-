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
    echo.
    echo Creando config.properties con valores por defecto...
    echo # Anthropic Claude API Configuration > "src\main\resources\config.properties"
    echo anthropic.api-key= >> "src\main\resources\config.properties"
    echo anthropic.model=claude-sonnet-4-20250514 >> "src\main\resources\config.properties"
    echo anthropic.max-tokens=4096 >> "src\main\resources\config.properties"
    echo. >> "src\main\resources\config.properties"
    echo # MCP O3 Server Configuration >> "src\main\resources\config.properties"
    echo mcp.o3.enabled=true >> "src\main\resources\config.properties"
    echo mcp.o3.jar-path=mcp/mcp_o3-0.0.4-SNAPSHOT.jar >> "src\main\resources\config.properties"
    echo mcp.o3.working-directory=%CD%\mcp_o3 >> "src\main\resources\config.properties"
    echo.
    echo [OK] Archivo creado. Se te pedira el API Key al iniciar.
    echo.
)

echo [2/5] Configuracion encontrada
echo.

REM Verificar API Key (en texto plano O encriptado)
set API_KEY_CONFIGURED=0

REM Buscar API Key en texto plano
findstr /C:"sk-ant-api03-" "src\main\resources\config.properties" >nul 2>&1
if not errorlevel 1 (
    echo [3/5] API Key configurada ^(texto plano - se encriptara automaticamente^)
    set API_KEY_CONFIGURED=1
) else (
    REM Buscar API Key encriptada
    findstr /C:"anthropic.api-key.encrypted=" "src\main\resources\config.properties" | findstr /V /C:"anthropic.api-key.encrypted=$" >nul 2>&1
    if not errorlevel 1 (
        echo [3/5] API Key configurada ^(encriptada^)
        set API_KEY_CONFIGURED=1
    ) else (
        echo [3/5] API Key NO configurada
        echo.
        echo     Se abrira la ventana de configuracion al iniciar
        echo     para que ingreses tu API Key de forma segura.
        echo.
        set API_KEY_CONFIGURED=0
    )
)

echo.

REM Compilar
echo [4/5] Compilando proyecto...
echo.
call mvn clean package -DskipTests -q
if errorlevel 1 (
    echo [ERROR] Fallo la compilacion
    echo.
    echo Ejecuta manualmente: mvn clean package
    pause
    exit /b 1
)

echo [5/5] Compilacion exitosa
echo.

REM Verificar JAR compilado (con dependencias)
if not exist "target\chatbot-ia-executable.jar" (
    echo [ERROR] JAR ejecutable no encontrado en target\
    echo.
    echo Ejecuta: mvn clean package
    pause
    exit /b 1
)

echo [INFO] Usando JAR con dependencias: chatbot-ia-executable.jar
echo.

REM Verificar MCP (embebido en resources)
if not exist "src\main\resources\mcp\mcp_o3-0.0.4-SNAPSHOT.jar" (
    echo [ADVERTENCIA] MCP O3 no encontrado en resources
    echo Copia el JAR a: src\main\resources\mcp\
    echo O compila con: cd ..\mcp_o3 ^&^& mvn clean package
    echo.
    echo El chatbot funcionara sin MCP O3 ^(solo Claude AI^)
    timeout /t 3 >nul
) else (
    echo [INFO] MCP O3 encontrado y listo
)

echo.
echo ========================================
echo   SELECCIONA MODO DE EJECUCION:
echo ========================================
echo.
echo   [1] Interfaz Grafica (Recomendado)
echo   [2] Modo Consola
echo   [3] Configurar API Key (Manual)
echo   [4] Cancelar
echo.
echo ========================================

set /p opcion="Opcion (1-4): "

if "%opcion%"=="1" (
    echo.
    echo ========================================
    echo   INICIANDO INTERFAZ GRAFICA
    echo ========================================
    echo.
    
    if "%API_KEY_CONFIGURED%"=="0" (
        echo [NOTA] Se abrira la ventana de configuracion
        echo        para que ingreses tu API Key.
        echo.
        timeout /t 2 >nul
    )
    
    echo Ejecutando: java -jar target\chatbot-ia-executable.jar
    echo.
    java -jar "target\chatbot-ia-executable.jar"
    
) else if "%opcion%"=="2" (
    echo.
    echo ========================================
    echo   INICIANDO MODO CONSOLA
    echo ========================================
    echo.
    
    if "%API_KEY_CONFIGURED%"=="0" (
        echo [ERROR] Modo consola requiere API Key configurado
        echo.
        echo Por favor:
        echo   1. Ejecuta primero la interfaz grafica ^(Opcion 1^)
        echo   2. Configura tu API Key
        echo   3. Luego podras usar el modo consola
        echo.
        pause
        exit /b 1
    )
    
    echo Ejecutando: java -jar target\chatbot-ia-executable.jar --console
    echo.
    java -jar "target\chatbot-ia-executable.jar" --console
    
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
    echo Guardando en config.properties...
    
    REM Crear backup
    copy "src\main\resources\config.properties" "src\main\resources\config.properties.backup" >nul 2>&1
    
    REM Actualizar API Key en texto plano (se encriptara automaticamente)
    powershell -Command "(Get-Content 'src\main\resources\config.properties') -replace '^anthropic\.api-key=.*$', 'anthropic.api-key=%API_KEY_INPUT%' | Set-Content 'src\main\resources\config.properties'"
    
    echo.
    echo [OK] API Key guardado
    echo      Se encriptara automaticamente al iniciar el chatbot
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