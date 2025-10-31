@echo off
echo ========================================
echo   LIMPIEZA DE ARCHIVOS OBSOLETOS
echo ========================================
echo.

echo Eliminando archivos obsoletos de la version anterior...
echo.

REM Eliminar O3Config (ya no se usa, ahora es MCPConfig)
if exist "src\main\java\com\chatbot\config\O3Config.java" (
    del /F /Q "src\main\java\com\chatbot\config\O3Config.java"
    echo [X] Eliminado: O3Config.java
) else (
    echo [OK] O3Config.java ya no existe
)

REM Eliminar O3Service (ya no se usa, ahora es MCPService)
if exist "src\main\java\com\chatbot\service\O3Service.java" (
    del /F /Q "src\main\java\com\chatbot\service\O3Service.java"
    echo [X] Eliminado: O3Service.java
) else (
    echo [OK] O3Service.java ya no existe
)

REM Eliminar App.java (no se usa, Main.java es el punto de entrada)
if exist "src\main\java\com\chatbot\App.java" (
    del /F /Q "src\main\java\com\chatbot\App.java"
    echo [X] Eliminado: App.java
) else (
    echo [OK] App.java ya no existe
)

REM Eliminar lib folder (ya no usamos O3 directo)
if exist "lib" (
    rmdir /S /Q "lib"
    echo [X] Eliminado: carpeta lib/
) else (
    echo [OK] Carpeta lib/ ya no existe
)

REM Eliminar Readme.txt viejo
if exist "Readme.txt" (
    del /F /Q "Readme.txt"
    echo [X] Eliminado: Readme.txt viejo
) else (
    echo [OK] Readme.txt ya no existe
)

echo.
echo ========================================
echo   LIMPIEZA COMPLETADA
echo ========================================
echo.
echo Archivos eliminados:
echo   - O3Config.java (obsoleto)
echo   - O3Service.java (obsoleto)
echo   - App.java (no usado)
echo   - lib/ (librerias O3 obsoletas)
echo   - Readme.txt (viejo)
echo.
echo Ahora ejecuta:
echo   mvn clean compile
echo.
pause
