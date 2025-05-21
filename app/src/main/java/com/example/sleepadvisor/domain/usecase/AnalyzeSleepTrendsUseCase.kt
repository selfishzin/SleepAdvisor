package com.example.sleepadvisor.domain.usecase

import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepTrendAnalysis
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Caso de uso responsável por analisar tendências semanais nos padrões de sono.
 * Identifica padrões, inconsistências e fornece recomendações para melhorar a regularidade.
 */
class AnalyzeSleepTrendsUseCase @Inject constructor() {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Analisa tendências semanais de sono com base em uma lista de sessões.
     * 
     * @param sessions Lista de sessões de sono da semana
     * @return Análise detalhada das tendências semanais
     */
    operator fun invoke(sessions: List<SleepSession>): SleepTrendAnalysis {
        if (sessions.isEmpty()) {
            return SleepTrendAnalysis(
                overallTrend = "Dados insuficientes para análise de tendências.",
                consistencyScore = 0,
                consistencyLevel = "Indeterminado",
                averageSleepDuration = Duration.ZERO,
                averageBedtime = "",
                averageWakeTime = "",
                durationTrend = "Dados insuficientes.",
                qualityTrend = "Dados insuficientes.",
                weekdayVsWeekendAnalysis = "Dados insuficientes.",
                recommendations = listOf("Registre seu sono por pelo menos 5 dias para obter uma análise de tendências.")
            )
        }

        // Ordenar sessões por data
        val sortedSessions = sessions.sortedBy { it.startTime }
        
        // Calcular duração média de sono
        val averageDuration = calculateAverageDuration(sortedSessions)
        
        // Calcular horários médios de dormir e acordar
        val (averageBedtime, averageWakeTime) = calculateAverageTimes(sortedSessions)
        
        // Analisar consistência dos horários
        val consistencyScore = calculateConsistencyScore(sortedSessions)
        val consistencyLevel = getConsistencyLevel(consistencyScore)
        
        // Analisar tendência de duração
        val durationTrend = analyzeDurationTrend(sortedSessions)
        
        // Analisar tendência de qualidade
        val qualityTrend = analyzeQualityTrend(sortedSessions)
        
        // Analisar diferenças entre dias de semana e fim de semana
        val weekdayVsWeekend = analyzeWeekdayVsWeekend(sortedSessions)
        
        // Determinar tendência geral
        val overallTrend = determineOverallTrend(
            durationTrend,
            qualityTrend,
            consistencyScore
        )
        
        // Gerar recomendações personalizadas
        val recommendations = generateRecommendations(
            sortedSessions,
            consistencyScore,
            durationTrend,
            qualityTrend,
            weekdayVsWeekend
        )
        
        return SleepTrendAnalysis(
            overallTrend = overallTrend,
            consistencyScore = consistencyScore,
            consistencyLevel = consistencyLevel,
            averageSleepDuration = averageDuration,
            averageBedtime = averageBedtime,
            averageWakeTime = averageWakeTime,
            durationTrend = durationTrend,
            qualityTrend = qualityTrend,
            weekdayVsWeekendAnalysis = weekdayVsWeekend,
            recommendations = recommendations
        )
    }
    
    /**
     * Calcula a duração média do sono.
     */
    private fun calculateAverageDuration(sessions: List<SleepSession>): Duration {
        if (sessions.isEmpty()) return Duration.ZERO
        
        val totalMinutes = sessions.sumOf { it.duration.toMinutes() }
        return Duration.ofMinutes(totalMinutes / sessions.size)
    }
    
    /**
     * Calcula os horários médios de dormir e acordar.
     * Retorna um par com (horário médio de dormir, horário médio de acordar)
     */
    private fun calculateAverageTimes(sessions: List<SleepSession>): Pair<String, String> {
        if (sessions.isEmpty()) return Pair("", "")
        
        // Calcular média de horário de dormir
        val avgBedtimeMinutes = sessions
            .map { it.startTime.atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay() / 60 }
            .average()
            .toInt()
        
        // Calcular média de horário de acordar
        val avgWakeTimeMinutes = sessions
            .map { it.endTime.atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay() / 60 }
            .average()
            .toInt()
        
        // Converter minutos para LocalTime
        val avgBedtime = LocalTime.of(avgBedtimeMinutes / 60, avgBedtimeMinutes % 60)
        val avgWakeTime = LocalTime.of(avgWakeTimeMinutes / 60, avgWakeTimeMinutes % 60)
        
        return Pair(
            avgBedtime.format(timeFormatter),
            avgWakeTime.format(timeFormatter)
        )
    }
    
