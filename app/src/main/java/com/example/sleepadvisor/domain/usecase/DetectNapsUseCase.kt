package com.example.sleepadvisor.domain.usecase

import com.example.sleepadvisor.domain.model.NapAnalysis
import com.example.sleepadvisor.domain.model.SleepSession
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Caso de uso responsável por detectar e analisar sonecas (naps) a partir das sessões de sono.
 * Implementa algoritmos para classificar a qualidade das sonecas e gerar recomendações.
 */
class DetectNapsUseCase @Inject constructor() {

    // Duração mínima para considerar uma sessão como soneca (15 minutos)
    private val MIN_NAP_DURATION = Duration.ofMinutes(15)
    
    // Duração máxima para considerar uma sessão como soneca (3 horas)
    private val MAX_NAP_DURATION = Duration.ofHours(3)
    
    // Horário de início para considerar uma sessão como sono noturno (20:00)
    private val NIGHT_SLEEP_START_TIME = LocalTime.of(20, 0)
    
    // Horário de fim para considerar uma sessão como sono noturno (10:00 do dia seguinte)
    private val NIGHT_SLEEP_END_TIME = LocalTime.of(10, 0)

    /**
     * Detecta e analisa sonecas a partir de uma lista de sessões de sono.
     * 
     * @param sessions Lista de sessões de sono
     * @return Lista de análises de sonecas
     */
    operator fun invoke(sessions: List<SleepSession>): List<NapAnalysis> {
        // Filtrar apenas as sessões que são consideradas sonecas
        val napSessions = sessions.filter { isNap(it) }
        
        // Analisar cada soneca
        return napSessions.map { analyzeNap(it) }
    }
    
