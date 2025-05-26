package com.example.sleepadvisor.domain.model

import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt
import com.example.sleepadvisor.domain.model.SleepMetrics

/**
 * Análises avançadas para a classe SleepSession.
 * Fornece métodos para calcular métricas e insights sobre os padrões de sono.
 */

/**
 * Calcula a eficiência do sono com base no tempo total na cama vs tempo dormindo.
 * @return Eficiência do sono em porcentagem (0-100)
 */
fun SleepSession.calculateSleepEfficiency(): Int {
    return SleepMetrics.calculateSleepEfficiency(stages, duration)
}

/**
 * Calcula o tempo total gasto em cada estágio de sono.
 * @return Mapa com o tempo gasto em cada estágio
 */
fun SleepSession.getTimeByStage(): Map<SleepStageType, Duration> {
    return SleepMetrics.calculateTimeByStage(stages)
}

/**
 * Verifica se o horário de dormir está dentro da janela ideal para um adulto.
 * @return true se o horário de dormir for considerado ideal
 */
fun SleepSession.isBedtimeOptimal(): Boolean {
    val bedHour = startTime.atZone(ZoneId.systemDefault()).hour
    // Horário ideal: entre 20h e 23h
    return bedHour in 20..23
}

/**
 * Calcula a latência do sono (tempo para adormecer após deitar).
 * @return Duração da latência do sono ou null se não for possível determinar
 */
fun SleepSession.calculateSleepLatency(): Duration? {
    return SleepMetrics.calculateSleepLatency(stages, startTime)
}

/**
 * Calcula a eficiência do sono REM.
 * @return Porcentagem do sono REM em relação ao tempo total de sono (0-100)
 */
fun SleepSession.calculateREMEfficiency(): Int {
    val timeByStage = getTimeByStage()
    val totalSleepTime = timeByStage
        .filter { it.key != SleepStageType.AWAKE }
        .values.sumOf { it.seconds }
    
    if (totalSleepTime <= 0) return 0
    
    val remTime = timeByStage[SleepStageType.REM]?.seconds ?: 0
    return ((remTime.toDouble() / totalSleepTime) * 100).roundToInt()
}

/**
 * Gera um resumo das métricas de sono.
 * @return Mapa com as métricas calculadas
 */
fun SleepSession.generateSleepMetrics(): Map<String, Any> {
    val timeByStage = getTimeByStage()
    val totalSleepTime = timeByStage
        .filter { it.key != SleepStageType.AWAKE }
        .values.sumOf { it.seconds }
    
    return mapOf(
        "sleepEfficiency" to calculateSleepEfficiency(),
        "totalSleepTime" to Duration.ofSeconds(totalSleepTime),
        "timeByStage" to timeByStage,
        "isBedtimeOptimal" to isBedtimeOptimal(),
        "sleepLatency" to (calculateSleepLatency()?.seconds?.let { Duration.ofSeconds(it) } ?: Duration.ZERO),
        "remEfficiency" to calculateREMEfficiency(),
        "awakenings" to wakeDuringNightCount,
        "hasValidStages" to hasValidStages()
    )
}

/**
 * Gera recomendações personalizadas com base nos padrões de sono.
 * @return Lista de recomendações
 */
fun SleepSession.generateSleepRecommendations(): List<String> {
    val recommendations = mutableListOf<String>()
    val metrics = generateSleepMetrics()
    
    // Recomendações baseadas na eficiência do sono
    val efficiency = metrics["sleepEfficiency"] as Int
    when {
        efficiency < 85 -> recommendations.add("Sua eficiência de sono está baixa (${efficiency}%). Tente criar um ambiente mais propício para o sono.")
        efficiency < 90 -> recommendations.add("Sua eficiência de sono está boa (${efficiency}%), mas pode melhorar.")
        else -> recommendations.add("Ótima eficiência de sono (${efficiency}%)! Continue mantendo esses hábitos.")
    }
    
    // Recomendações baseadas no horário de dormir
    if (!isBedtimeOptimal()) {
        recommendations.add("Tente dormir entre 20h e 23h para um sono mais reparador.")
    }
    
    // Recomendações baseadas na latência do sono
    val latency = metrics["sleepLatency"] as Duration
    if (latency > Duration.ofMinutes(30)) {
        recommendations.add("Você está demorando muito para adormecer (${latency.toMinutes()} minutos). Considere práticas de relaxamento antes de dormir.")
    }
    
    // Recomendações baseadas no sono REM
    val remEfficiency = metrics["remEfficiency"] as Int
    when {
        remEfficiency < 20 -> recommendations.add("Seu tempo de sono REM está baixo. Tente dormir mais cedo para aumentar o sono REM.")
        remEfficiency > 30 -> recommendations.add("Seu tempo de sono REM está alto, o que pode indicar privação de sono. Tite dormir mais horas por noite.")
    }
    
    // Recomendações baseadas em despertares noturnos
    if (wakeDuringNightCount > 2) {
        recommendations.add("Você está acordando muitas vezes durante a noite. Evite cafeína e telas antes de dormir.")
    }
    
    return recommendations
}
