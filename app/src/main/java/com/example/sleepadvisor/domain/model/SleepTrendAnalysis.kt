package com.example.sleepadvisor.domain.model

import java.time.Duration

/**
 * Modelo de domínio que representa uma análise detalhada das tendências semanais de sono
 * Contém informações sobre consistência, duração média, horários médios e recomendações
 */
data class SleepTrendAnalysis(
    val overallTrend: String,                // Tendência geral do sono (melhorando, piorando, estável)
    val consistencyScore: Int,               // Pontuação de consistência (0-100)
    val consistencyLevel: String,            // Nível de consistência (Excelente, Muito Boa, Boa, etc.)
    val averageSleepDuration: Duration,      // Duração média de sono
    val averageBedtime: String,              // Horário médio de dormir
    val averageWakeTime: String,             // Horário médio de acordar
    val durationTrend: String,               // Tendência de duração (aumentando, diminuindo, estável)
    val qualityTrend: String,                // Tendência de qualidade (melhorando, piorando, estável)
    val weekdayVsWeekendAnalysis: String,    // Análise comparativa entre dias de semana e fim de semana
    val recommendations: List<String>         // Recomendações personalizadas
)
