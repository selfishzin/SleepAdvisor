package com.example.sleepadvisor.domain.model

/**
 * Modelo de domínio que representa recomendações personalizadas de sono
 * Combina recomendações prioritárias, gerais, reforços positivos e fatos científicos
 */
data class SleepRecommendations(
    val priorityRecommendations: List<String>,    // Recomendações de alta prioridade (máximo 3)
    val generalRecommendations: List<String>,     // Recomendações gerais (máximo 5)
    val positiveReinforcements: List<String>,     // Reforços positivos (máximo 2)
    val scientificFact: String,                   // Fato científico personalizado
    val idealBedtime: String,                     // Horário ideal para dormir
    val idealWakeTime: String,                    // Horário ideal para acordar
    val idealNapTime: String                      // Horário ideal para sonecas
)
