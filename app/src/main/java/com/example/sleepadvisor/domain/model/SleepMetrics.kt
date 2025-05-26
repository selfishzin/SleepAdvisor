package com.example.sleepadvisor.domain.model

import java.time.Duration
import kotlin.math.absoluteValue

/**
 * Objeto utilitário para cálculos de métricas de sono padronizadas.
 * Garante consistência nos cálculos em todo o aplicativo.
 */
object SleepMetrics {

    /**
     * Calcula a eficiência do sono com base nos estágios e duração total.
     * @param stages Lista de estágios de sono
     * @param duration Duração total da sessão de sono
     * @return Eficiência do sono em porcentagem (0-100)
     */
    /**
     * Calcula a eficiência do sono com base nos estágios e duração total.
     * A eficiência é calculada como a porcentagem de tempo dormindo em relação ao tempo total na cama.
     * @param stages Lista de estágios de sono
     * @param duration Duração total da sessão de sono
     * @return Eficiência do sono em porcentagem (0-100)
     */
    fun calculateSleepEfficiency(stages: List<SleepStage>, duration: Duration): Int {
        if (duration.isZero || duration.isNegative) return 0
        
        try {
            val totalSleepTime = stages
                .filter { it.type != SleepStageType.AWAKE }
                .sumOf { it.duration.seconds }
            
            // Garante que não há divisão por zero e que o valor está dentro dos limites
            if (duration.seconds == 0L) return 0
            
            val efficiency = (totalSleepTime.toDouble() / duration.seconds) * 100.0
            return when {
                efficiency.isNaN() || efficiency.isInfinite() -> 0
                efficiency < 0 -> 0
                efficiency > 100 -> 100
                else -> efficiency.toInt()
            }
        } catch (e: Exception) {
            // Em caso de erro, retorna um valor padrão
            return 0
        }
    }

    /**
     * Calcula a porcentagem de um estágio específico de sono.
     * @param stages Lista de estágios de sono
     * @param targetStage Tipo de estágio para calcular a porcentagem
     * @return Porcentagem do estágio (0-100)
     */
    fun calculateStagePercentage(stages: List<SleepStage>, targetStage: SleepStageType): Double {
        return calculateAllStagePercentages(stages)[targetStage] ?: 0.0
    }
    
    /**
     * Calcula as porcentagens de todos os estágios de sono de uma vez.
     * @param stages Lista de estágios de sono
     * @return Mapa com as porcentagens de cada estágio de sono (0-100)
     */
    fun calculateAllStagePercentages(stages: List<SleepStage>): Map<SleepStageType, Double> {
        if (stages.isEmpty()) {
            return mapOf(
                SleepStageType.LIGHT to DEFAULT_LIGHT_SLEEP_PERCENTAGE,
                SleepStageType.DEEP to DEFAULT_DEEP_SLEEP_PERCENTAGE,
                SleepStageType.REM to DEFAULT_REM_SLEEP_PERCENTAGE,
                SleepStageType.AWAKE to 0.0,
                SleepStageType.UNKNOWN to 0.0
            )
        }
        
        val totalDuration = stages.sumOf { it.duration.toMillis() }.toDouble()
        if (totalDuration == 0.0) {
            return mapOf(
                SleepStageType.LIGHT to 0.0,
                SleepStageType.DEEP to 0.0,
                SleepStageType.REM to 0.0,
                SleepStageType.AWAKE to 0.0,
                SleepStageType.UNKNOWN to 0.0
            )
        }
        
        // Agrupa os estágios por tipo e calcula a duração total de cada um
        val stageDurations = stages.groupBy { it.type }
            .mapValues { (_, stages) ->
                stages.sumOf { it.duration.toMillis() }.toDouble()
            }
        
        // Calcula as porcentagens para cada tipo de estágio
        return stageDurations.mapValues { (_, duration) ->
            (duration / totalDuration) * 100.0
        }
    }

    /**
     * Classifica a qualidade do sono com base na eficiência.
     * @param efficiency Eficiência do sono (0-100)
     * @return Classificação da qualidade
     */
    fun getSleepQualityLabel(efficiency: Int): String {
        return when {
            efficiency >= 90 -> "Excelente"
            efficiency >= 85 -> "Muito Boa"
            efficiency >= 80 -> "Boa"
            efficiency >= 70 -> "Regular"
            efficiency >= 60 -> "Razoável"
            else -> "Ruim"
        }
    }

