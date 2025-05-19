# Script de automação para build e instalação do SleepAdvisor
# Deve ser executado em PowerShell no Windows

$ErrorActionPreference = "Stop"
$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path

function Check-Command {
    param (
        [string]$Command
    )
    
    try {
        Get-Command $Command | Out-Null
        return $true
    } catch {
        return $false
    }
}

function Check-Prerequisites {
    Write-Host "Verificando pré-requisitos..." -ForegroundColor Yellow
    
    if (-not (Check-Command "adb")) {
        Write-Host "ERRO: ADB não encontrado no PATH. Instale o Android SDK e adicione adb ao PATH." -ForegroundColor Red
        exit 1
    }
    
    Write-Host "Verificando dispositivos conectados..." -ForegroundColor Yellow
    $devices = adb devices
    
    if ($devices.Count -le 1) {
        Write-Host "ERRO: Nenhum dispositivo Android conectado. Conecte um dispositivo e ative a depuração USB." -ForegroundColor Red
        exit 1
    }
    
    Write-Host "Pré-requisitos verificados com sucesso!" -ForegroundColor Green
}

function Build-ReleaseAPK {
    Write-Host "Compilando APK de release..." -ForegroundColor Yellow
    
    Push-Location $projectDir
    try {
        # Executa a tarefa Gradle para gerar o APK de release
        if (Test-Path "gradlew.bat") {
            & .\gradlew.bat clean assembleRelease --stacktrace
        } else {
            Write-Host "ERRO: gradlew.bat não encontrado no diretório do projeto." -ForegroundColor Red
            exit 1
        }
        
        $apkPath = "$projectDir\app\build\outputs\apk\release\app-release.apk"
        if (-not (Test-Path $apkPath)) {
            Write-Host "ERRO: APK não foi gerado. Verifique os logs do Gradle." -ForegroundColor Red
            exit 1
        }
        
        Write-Host "APK compilado com sucesso: $apkPath" -ForegroundColor Green
    } finally {
        Pop-Location
    }
}

function Install-APK {
    Write-Host "Instalando APK no dispositivo..." -ForegroundColor Yellow
    
    $apkPath = "$projectDir\app\build\outputs\apk\release\app-release.apk"
    & adb install -r $apkPath
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERRO: Falha ao instalar o APK. Código de saída: $LASTEXITCODE" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "APK instalado com sucesso!" -ForegroundColor Green
}

function Launch-App {
    Write-Host "Iniciando o aplicativo no dispositivo..." -ForegroundColor Yellow
    
    & adb shell am start -n "com.example.sleepadvisor/.MainActivity"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERRO: Falha ao iniciar o aplicativo. Código de saída: $LASTEXITCODE" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "Aplicativo iniciado com sucesso!" -ForegroundColor Green
}

function Start-LogCapture {
    Write-Host "Iniciando captura de logs (pressione Ctrl+C para parar)..." -ForegroundColor Yellow
    
    # Cria pasta de logs se não existir
    $logDir = "$projectDir\logs"
    if (-not (Test-Path $logDir)) {
        New-Item -ItemType Directory -Path $logDir | Out-Null
    }
    
    $logFile = "$logDir\sleep_advisor_log_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"
    Write-Host "Os logs serão salvos em: $logFile" -ForegroundColor Cyan
    
    & adb logcat -v threadtime | Select-String -Pattern "com.example.sleepadvisor" | Tee-Object -FilePath $logFile
}

function Show-TestInstructions {
    Write-Host "`n=========================================================" -ForegroundColor Cyan
    Write-Host "                  INSTRUÇÕES DE TESTE                  " -ForegroundColor Cyan
    Write-Host "=========================================================" -ForegroundColor Cyan
    Write-Host "1. Verifique se o aplicativo iniciou corretamente"
    Write-Host "2. Execute os testes conforme o checklist em test_release_adb.md"
    Write-Host "3. Para capturar logs, execute o comando: .\test_release_device.ps1 -logs"
    Write-Host "4. Para desinstalar o aplicativo: adb uninstall com.example.sleepadvisor"
    Write-Host "=========================================================" -ForegroundColor Cyan
    Write-Host "Pressione Enter para encerrar..." -ForegroundColor Yellow
    Read-Host
}

# Parâmetros de linha de comando
param (
    [switch]$build,
    [switch]$install,
    [switch]$launch,
    [switch]$logs,
    [switch]$all
)

# Se nenhum parâmetro for especificado, assume -all
if (-not ($build -or $install -or $launch -or $logs)) {
    $all = $true
}

# Processar fluxo baseado nos parâmetros
Check-Prerequisites

if ($build -or $all) {
    Build-ReleaseAPK
}

if ($install -or $all) {
    Install-APK
}

if ($launch -or $all) {
    Launch-App
}

if ($logs) {
    Start-LogCapture
} elseif ($all) {
    Show-TestInstructions
}
