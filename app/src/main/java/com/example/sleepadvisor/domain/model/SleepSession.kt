package com.example.sleepadvisor.domain.model

import androidx.health.connect.client.records.HeartRateRecord
import com.example.sleepadvisor.domain.model.SleepMetrics
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

// Import the SleepSource enum
import com.example.sleepadvisor.domain.model.SleepSource

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
    val source: SleepSource 
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
        source: SleepSource,
        stages: List<SleepStage> = emptyList(),
        wakeDuringNightCount: Int = 0,
        heartRateSamples: List<HeartRateRecord.Sample>? = null,
        efficiency: Double = 0.0,
        deepSleepPercentage: Double = 0.0,
        remSleepPercentage: Double = 0.0,
        lightSleepPercentage: Double = 0.0
    ) : this(
        id = id,
        startTime = startTime,
        endTime = endTime,
        duration = Duration.between(startTime, endTime),
        title = title,
        notes = notes,
        stages = stages,
        efficiency = efficiency,
        deepSleepPercentage = deepSleepPercentage,
        remSleepPercentage = remSleepPercentage,
        lightSleepPercentage = lightSleepPercentage,
        wakeDuringNightCount = wakeDuringNightCount,
        heartRateSamples = heartRateSamples,
        source = source
    )

    /**
     * Verifica se a sessão tem dados válidos de estágios do sono
     * @return true se os estágios do sono contêm dados válidos
     */
    fun hasValidStages(): Boolean {
        return stages.isNotEmpty() && 
               (deepSleepPercentage > 0 || remSleepPercentage > 0 || lightSleepPercentage > 0)
    }

    /**
     * Calcula e atualiza as porcentagens de cada estágio de sono com base nos estágios registrados.
     * @return Nova instância de SleepSession com as porcentagens atualizadas
     */
    /**
     * Calcula e atualiza as porcentagens de cada estágio de sono com base nos estágios registrados.
     * Garante que as porcentagens estejam sempre entre 0 e 100 e que a soma não ultrapasse 100%.
     * @return Nova instância de SleepSession com as porcentagens atualizadas
     */
    fun calculateAndUpdateStagePercentages(): SleepSession {
        if (stages.isEmpty()) {
            return this.copy(
                deepSleepPercentage = 0.0,
                remSleepPercentage = 0.0,
                lightSleepPercentage = 0.0,
                efficiency = 0.0
            )
        }

        try {
            val totalSleepDuration = stages.sumOf { it.duration.toMillis() }
            
            if (totalSleepDuration <= 0L) {
                return this.copy(
                    deepSleepPercentage = 0.0,
                    remSleepPercentage = 0.0,
                    lightSleepPercentage = 0.0,
                    efficiency = 0.0
                )
            }

            // Agrupa os estágios por tipo e calcula a duração total de cada um
            val stageDurations = stages.groupBy { it.type }
                .mapValues { (_, stageList) -> 
                    stageList.sumOf { it.duration.toMillis() }.toDouble()
                }


            // Calcula as porcentagens para cada estágio
            val deepSleepMs = stageDurations[SleepStageType.DEEP] ?: 0.0
            val remSleepMs = stageDurations[SleepStageType.REM] ?: 0.0
            val lightSleepMs = stageDurations[SleepStageType.LIGHT] ?: 0.0
            
            // Calcula as porcentagens garantindo que estejam entre 0 e 100
            val deepSleepPercentage = ((deepSleepMs / totalSleepDuration) * 100.0).coerceIn(0.0, 100.0)
            val remSleepPercentage = ((remSleepMs / totalSleepDuration) * 100.0).coerceIn(0.0, 100.0)
            val lightSleepPercentage = ((lightSleepMs / totalSleepDuration) * 100.0).coerceIn(0.0, 100.0)
            
            // Calcula a eficiência do sono usando o SleepMetrics
            val calculatedEfficiency = SleepMetrics.calculateSleepEfficiency(
                stages, 
                Duration.ofMillis(totalSleepDuration)
            )
            
            // Garante que a eficiência esteja entre 0 e 100
            val safeEfficiency = calculatedEfficiency.coerceIn(0, 100).toDouble()

            return this.copy(
                deepSleepPercentage = deepSleepPercentage,
                remSleepPercentage = remSleepPercentage,
                lightSleepPercentage = lightSleepPercentage,
                efficiency = safeEfficiency
            )
        } catch (e: Exception) {
            // Em caso de erro, retorna a instância atual com valores zerados
            return this.copy(
                deepSleepPercentage = 0.0,
                remSleepPercentage = 0.0,
                lightSleepPercentage = 0.0,
                efficiency = 0.0
            )
        }
    }

    companion object {
        // O companion object agora está limpo de constantes de cálculo de pontuação.
        // Essas lógicas pertencem a serviços ou use cases.
    }
}