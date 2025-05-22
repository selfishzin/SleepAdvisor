package com.example.sleepadvisor.domain.model

/**
 * Modelo que representa uma programação ideal de sono para o usuário
 */
data class IdealSleepSchedule(
    val suggestedBedtime: String,
    val suggestedWakeTime: String,
    val idealSleepDuration: String
)
