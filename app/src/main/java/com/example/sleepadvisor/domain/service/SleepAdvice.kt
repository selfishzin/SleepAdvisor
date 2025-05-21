package com.example.sleepadvisor.domain.service

/**
 * Modelo de domínio que representa um conselho de sono gerado pelo serviço de IA
 * Contém recomendações personalizadas baseadas na análise de múltiplas sessões de sono
 */
data class SleepAdvice(
    val mainAdvice: String,
    val supportingFacts: List<String> = emptyList(),
    val customRecommendations: List<String> = emptyList(),
    val qualityTrend: String? = null,
    val consistencyScore: Int? = null,
    val idealsleepSchedule: IdealSleepSchedule? = null
) {
    /**
     * Retorna o conselho principal como uma string formatada para exibição
     */
    fun getFormattedAdvice(): String {
        return mainAdvice
    }
}

/**
 * Representa uma programação ideal de sono baseada nas análises do usuário
 */
data class IdealSleepSchedule(
    val suggestedBedtime: String,
    val suggestedWakeTime: String,
    val idealSleepDuration: String
)
