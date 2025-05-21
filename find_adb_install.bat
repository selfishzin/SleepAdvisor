@echo off
setlocal enabledelayedexpansion

echo Procurando ADB e instalando SleepAdvisor...

REM Lista de locais comuns onde o ADB pode estar
set "LOCATIONS=^
C:\Program Files\Android\Android Studio\platform-tools\adb.exe^
C:\Program Files (x86)\Android\android-sdk\platform-tools\adb.exe^
%LOCALAPPDATA%\Android\sdk\platform-tools\adb.exe^
%USERPROFILE%\AppData\Local\Android\sdk\platform-tools\adb.exe^
%ANDROID_HOME%\platform-tools\adb.exe^
%ANDROID_SDK_ROOT%\platform-tools\adb.exe"

set "ADB_PATH="

REM Verifica cada local
for %%L in (%LOCATIONS%) do (
    if exist "%%L" (
        set "ADB_PATH=%%L"
        echo ADB encontrado em: !ADB_PATH!
        goto :found
    )
)

echo ADB não encontrado em nenhum local comum.
echo.
echo Por favor, informe o caminho completo para o adb.exe:
set /p ADB_PATH=

if not exist "!ADB_PATH!" (
    echo Caminho inválido ou ADB não encontrado.
    pause
    exit /b 1
)

:found
echo.
echo Usando ADB em: !ADB_PATH!
echo.

REM Verifica dispositivos conectados
echo Verificando dispositivos conectados...
"!ADB_PATH!" devices
if %ERRORLEVEL% NEQ 0 (
    echo ERRO: Falha ao verificar dispositivos.
    pause
    exit /b 1
)

REM Instala o APK
echo.
echo Instalando APK...
"!ADB_PATH!" install -r "app\build\outputs\apk\release\app-release.apk"
if %ERRORLEVEL% NEQ 0 (
    echo ERRO: Falha ao instalar o APK.
    pause
    exit /b 1
)

echo.
echo APK instalado com sucesso!
echo Iniciando aplicativo...
"!ADB_PATH!" shell am start -n "com.example.sleepadvisor/.MainActivity"

echo.
echo Instalação concluída!
pause
