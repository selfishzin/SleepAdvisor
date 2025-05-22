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
enum class SleepStageType {
    AWAKE,
    LIGHT,
    DEEP,
    REM,
    UNKNOWN, 
    SLEEPING,
    OUT_OF_BED
}