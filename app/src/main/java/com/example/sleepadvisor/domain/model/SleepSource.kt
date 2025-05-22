package com.example.sleepadvisor.domain.model

/**
 * Enum que representa a origem dos dados de sono
 */
enum class SleepSource(val displayName: String) {
    MANUAL("Manual"),
    HEALTH_CONNECT("Health Connect"),
    GOOGLE_FIT("Google Fit"),
    SIMULATION("Simulado"),
    UNKNOWN("Desconhecida");

    companion object {
        // Função para converter string para SleepSource, útil se o modelo ainda usa string
        fun fromString(source: String?): SleepSource {
            return when (source?.trim()?.lowercase()) {
                "manual" -> MANUAL
                "healthconnect" -> HEALTH_CONNECT
                "simulation" -> SIMULATION
                "googlefit" -> GOOGLE_FIT
                else -> UNKNOWN
            }
        }
    }
}
