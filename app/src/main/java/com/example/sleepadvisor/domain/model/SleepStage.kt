package com.example.sleepadvisor.domain.model

import java.time.Duration
import java.time.Instant

// Import the SleepSource enum
import com.example.sleepadvisor.domain.model.SleepSource

/**
 * Representa um estágio individual dentro de uma sessão de sono.
 */
data class SleepStage(
    val startTime: Instant, 
    val endTime: Instant,   
    val type: SleepStageType,
    val source: SleepSource      
) {
    val duration: Duration = Duration.between(startTime, endTime)
}

/**
 * Tipos de estágio de sono possíveis.
 */
enum class SleepStageType(val displayName: String) {
    AWAKE("Acordado"),
    LIGHT("Sono Leve"),
    DEEP("Sono Profundo"),
    REM("REM"),
    UNKNOWN("Desconhecido"),
    SLEEPING("Dormindo"),
    OUT_OF_BED("Fora da Cama")
}