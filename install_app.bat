@echo off
echo Instalando SleepAdvisor no dispositivo...

REM Verifica se o ADB está disponível
adb version
if %ERRORLEVEL% NEQ 0 (
    echo ERRO: ADB não encontrado no PATH.
    echo Por favor, adicione o diretório platform-tools do Android SDK ao PATH.
    pause
    exit /b 1
)

REM Verifica dispositivos conectados
echo Verificando dispositivos conectados...
adb devices
if %ERRORLEVEL% NEQ 0 (
    echo ERRO: Falha ao verificar dispositivos.
    pause
    exit /b 1
)

REM Instala o APK
echo Instalando APK...
adb install -r "app\build\outputs\apk\release\app-release.apk"
if %ERRORLEVEL% NEQ 0 (
    echo ERRO: Falha ao instalar o APK.
    pause
    exit /b 1
)

echo APK instalado com sucesso!
echo Iniciando aplicativo...
adb shell am start -n "com.example.sleepadvisor/.MainActivity"

echo Instalação concluída!
pause
