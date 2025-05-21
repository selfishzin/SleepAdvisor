# SleepAdvisor

Aplicativo Android para análise avançada de sono, desenvolvido com Kotlin e Jetpack Compose.

## Funcionalidades

- **Análise Detalhada do Sono**: Simulação realista de ciclos de sono (4-6 ciclos de 90 minutos)
- **Estágios do Sono**: Análise detalhada dos estágios do sono (Leve, Profundo, REM)
- **Pontuação de Qualidade**: Pontuação de qualidade do sono (0-100) com classificação verbal
- **Análise de Sonecas**: Detecção e análise de sonecas (naps)
- **Métricas de Saúde**: Simulação de métricas como frequência cardíaca, saturação de oxigênio e ronco
- **Recomendações Personalizadas**: Fatos científicos personalizados sobre o sono
- **Tendências Semanais**: Detecção de tendências e padrões semanais
- **Horários Ideais**: Recomendações de horário ideal para sonecas

## Arquitetura

O aplicativo segue a arquitetura MVVM + Clean Architecture com as seguintes camadas:

- **Presentation (UI)**: ViewModels com StateFlow para gerenciamento de estado
- **Domain**: UseCases para cada funcionalidade específica
- **Data**: Implementações de repositório e fontes de dados
- **Di**: Injeção de dependências com Hilt

## Tecnologias

- Kotlin
- Jetpack Compose
- Room Database
- Health Connect API
- Hilt para injeção de dependências
- Coroutines e Flow
