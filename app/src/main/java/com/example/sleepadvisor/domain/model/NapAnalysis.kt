package com.example.sleepadvisor.domain.model

/**
 * Modelo de domínio que representa uma análise detalhada de uma soneca (nap)
 * Contém a sessão de sono, qualidade, horário ideal e recomendações
 */
data class NapAnalysis(
    val session: SleepSession,              // A sessão de sono classificada como soneca
    val quality: String,                     // Classificação da qualidade (Excelente, Boa, Regular, Ruim)
    val idealTime: String,                   // Horário ideal recomendado para sonecas
    val recommendations: List<String>,       // Recomendações personalizadas
    val impactOnNightSleep: String           // Análise do impacto potencial no sono noturno
)
