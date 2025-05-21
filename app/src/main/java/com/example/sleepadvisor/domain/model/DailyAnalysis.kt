package com.example.sleepadvisor.domain.model

import com.example.sleepadvisor.domain.model.SleepStage

/**
 * Modelo de domínio que representa uma análise diária de sono
 */
data class DailyAnalysis(
    val date: String,
    val analysis: String,
    val recommendations: List<String> = emptyList(),
    val sleepScore: Int,
    val sleepQuality: String,
    val sleepStages: List<SleepStage>? = null,
    val scientificFact: String? = null
) {
    /**
     * Obtém a principal recomendação (primeira da lista)
     */
    val primaryRecommendation: String?
        get() = recommendations.firstOrNull()

    /**
     * Obtém a recomendação secundária (segunda da lista)
     */
    val secondaryRecommendation: String?
        get() = if (recommendations.size > 1) recommendations[1] else null

    /**
     * Retorna a descrição textual da qualidade do sono
     */
    val qualityDescription: String
        get() = sleepQuality

    /**
     * Retorna a pontuação numérica da qualidade do sono
     */
    val qualityScore: Int
        get() = sleepScore
}
