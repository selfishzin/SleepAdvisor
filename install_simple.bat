@echo off
echo Instalando SleepAdvisor no dispositivo...

REM Verifica se o ADB está no PATH
where adb > nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set ADB_CMD=adb
    goto :install
)

REM Verifica locais comuns
if exist "C:\Program Files\Android\Android Studio\platform-tools\adb.exe" (
    set "ADB_CMD=C:\Program Files\Android\Android Studio\platform-tools\adb.exe"
    goto :install
)

if exist "%LOCALAPPDATA%\Android\sdk\platform-tools\adb.exe" (
    set "ADB_CMD=%LOCALAPPDATA%\Android\sdk\platform-tools\adb.exe"
    goto :install
)

echo ADB não encontrado automaticamente.
echo.
echo Por favor, digite o caminho completo para o adb.exe:
set /p ADB_CMD=

:install
echo.
echo Usando ADB: %ADB_CMD%
echo.

REM Verifica dispositivos
echo Verificando dispositivos conectados...
"%ADB_CMD%" devices

REM Instala o APK
echo.
echo Instalando APK...
"%ADB_CMD%" install -r "app\build\outputs\apk\release\app-release.apk"

echo.
echo Iniciando aplicativo...
"%ADB_CMD%" shell am start -n "com.example.sleepadvisor/.MainActivity"

echo.
echo Processo concluído!
pause
