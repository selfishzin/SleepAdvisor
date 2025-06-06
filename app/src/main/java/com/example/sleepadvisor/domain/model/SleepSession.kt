package com.example.sleepadvisor.domain.model

import androidx.health.connect.client.records.HeartRateRecord
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Modelo de domínio que representa uma sessão de sono
 * Pode ser originada do Health Connect, de uma entrada manual, ou ter estágios estimados.
 */
data class SleepSession(
    val id: String,
    val startTime: Instant, 
    val endTime: Instant,   
    val duration: Duration = Duration.between(startTime, endTime),
    val title: String? = null, 
    val notes: String? = null,
    val stages: List<SleepStage> = emptyList(),
    val efficiency: Double = 0.0, 
    val deepSleepPercentage: Double = 0.0, 
    val remSleepPercentage: Double = 0.0, 
    val lightSleepPercentage: Double = 0.0, 
    val wakeDuringNightCount: Int = 0,
    val heartRateSamples: List<HeartRateRecord.Sample>? = null,
    val source: String 
) {

    val startTimeZoned: ZonedDateTime
        get() = startTime.atZone(ZoneId.systemDefault())

    val endTimeZoned: ZonedDateTime
        get() = endTime.atZone(ZoneId.systemDefault())

    // Construtor secundário para facilitar a criação, se necessário
    constructor(
        id: String,
        startTime: Instant,
        endTime: Instant,
        notes: String? = null,
        title: String? = null,
        source: String,
        stages: List<SleepStage> = emptyList(),
        wakeDuringNightCount: Int = 0,
        heartRateSamples: List<HeartRateRecord.Sample>? = null
        // efficiency e porcentagens são tipicamente calculados, não passados no construtor básico
    ) : this(
        id = id,
        startTime = startTime,
        endTime = endTime,
        duration = Duration.between(startTime, endTime),
        title = title,
        notes = notes,
        stages = stages,
        efficiency = 0.0, 
        deepSleepPercentage = 0.0, 
        remSleepPercentage = 0.0,  
        lightSleepPercentage = 0.0, 
        wakeDuringNightCount = wakeDuringNightCount,
        heartRateSamples = heartRateSamples,
        source = source
    )

    companion object {
        // O companion object agora está limpo de constantes de cálculo de pontuação.
        // Essas lógicas pertencem a serviços ou use cases.
    }
}