    /**
     * Calcula a pontuação de consistência dos horários de sono (0-100).
     * Leva em consideração a variação nos horários de dormir e acordar.
     */
    private fun calculateConsistencyScore(sessions: List<SleepSession>): Int {
        if (sessions.size < 3) return 0
        
        // Calcular desvio padrão dos horários de dormir
        val bedtimeMinutes = sessions.map { it.startTime.atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay() / 60.0 }
        val bedtimeStdDev = calculateStandardDeviation(bedtimeMinutes)
        
        // Calcular desvio padrão dos horários de acordar
        val wakeTimeMinutes = sessions.map { it.endTime.atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay() / 60.0 }
        val wakeTimeStdDev = calculateStandardDeviation(wakeTimeMinutes)
        
        // Calcular pontuação (menor desvio = maior consistência)
        // Desvio de 15 minutos ou menos é excelente (90-100)
        // Desvio de 60 minutos ou mais é ruim (0-40)
        val bedtimeScore = 100 - (bedtimeStdDev * 1.5).coerceIn(0.0, 60.0)
        val wakeTimeScore = 100 - (wakeTimeStdDev * 1.5).coerceIn(0.0, 60.0)
        
        // Média ponderada (horário de dormir tem peso maior)
        return ((bedtimeScore * 0.6) + (wakeTimeScore * 0.4)).toInt().coerceIn(0, 100)
    }
    
