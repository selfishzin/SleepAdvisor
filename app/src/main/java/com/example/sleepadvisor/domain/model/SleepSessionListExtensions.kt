package com.example.sleepadvisor.domain.model

import java.time.Duration
import java.time.LocalTime
import kotlin.math.roundToInt

/**
 * Extensões para listas de SleepSession para cálculo de métricas de sono
 */

/**
 * Calcula a duração média do sono a partir de uma lista de sessões
 */
fun List<SleepSession>.calculateAverageDuration(): Duration {
    if (this.isEmpty()) return Duration.ZERO
    
    val totalDuration = this.fold(Duration.ZERO) { acc, session ->
        acc.plus(session.duration)
    }
    
    return totalDuration.dividedBy(this.size.toLong())
}

/**
 * Calcula a qualidade média do sono (0-10) com base nas eficiências das sessões
 */
fun List<SleepSession>.calculateSleepQuality(): Double {
    if (this.isEmpty()) return 0.0
    
    val validSessions = this.filter { it.efficiency != null && it.efficiency > 0 }
    if (validSessions.isEmpty()) return 0.0
    
    val totalQuality = validSessions.sumOf { it.efficiency ?: 0.0 }
    return (totalQuality / validSessions.size).coerceIn(0.0, 10.0)
}

/**
 * Filtra sessões de sono noturno (excluindo sonecas)
 */
fun List<SleepSession>.filterNightSleep(): List<SleepSession> {
    return this.filter { session ->
        // Considera como sono noturno se a duração for maior que 3 horas
        // e começar entre 20h-23h59 ou terminar entre 00h-10h
        val durationHours = session.duration.seconds / 3600.0
        val startHour = session.startTimeZoned.hour
        
        durationHours >= 3 && (startHour in 20..23 || startHour in 0..9)
    }
}

/**
 * Agrupa sessões por dia da semana
 * @return Mapa onde a chave é o dia da semana (1=Segunda, 7=Domingo)
 */
fun List<SleepSession>.groupByDayOfWeek(): Map<Int, List<SleepSession>> {
    return this.groupBy { session ->
        session.startTimeZoned.dayOfWeek.value
    }
}

/**
 * Calcula o horário médio de dormir
 */
fun List<SleepSession>.calculateAverageBedtime(): LocalTime? {
    if (this.isEmpty()) return null
    
    val totalSeconds = this.sumOf { session ->
        val localTime = LocalTime.from(session.startTimeZoned)
        localTime.toSecondOfDay().toLong()
    }
    
    val averageSeconds = (totalSeconds.toDouble() / this.size).roundToInt()
    return LocalTime.ofSecondOfDay(averageSeconds.toLong())
}

/**
 * Calcula o horário médio de acordar
 */
fun List<SleepSession>.calculateAverageWakeTime(): LocalTime? {
    if (this.isEmpty()) return null
    
    val totalSeconds = this.sumOf { session ->
        val localTime = LocalTime.from(session.endTimeZoned)
        localTime.toSecondOfDay().toLong()
    }
    
    val averageSeconds = (totalSeconds.toDouble() / this.size).roundToInt()
    return LocalTime.ofSecondOfDay(averageSeconds.toLong())
}

/**
 * Calcula a consistência dos horários de sono (0-100)
 * Quanto mais próximos os horários de dormir/acordar, maior a pontuação
 */
fun List<SleepSession>.calculateConsistencyScore(): Int {
    if (this.size < 3) return 0
    
    val bedtimes = this.map { LocalTime.from(it.startTimeZoned) }
    val wakeTimes = this.map { LocalTime.from(it.endTimeZoned) }
    
    val bedtimeConsistency = calculateTimeConsistency(bedtimes)
    val wakeTimeConsistency = calculateTimeConsistency(wakeTimes)
    
    return ((bedtimeConsistency + wakeTimeConsistency) / 2).coerceIn(0, 100)
}

/**
 * Função auxiliar para calcular a consistência de horários
 */
private fun calculateTimeConsistency(times: List<LocalTime>): Int {
    if (times.size < 2) return 0
    
    val avgSeconds = times.map { it.toSecondOfDay() }.average().toInt()
    
    val totalDeviation = times.sumOf { time: LocalTime ->
        val diff = Math.abs(time.toSecondOfDay() - avgSeconds)
        // Considera que 2 horas é o desvio máximo aceitável
        (1 - (diff / 7200.0).coerceAtMost(1.0)) * 100
    }
    
    return (totalDeviation / times.size).toInt()
}


