package com.example.sleepadvisor.domain.model

import com.example.sleepadvisor.domain.model.SleepMetrics
import java.time.Duration
import java.time.format.DateTimeFormatter

/**
 * Funções de extensão para a classe SleepSession
 */

/**
 * Calcula a porcentagem de tempo gasto em um determinado tipo de estágio de sono.
 * Se a duração total da SESSÃO DE SONO for zero ou negativa, OU se não houver estágios,
 * retorna valores estimados padrão seguindo a distribuição típica de 55% sono leve, 25% sono profundo e 20% REM.
 *
 * @param type Tipo do estágio de sono para calcular a porcentagem
 * @return Porcentagem de tempo gasto no estágio especificado (0-100)
 */
fun SleepSession.getStagePercentage(type: SleepStageType): Double {
    // Se a duração total da SESSÃO DE SONO for zero ou negativa, OU se não houver estágios,
    // retorna valores estimados padrão.
    if (this.duration.isZero || this.duration.isNegative || stages.isEmpty()) {
        return when (type) {
            SleepStageType.LIGHT -> SleepMetrics.DEFAULT_LIGHT_SLEEP_PERCENTAGE
            SleepStageType.DEEP -> SleepMetrics.DEFAULT_DEEP_SLEEP_PERCENTAGE
            SleepStageType.REM -> SleepMetrics.DEFAULT_REM_SLEEP_PERCENTAGE
            else -> 0.0 // AWAKE, UNKNOWN, etc. will be 0
        }
    }

    // Usa o SleepMetrics para calcular a porcentagem do estágio
    return SleepMetrics.calculateStagePercentage(stages, type)
}

/**
 * Verifica se os estágios de sono desta sessão são estimados ou reais.
 * 
 * @return true se os estágios são estimados, false se são dados reais
 */
fun SleepSession.hasEstimatedStages(): Boolean {
    return source == SleepSource.SIMULATION || stages.any { it.source == SleepSource.SIMULATION }
}

/**
 * Verifica se os estágios de sono desta sessão são válidos.
 * Estágios são considerados válidos se:
 * 1. A sessão tem estágios com duração total maior que zero, OU
 * 2. As porcentagens dos estágios foram definidas explicitamente (maiores que zero)
 * 
 * @return true se os estágios são válidos, false caso contrário
 */
fun SleepSession.hasValidStages(): Boolean {
    // Se não há estágios, verifica se as porcentagens foram definidas explicitamente
    if (stages.isEmpty()) {
        return lightSleepPercentage > 0 || deepSleepPercentage > 0 || remSleepPercentage > 0
    }
    
    // Se há estágios, verifica se a soma das durações é maior que zero
    val totalStageDuration = stages.sumOf { it.duration.seconds }
    return totalStageDuration > 0
}

/**
 * Atualiza os percentuais de estágios de sono no objeto SleepSession
 * com base nos estágios registrados.
 *
 * @return SleepSession atualizado com os percentuais calculados
 */
fun SleepSession.calculateAndUpdateStagePercentages(): SleepSession {
    if (stages.isEmpty()) {
        return this.copy(
            lightSleepPercentage = SleepMetrics.DEFAULT_LIGHT_SLEEP_PERCENTAGE,
            deepSleepPercentage = SleepMetrics.DEFAULT_DEEP_SLEEP_PERCENTAGE,
            remSleepPercentage = SleepMetrics.DEFAULT_REM_SLEEP_PERCENTAGE
        )
    }
    
    // Usa o SleepMetrics para calcular todas as porcentagens de uma vez
    val percentages = SleepMetrics.calculateAllStagePercentages(stages)
    
    return this.copy(
        lightSleepPercentage = percentages[SleepStageType.LIGHT] ?: 0.0,
        deepSleepPercentage = percentages[SleepStageType.DEEP] ?: 0.0,
        remSleepPercentage = percentages[SleepStageType.REM] ?: 0.0
    )
}

/**
 * Cria uma análise diária simplificada a partir de uma sessão de sono
 */
fun SleepSession.toDailyAnalysis(): DailyAnalysis {
    val sleepScore = when {
        efficiency > 90 -> 90 + ((efficiency - 90) / 10 * 10).toInt()
        efficiency > 80 -> 80 + ((efficiency - 80) / 10 * 10).toInt()
        efficiency > 70 -> 70 + ((efficiency - 70) / 10 * 10).toInt()
        efficiency > 60 -> 60 + ((efficiency - 60) / 10 * 10).toInt()
        else -> (efficiency * 0.6).toInt()
    }
    
    val sleepQuality = when {
        sleepScore >= 90 -> "Excelente"
        sleepScore >= 80 -> "Bom"
        sleepScore >= 70 -> "Regular"
        sleepScore >= 60 -> "Razoável"
        else -> "Insuficiente"
    }
    
    val analysis = when {
        efficiency >= 90 -> "Você teve uma noite de sono excelente com alta eficiência. Seu corpo conseguiu descansar adequadamente."
        efficiency >= 80 -> "Você teve uma boa noite de sono. Sua eficiência está dentro dos parâmetros recomendados."
        efficiency >= 70 -> "Sua noite de sono foi regular. Há espaço para melhorias na eficiência do sono."
        efficiency >= 60 -> "Sua eficiência do sono está abaixo do ideal. Considere ajustar seus hábitos de sono."
        else -> "Sua eficiência do sono está significativamente baixa. Recomendamos consultar um especialista."
    }
    
    val recommendations = mutableListOf<String>()
    
    // Recomendações baseadas na eficiência
    if (efficiency < 80) {
        recommendations.add("Tente manter um horário regular para dormir e acordar, mesmo nos finais de semana.")
    }
    
    // Recomendações baseadas na duração
    val durationHours = duration.toHours()
    when {
        durationHours < 6 -> recommendations.add("Sua duração de sono está abaixo do recomendado. Tente dormir pelo menos 7 horas por noite.")
        durationHours > 9 -> recommendations.add("Você dormiu mais do que o recomendado. Excesso de sono pode causar sonolência durante o dia.")
    }
    
    // Recomendações baseadas nos despertares
    if (wakeDuringNightCount > 2) {
        recommendations.add("Você acordou ${wakeDuringNightCount} vezes durante a noite. Evite cafeína e álcool antes de dormir para reduzir despertares.")
    }
    
    // Limitar a 3 recomendações
    val finalRecommendations = if (recommendations.isEmpty()) {
        listOf("Continue mantendo bons hábitos de sono para preservar sua saúde.")
    } else {
        recommendations.take(3)
    }
    
    // Formatar a data
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val formattedDate = startTime.atZone(java.time.ZoneId.systemDefault()).format(dateFormatter)
    
    return DailyAnalysis(
        date = formattedDate,
        analysis = analysis,
        recommendations = finalRecommendations,
        sleepScore = sleepScore,
        sleepQuality = sleepQuality,
        sleepStages = stages
    )
}
