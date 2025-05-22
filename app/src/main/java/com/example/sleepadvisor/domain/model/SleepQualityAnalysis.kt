package com.example.sleepadvisor.domain.model

/**
 * Modelo de domínio que representa uma análise detalhada da qualidade do sono
 * Contém pontuação, classificação verbal e análises específicas de diferentes aspectos do sono
 */
data class SleepQualityAnalysis(
    val score: Int,                          // Pontuação de 0-100
    val qualityLabel: String,                // Classificação verbal (Excelente, Muito Boa, Boa, etc.)
    val stageAnalysis: String,               // Análise dos estágios do sono (REM, Profundo, Leve)
    val durationAnalysis: String,            // Análise da duração total do sono
    val continuityAnalysis: String,          // Análise da continuidade do sono (despertares)
    val recommendations: List<String>,       // Recomendações personalizadas
    val scientificFact: String,              // Fato científico personalizado
    val metrics: Map<String, Any> = emptyMap() // Métricas detalhadas da análise
) {
    /**
     * Cria uma cópia desta análise com métricas adicionais
     */
    fun copyWithMetrics(additionalMetrics: Map<String, Any>): SleepQualityAnalysis {
        return copy(metrics = this.metrics + additionalMetrics)
    }
}