    /**
     * Calcula o desvio padrão de uma lista de valores.
     */
    private fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.size <= 1) return 0.0
        
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return kotlin.math.sqrt(variance)
    }
    
    /**
     * Determina o nível de consistência com base na pontuação.
     */
    private fun getConsistencyLevel(score: Int): String {
        return when {
            score >= 90 -> "Excelente"
            score >= 75 -> "Muito Boa"
            score >= 60 -> "Boa"
            score >= 45 -> "Regular"
            score >= 30 -> "Baixa"
            else -> "Muito Baixa"
        }
    }
    
    /**
     * Analisa a tendência de duração do sono ao longo da semana.
     */
    private fun analyzeDurationTrend(sessions: List<SleepSession>): String {
        if (sessions.size < 4) return "Dados insuficientes para determinar tendência de duração."
        
        // Dividir a semana em duas partes para comparar
        val firstHalf = sessions.take(sessions.size / 2)
        val secondHalf = sessions.drop(sessions.size / 2)
        
        // Calcular duração média em cada metade
        val firstHalfAvg = firstHalf.map { it.duration.toMinutes() }.average()
        val secondHalfAvg = secondHalf.map { it.duration.toMinutes() }.average()
        
        // Calcular diferença percentual
        val percentChange = ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100
        
        return when {
            percentChange > 10 -> "Sua duração de sono está aumentando significativamente."
            percentChange > 5 -> "Sua duração de sono está aumentando moderadamente."
            percentChange < -10 -> "Sua duração de sono está diminuindo significativamente."
            percentChange < -5 -> "Sua duração de sono está diminuindo moderadamente."
            else -> "Sua duração de sono está estável."
        }
    }
    
    /**
     * Analisa a tendência de qualidade do sono ao longo da semana.
     */
    private fun analyzeQualityTrend(sessions: List<SleepSession>): String {
        if (sessions.size < 4) return "Dados insuficientes para determinar tendência de qualidade."
        
        // Dividir a semana em duas partes para comparar
        val firstHalf = sessions.take(sessions.size / 2)
        val secondHalf = sessions.drop(sessions.size / 2)
        
        // Calcular eficiência média em cada metade
        val firstHalfAvg = firstHalf.map { it.efficiency }.average()
        val secondHalfAvg = secondHalf.map { it.efficiency }.average()
        
        // Calcular diferença absoluta
        val difference = secondHalfAvg - firstHalfAvg
        
        return when {
            difference > 5 -> "Sua qualidade de sono está melhorando significativamente."
            difference > 2 -> "Sua qualidade de sono está melhorando moderadamente."
            difference < -5 -> "Sua qualidade de sono está piorando significativamente."
            difference < -2 -> "Sua qualidade de sono está piorando moderadamente."
            else -> "Sua qualidade de sono está estável."
        }
    }
    
    /**
     * Analisa diferenças entre dias de semana e fim de semana.
     */
    private fun analyzeWeekdayVsWeekend(sessions: List<SleepSession>): String {
        // Separar sessões de dias de semana e fim de semana
        val weekdaySessions = sessions.filter { 
            val dayOfWeek = it.startTime.atZone(ZoneId.systemDefault()).dayOfWeek
            dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY
        }
        
        val weekendSessions = sessions.filter { 
            val dayOfWeek = it.startTime.atZone(ZoneId.systemDefault()).dayOfWeek
            dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
        }
        
        if (weekdaySessions.isEmpty() || weekendSessions.isEmpty()) {
            return "Dados insuficientes para comparar dias de semana e fim de semana."
        }
        
        // Calcular duração média
        val weekdayDuration = weekdaySessions.map { it.duration.toMinutes() }.average()
        val weekendDuration = weekendSessions.map { it.duration.toMinutes() }.average()
        
        // Calcular horários médios
        val weekdayBedtime = weekdaySessions.map { it.startTime.atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay() / 60 }.average().toInt()
        val weekendBedtime = weekendSessions.map { it.startTime.atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay() / 60 }.average().toInt()
        
        val weekdayWakeTime = weekdaySessions.map { it.endTime.atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay() / 60 }.average().toInt()
        val weekendWakeTime = weekendSessions.map { it.endTime.atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay() / 60 }.average().toInt()
        
        // Calcular diferenças
        val durationDiff = weekendDuration - weekdayDuration
        val bedtimeDiff = weekendBedtime - weekdayBedtime
        val waketimeDiff = weekendWakeTime - weekdayWakeTime
        
        val analysis = StringBuilder()
        
        // Analisar diferença de duração
        if (kotlin.math.abs(durationDiff) > 60) {
            if (durationDiff > 0) {
                analysis.append("Você dorme significativamente mais nos fins de semana (${(durationDiff / 60).toInt()}h${(durationDiff % 60).toInt()}min a mais). ")
            } else {
                analysis.append("Você dorme significativamente menos nos fins de semana (${(-durationDiff / 60).toInt()}h${(-durationDiff % 60).toInt()}min a menos). ")
            }
        } else if (kotlin.math.abs(durationDiff) > 30) {
            if (durationDiff > 0) {
                analysis.append("Você dorme um pouco mais nos fins de semana (${(durationDiff % 60).toInt()}min a mais). ")
            } else {
                analysis.append("Você dorme um pouco menos nos fins de semana (${(-durationDiff % 60).toInt()}min a menos). ")
            }
        } else {
            analysis.append("Sua duração de sono é consistente entre dias de semana e fins de semana. ")
        }
        
        // Analisar diferença de horário de dormir
        if (kotlin.math.abs(bedtimeDiff.toDouble()) > 60) {
            if (bedtimeDiff > 0) {
                analysis.append("Você vai dormir significativamente mais tarde nos fins de semana (${(bedtimeDiff / 60).toInt()}h${(bedtimeDiff % 60).toInt()}min). ")
            } else {
                analysis.append("Você vai dormir significativamente mais cedo nos fins de semana (${(-bedtimeDiff / 60).toInt()}h${(-bedtimeDiff % 60).toInt()}min). ")
            }
        } else if (kotlin.math.abs(bedtimeDiff.toDouble()) > 30) {
            if (bedtimeDiff > 0) {
                analysis.append("Você vai dormir um pouco mais tarde nos fins de semana (${(bedtimeDiff % 60).toInt()}min). ")
            } else {
                analysis.append("Você vai dormir um pouco mais cedo nos fins de semana (${(-bedtimeDiff % 60).toInt()}min). ")
            }
        }
        
        // Analisar diferença de horário de acordar
        if (kotlin.math.abs(waketimeDiff.toDouble()) > 60) {
            if (waketimeDiff > 0) {
                analysis.append("Você acorda significativamente mais tarde nos fins de semana (${(waketimeDiff / 60).toInt()}h${(waketimeDiff % 60).toInt()}min).")
            } else {
                analysis.append("Você acorda significativamente mais cedo nos fins de semana (${(-waketimeDiff / 60).toInt()}h${(-waketimeDiff % 60).toInt()}min).")
            }
        } else if (kotlin.math.abs(waketimeDiff.toDouble()) > 30) {
            if (waketimeDiff > 0) {
                analysis.append("Você acorda um pouco mais tarde nos fins de semana (${(waketimeDiff % 60).toInt()}min).")
            } else {
                analysis.append("Você acorda um pouco mais cedo nos fins de semana (${(-waketimeDiff % 60).toInt()}min).")
            }
        }
        
        // Se não houver diferenças significativas
        if (analysis.isEmpty()) {
            return "Seus padrões de sono são muito consistentes entre dias de semana e fins de semana, o que é excelente para seu ritmo circadiano."
        }
        
        // Adicionar comentário sobre jet lag social se aplicável
        if (waketimeDiff > 90 || bedtimeDiff > 90) {
            analysis.append(" Esta inconsistência pode causar 'jet lag social', dificultando a adaptação na segunda-feira.")
        }
        
        return analysis.toString()
    }
    
    /**
     * Determina a tendência geral do sono com base em múltiplos fatores.
     */
    private fun determineOverallTrend(
        durationTrend: String,
        qualityTrend: String,
        consistencyScore: Int
    ): String {
        // Verificar se há dados suficientes
        if (durationTrend.contains("insuficientes") || qualityTrend.contains("insuficientes")) {
            return "Dados insuficientes para determinar tendência geral."
        }
        
        // Analisar tendências de duração e qualidade
        val isDurationImproving = durationTrend.contains("aumentando") || durationTrend.contains("estável")
        val isQualityImproving = qualityTrend.contains("melhorando") || qualityTrend.contains("estável")
        
        // Determinar tendência geral
        return when {
            isDurationImproving && isQualityImproving && consistencyScore >= 75 ->
                "Seus padrões de sono estão excelentes, com boa duração, qualidade e consistência."
                
            isDurationImproving && isQualityImproving ->
                "Seus padrões de sono estão melhorando, com tendências positivas em duração e qualidade."
                
            !isDurationImproving && !isQualityImproving ->
                "Seus padrões de sono estão piorando, com tendências negativas em duração e qualidade."
                
            isDurationImproving && !isQualityImproving ->
                "Sua duração de sono está melhorando, mas a qualidade está piorando."
                
            !isDurationImproving && isQualityImproving ->
                "Sua qualidade de sono está melhorando, mas a duração está diminuindo."
                
            else ->
                "Seus padrões de sono mostram resultados mistos, com algumas melhorias e alguns desafios."
        }
    }
    
    /**
     * Gera recomendações personalizadas com base na análise de tendências.
     */
    private fun generateRecommendations(
        sessions: List<SleepSession>,
        consistencyScore: Int,
        durationTrend: String,
        qualityTrend: String,
        weekdayVsWeekend: String
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Recomendações baseadas na consistência
        when {
            consistencyScore < 50 ->
                recommendations.add("Tente manter horários mais regulares para dormir e acordar, mesmo nos fins de semana. A consistência é fundamental para um sono de qualidade.")
                
            consistencyScore in 50..70 ->
                recommendations.add("Sua consistência de horários está razoável, mas pode melhorar. Tente não variar mais que 30 minutos nos horários de dormir e acordar.")
        }
        
        // Recomendações baseadas na tendência de duração
        if (durationTrend.contains("diminuindo")) {
            recommendations.add("Sua duração de sono está diminuindo. Tente priorizar o sono e garantir pelo menos 7 horas por noite para adultos.")
        }
        
        // Recomendações baseadas na tendência de qualidade
        if (qualityTrend.contains("piorando")) {
            recommendations.add("Sua qualidade de sono está piorando. Considere melhorar sua higiene do sono: ambiente escuro e fresco, evitar cafeína e álcool, e reduzir o uso de telas antes de dormir.")
        }
        
        // Recomendações baseadas na diferença entre dias de semana e fim de semana
        if (weekdayVsWeekend.contains("jet lag social")) {
            recommendations.add("Você está experimentando 'jet lag social' devido à grande diferença entre seus horários de sono nos dias de semana e fins de semana. Tente reduzir essa diferença para melhorar seu ritmo circadiano.")
        }
        
        // Recomendações gerais se a lista estiver vazia
        if (recommendations.isEmpty()) {
            if (consistencyScore >= 80 && !durationTrend.contains("diminuindo") && !qualityTrend.contains("piorando")) {
                recommendations.add("Continue mantendo seus excelentes padrões de sono. A consistência que você demonstra é ideal para saúde e bem-estar.")
            } else {
                recommendations.add("Para melhorar ainda mais seu sono, mantenha horários regulares, crie um ambiente propício para o sono, e pratique uma rotina relaxante antes de dormir.")
            }
        }
        
        return recommendations
    }
}
