package com.example.sleepadvisor.domain.model

import java.time.Duration

/**
 * Modelo de domínio que representa uma análise detalhada das tendências semanais de sono
 * Contém informações sobre consistência, duração média, horários médios e recomendações
 *
 * @property overallTrend Tendência geral do sono (melhorando, piorando, estável)
 * @property consistencyScore Pontuação de consistência (0-100), opcional
 * @property consistencyLevel Nível de consistência (Excelente, Muito Boa, Boa, etc.), opcional
 * @property averageSleepDuration Duração média de sono, opcional
 * @property averageBedtime Horário médio de dormir, opcional
 * @property averageWakeTime Horário médio de acordar, opcional
 * @property durationTrend Tendência de duração (aumentando, diminuindo, estável), opcional
 * @property qualityTrend Tendência de qualidade (melhorando, piorando, estável), opcional
 * @property weekdayVsWeekendAnalysis Análise comparativa entre dias de semana e fim de semana, opcional
 * @property recommendations Lista de recomendações personalizadas, padrão vazia
 */
data class SleepTrendAnalysis(
    val overallTrend: String = "estável",
    val consistencyScore: Int? = null,
    val consistencyLevel: String? = null,
    val averageSleepDuration: Duration? = null,
    val averageBedtime: String? = null,
    val averageWakeTime: String? = null,
    val durationTrend: String? = null,
    val qualityTrend: String? = null,
    val weekdayVsWeekendAnalysis: String? = null,
    val recommendations: List<String> = emptyList()
) {
    companion object {
        /**
         * Cria uma instância de SleepTrendAnalysis com valores padrão para indicar dados insuficientes
         */
        fun createDefault(): SleepTrendAnalysis {
            return SleepTrendAnalysis(
                overallTrend = "estável",
                consistencyScore = null,
                consistencyLevel = null,
                averageSleepDuration = null,
                averageBedtime = null,
                averageWakeTime = null,
                durationTrend = null,
                qualityTrend = null,
                weekdayVsWeekendAnalysis = "Dados insuficientes para análise",
                recommendations = listOf("Registre mais noites de sono para obter análises detalhadas")
            )
        }
    }
}
