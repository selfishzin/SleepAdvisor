# Teste de Release com ADB - SleepAdvisor

Este documento descreve o processo para compilar, instalar e testar o aplicativo SleepAdvisor via ADB em um dispositivo real.

## Pré-requisitos

1. Dispositivo Android com o modo de desenvolvedor e depuração USB ativados
2. Computador com Android SDK instalado (incluindo o ADB)
3. Cabo USB para conectar o dispositivo ao computador

## 1. Preparação do Build de Release

### 1.1 Verificar configurações de release

Verifique se as configurações de release estão corretas em `app/build.gradle`:

```gradle
buildTypes {
    release {
        minifyEnabled true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        signingConfig signingConfigs.debug // Para testes. Em produção real, usaríamos uma chave de assinatura de produção
    }
}
```

### 1.2 Compilar o APK de Release

Execute o seguinte comando na raiz do projeto:

```bash
./gradlew assembleRelease
```

O APK será gerado em `app/build/outputs/apk/release/app-release.apk`

## 2. Instalação via ADB

### 2.1 Verificar conexão do dispositivo

```bash
adb devices
```

Este comando deve listar seu dispositivo conectado.

### 2.2 Instalar o APK

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

A flag `-r` substitui a instalação existente, se houver.

## 3. Checklist de Testes Manuais no Dispositivo Real

### 3.1 Inicialização e Permissões

- [ ] Iniciar o aplicativo - verificar se a tela inicial é exibida sem problemas
- [ ] Verificar se o diálogo de permissões do Health Connect é exibido
- [ ] Conceder as permissões e verificar se o aplicativo responde adequadamente

### 3.2 Fluxo de Entrada de Dados

- [ ] Adicionar uma nova entrada manual de sono com todos os campos preenchidos
- [ ] Verificar se a entrada foi salva corretamente na tela principal
- [ ] Tentar adicionar uma segunda entrada para o mesmo dia e verificar se o diálogo de substituição aparece
- [ ] Editar uma entrada existente e verificar se as alterações são salvas

### 3.3 Cálculos e Visualização

- [ ] Verificar se a eficiência do sono é calculada e exibida corretamente
- [ ] Verificar se os percentuais de sono (leve, profundo, REM) são exibidos
- [ ] Verificar se o número de despertares está sendo considerado no cálculo da eficiência

### 3.4 Integração com IA

- [ ] Verificar se as recomendações da IA são exibidas após ter dados suficientes
- [ ] Testar o comportamento offline (ativar modo avião)
- [ ] Verificar se mensagens de erro adequadas são exibidas quando não há conexão

### 3.5 Desempenho e Responsividade

- [ ] Navegar rapidamente entre as telas para verificar fluidez
- [ ] Adicionar várias entradas em sequência para testar a estabilidade
- [ ] Verificar se a aplicação funciona bem com diferentes tamanhos de tela (girar o dispositivo)

### 3.6 Acessibilidade

- [ ] Ativar o TalkBack e verificar se todos os elementos são anunciados corretamente
- [ ] Verificar navegação por teclado (se estiver usando um teclado físico)
- [ ] Aumentar o tamanho da fonte nas configurações do sistema e verificar se o layout se adapta

## 4. Logs e Diagnóstico

Para capturar logs durante o teste:

```bash
adb logcat -v threadtime > sleep_advisor_logcat.txt
```

Para filtrar apenas os logs do aplicativo:

```bash
adb logcat -v threadtime | grep "com.example.sleepadvisor" > sleep_advisor_app_logs.txt
```

## 5. Desinstalação (se necessário)

```bash
adb uninstall com.example.sleepadvisor
```

## 6. Documentação de Bugs

Document qualquer problema encontrado com os seguintes detalhes:
- Passos para reproduzir
- Comportamento esperado vs. comportamento real
- Screenshots ou videos
- Logs relevantes
- Informações do dispositivo (modelo, versão do Android)