    /**
     * Verifica se uma sessão de sono é considerada uma soneca.
     * Uma soneca é definida como:
     * 1. Duração entre 15 minutos e 3 horas
     * 2. Ocorre durante o dia (não durante o horário típico de sono noturno)
     */
    private fun isNap(session: SleepSession): Boolean {
        // Verificar duração
        val duration = session.duration
        if (duration < MIN_NAP_DURATION || duration > MAX_NAP_DURATION) {
            return false
        }
        
        // Verificar horário (deve ser durante o dia)
        val startTimeLocal = session.startTime.atZone(ZoneId.systemDefault()).toLocalTime()
        val endTimeLocal = session.endTime.atZone(ZoneId.systemDefault()).toLocalTime()
        
        // Se a sessão começar após o horário de início do sono noturno e antes do fim do sono noturno do dia seguinte,
        // não é considerada uma soneca
        if (startTimeLocal.isAfter(NIGHT_SLEEP_START_TIME) || startTimeLocal.isBefore(NIGHT_SLEEP_END_TIME)) {
            // Verificação adicional para sessões que cruzam a meia-noite
            if (endTimeLocal.isAfter(NIGHT_SLEEP_START_TIME) || endTimeLocal.isBefore(NIGHT_SLEEP_END_TIME)) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Analisa uma soneca e retorna uma análise detalhada.
     */
    private fun analyzeNap(session: SleepSession): NapAnalysis {
        // Calcular a qualidade da soneca
        val quality = calculateNapQuality(session)
        
        // Determinar o horário ideal para sonecas com base no horário atual
        val idealTime = determineIdealNapTime(session.startTime)
        
        // Gerar recomendações personalizadas
        val recommendations = generateNapRecommendations(session, quality, idealTime)
        
        // Verificar se a soneca pode afetar o sono noturno
        val impactOnNightSleep = analyzeImpactOnNightSleep(session)
        
        return NapAnalysis(
            session = session,
            quality = quality,
            idealTime = idealTime,
            recommendations = recommendations,
            impactOnNightSleep = impactOnNightSleep
        )
    }
    
    /**
     * Calcula a qualidade da soneca com base em diversos fatores.
     * Retorna uma classificação: "Excelente", "Boa", "Regular" ou "Ruim"
     */
    private fun calculateNapQuality(session: SleepSession): String {
        val duration = session.duration
        val startTimeLocal = session.startTime.atZone(ZoneId.systemDefault()).toLocalTime()
        
        // Fator 1: Duração (ideal: 20-30 minutos ou 90 minutos)
        val durationQuality = when {
            duration.toMinutes() in 20..30 -> 3  // Power nap ideal
            duration.toMinutes() in 85..95 -> 3  // Ciclo completo ideal
            duration.toMinutes() in 15..20 -> 2  // Curta, mas aceitável
            duration.toMinutes() in 30..45 -> 2  // Um pouco longa para power nap
            duration.toMinutes() in 45..85 -> 1  // Pode causar inércia do sono
            duration.toMinutes() > 95 -> 0       // Muito longa, pode afetar sono noturno
            else -> 1                            // Outros casos
        }
        
        // Fator 2: Horário (ideal: entre 13:00 e 15:00)
        val timeQuality = when {
            startTimeLocal.isAfter(LocalTime.of(13, 0)) && 
            startTimeLocal.isBefore(LocalTime.of(15, 0)) -> 3  // Horário ideal
            
            startTimeLocal.isAfter(LocalTime.of(12, 0)) && 
            startTimeLocal.isBefore(LocalTime.of(16, 0)) -> 2  // Horário bom
            
            startTimeLocal.isAfter(LocalTime.of(16, 0)) && 
            startTimeLocal.isBefore(LocalTime.of(18, 0)) -> 1  // Tarde demais, pode afetar sono noturno
            
            else -> 2  // Outros horários são aceitáveis
        }
        
        // Calcular pontuação total (0-6)
        val totalScore = durationQuality + timeQuality
        
        return when {
            totalScore >= 5 -> "Excelente"
            totalScore >= 3 -> "Boa"
            totalScore >= 2 -> "Regular"
            else -> "Ruim"
        }
    }
    
    /**
     * Determina o horário ideal para sonecas com base no horário atual.
     * Retorna uma string com o horário recomendado.
     */
    private fun determineIdealNapTime(currentTime: Instant): String {
        val zonedTime = currentTime.atZone(ZoneId.systemDefault())
        // O horário ideal para sonecas é geralmente entre 13:00 e 15:00,
        // quando há uma queda natural no ritmo circadiano
        return "13:00 - 15:00"
    }
    
    /**
     * Gera recomendações personalizadas para sonecas.
     */
    private fun generateNapRecommendations(
        session: SleepSession,
        quality: String,
        idealTime: String
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Recomendações baseadas na duração
        when {
            session.duration.toMinutes() > 30 && session.duration.toMinutes() < 90 -> {
                recommendations.add("Tente limitar suas sonecas a 20-30 minutos (power nap) ou estender para 90 minutos (ciclo completo) para evitar a inércia do sono.")
            }
            session.duration.toMinutes() > 90 -> {
                recommendations.add("Suas sonecas são muito longas. Tente limitar a 20-30 minutos para evitar interferência com o sono noturno.")
            }
        }
        
        // Recomendações baseadas no horário
        val startTimeLocal = session.startTime.atZone(ZoneId.systemDefault()).toLocalTime()
        if (startTimeLocal.isAfter(LocalTime.of(16, 0))) {
            recommendations.add("Evite sonecas após as 16:00, pois podem interferir com seu sono noturno.")
        }
        
        // Recomendações gerais
        if (quality == "Ruim" || quality == "Regular") {
            recommendations.add("O horário ideal para sonecas é entre $idealTime, quando há uma queda natural no ritmo circadiano.")
            recommendations.add("Mantenha suas sonecas curtas (20-30 minutos) para despertar revigorado sem sentir-se grogue.")
        }
        
        // Se não houver recomendações específicas
        if (recommendations.isEmpty()) {
            if (quality == "Excelente") {
                recommendations.add("Continue com este padrão de sonecas. Você está seguindo as melhores práticas!")
            } else {
                recommendations.add("Para sonecas ideais, mantenha a duração entre 20-30 minutos e tente dormir entre $idealTime.")
            }
        }
        
        return recommendations
    }
    
    /**
     * Analisa o possível impacto da soneca no sono noturno.
     */
    private fun analyzeImpactOnNightSleep(session: SleepSession): String {
        val startTimeLocal = session.startTime.atZone(ZoneId.systemDefault()).toLocalTime()
        val duration = session.duration
        
        return when {
            startTimeLocal.isAfter(LocalTime.of(16, 0)) && duration.toMinutes() > 30 -> 
                "Esta soneca pode interferir significativamente com seu sono noturno por ser tarde e longa."
                
            startTimeLocal.isAfter(LocalTime.of(16, 0)) -> 
                "Esta soneca pode atrasar seu sono noturno por ser muito tarde no dia."
                
            duration.toMinutes() > 90 -> 
                "Esta soneca é muito longa e pode reduzir sua pressão de sono à noite."
                
            startTimeLocal.isBefore(LocalTime.of(16, 0)) && duration.toMinutes() <= 30 -> 
                "Esta soneca tem baixo risco de interferir com seu sono noturno."
                
            else -> 
                "Esta soneca tem risco moderado de afetar seu sono noturno."
        }
    }
}
