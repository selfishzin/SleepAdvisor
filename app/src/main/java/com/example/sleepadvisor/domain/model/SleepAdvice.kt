package com.example.sleepadvisor.domain.model

/**
 * Modelo que representa conselhos e recomendau00e7u00f5es personalizadas de sono
 * geradas pelo serviu00e7o de IA.
 */
data class SleepAdvice(
    val mainAdvice: String,
    val supportingFacts: List<String> = emptyList(),
    val customRecommendations: List<String> = emptyList(),
    val qualityTrend: String? = null,
    val consistencyScore: Int? = null,
    val idealsleepSchedule: IdealSleepSchedule? = null
)