    /**
     * Calcula a latência do sono (tempo para adormecer).
     * @param stages Lista de estágios de sono
     * @param sessionStartTime Horário de início da sessão de sono
     * @return Duração da latência ou null se não for possível determinar
     */
    @Suppress("UNUSED_PARAMETER")
    fun calculateSleepLatency(stages: List<SleepStage>, sessionStartTime: java.time.Instant? = null): Duration? {
        if (stages.isEmpty()) return null
        
        val firstStage = stages.first()
        val startTime = sessionStartTime ?: firstStage.startTime
        
        val firstNonAwakeStage = stages.firstOrNull { it.type != SleepStageType.AWAKE } ?: return null
        return Duration.between(startTime, firstNonAwakeStage.startTime)
    }
    
    /**
     * Calcula uma pontuação de qualidade do sono baseada em métricas de sono.
     * @param efficiency Eficiência do sono (0-100)
     * @param deepSleepPercentage Porcentagem de sono profundo (0-100)
     * @param remSleepPercentage Porcentagem de sono REM (0-100)
     * @param wakeCount Número de vezes que o usuário acordou durante a noite
     * @param totalSleepDuration Duração total do sono em minutos
     * @return Pontuação de qualidade do sono (0-100)
     */
    fun calculateSleepQualityScore(
        efficiency: Int,
        deepSleepPercentage: Double,
        remSleepPercentage: Double,
        wakeCount: Int,
        totalSleepDuration: Long
    ): Int {
        // Ponderadores para cada fator
        val efficiencyWeight = 0.4
        val deepSleepWeight = 0.25
        val remSleepWeight = 0.2
        val wakeCountWeight = 0.1
        val durationWeight = 0.05
        
        // Normaliza os valores para a escala 0-1
        val normalizedEfficiency = efficiency / 100.0
        
        // Valores ideais (ajuste conforme necessário)
        val idealDeepSleep = 0.25 // 25%
        val idealRemSleep = 0.20 // 20%
        val idealDuration = 7.5 * 60 // 7.5 horas em minutos
        
        // Calcula os escores individuais
        val efficiencyScore = normalizedEfficiency
        val deepSleepScore = 1.0 - ((deepSleepPercentage / 100.0) - idealDeepSleep).absoluteValue / idealDeepSleep
        val remSleepScore = 1.0 - ((remSleepPercentage / 100.0) - idealRemSleep).absoluteValue / idealRemSleep
        val wakeScore = when (wakeCount) {
            0 -> 1.0
            1 -> 0.8
            2 -> 0.6
            3 -> 0.4
            4 -> 0.2
            else -> 0.0
        }
        val durationScore = 1.0 - ((totalSleepDuration - idealDuration).toDouble().absoluteValue / idealDuration)
        
        // Calcula a pontuação final ponderada
        val score = (efficiencyScore * efficiencyWeight) +
                   (deepSleepScore * deepSleepWeight) +
                   (remSleepScore * remSleepWeight) +
                   (wakeScore * wakeCountWeight) +
                   (durationScore * durationWeight)
        
        // Garante que a pontuação esteja no intervalo 0-100
        return (score * 100).toInt().coerceIn(0, 100)
    }
    
    /**
     * Calcula o tempo total gasto em cada estágio de sono.
     * @param stages Lista de estágios de sono
     * @return Mapa com a duração total de cada estágio de sono
     */
    fun calculateTimeByStage(stages: List<SleepStage>): Map<SleepStageType, Duration> {
        if (stages.isEmpty()) {
            return mapOf(
                SleepStageType.LIGHT to Duration.ZERO,
                SleepStageType.DEEP to Duration.ZERO,
                SleepStageType.REM to Duration.ZERO,
                SleepStageType.AWAKE to Duration.ZERO,
                SleepStageType.UNKNOWN to Duration.ZERO
            )
        }
        
        return stages.groupBy { it.type }
            .mapValues { (_, stages) ->
                stages.sumOf { it.duration.seconds }
                    .let { Duration.ofSeconds(it) }
            }
    }
    
    // Valores padrão para estágios de sono quando não há dados disponíveis
    const val DEFAULT_LIGHT_SLEEP_PERCENTAGE = 55.0
    const val DEFAULT_DEEP_SLEEP_PERCENTAGE = 25.0
    const val DEFAULT_REM_SLEEP_PERCENTAGE = 20.0
}
