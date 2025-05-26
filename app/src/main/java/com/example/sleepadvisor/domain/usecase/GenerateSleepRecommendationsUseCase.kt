package com.example.sleepadvisor.domain.usecase

import com.example.sleepadvisor.domain.model.NapAnalysis
import com.example.sleepadvisor.domain.model.SleepQualityAnalysis
import com.example.sleepadvisor.domain.model.SleepRecommendations
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepTrendAnalysis
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Caso de uso responsável por gerar recomendações personalizadas com base em todos os dados de sono.
 * Combina análises de qualidade, tendências e sonecas para criar recomendações abrangentes.
 */
class GenerateSleepRecommendationsUseCase @Inject constructor() {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Gera recomendações personalizadas abrangentes com base em múltiplas análises.
     * 
     * @param lastSession Última sessão de sono registrada
     * @param qualityAnalysis Análise de qualidade da última sessão
     * @param trendAnalysis Análise de tendências semanais
     * @param napAnalyses Lista de análises de sonecas
     * @return Recomendações personalizadas abrangentes
     */
    operator fun invoke(
        lastSession: SleepSession?,
        qualityAnalysis: SleepQualityAnalysis?,
        trendAnalysis: SleepTrendAnalysis?,
        napAnalyses: List<NapAnalysis>
    ): SleepRecommendations {
        // Lista de recomendações prioritárias
        val priorityRecommendations = mutableListOf<String>()
        
        // Lista de recomendações gerais
        val generalRecommendations = mutableListOf<String>()
        
        // Lista de pontos positivos a reforçar
        val positiveReinforcements = mutableListOf<String>()
        
        // Fatos científicos relevantes
        val scientificFacts = mutableListOf<String>()
        
        // Processar análise de qualidade da última sessão
        if (qualityAnalysis != null) {
            processQualityAnalysis(
                qualityAnalysis,
                priorityRecommendations,
                generalRecommendations,
                positiveReinforcements,
                scientificFacts
            )
        }
        
        // Processar análise de tendências
        if (trendAnalysis != null) {
            processTrendAnalysis(
                trendAnalysis,
                priorityRecommendations,
                generalRecommendations,
                positiveReinforcements
            )
        }
        
        // Processar análises de sonecas
        if (napAnalyses.isNotEmpty()) {
            processNapAnalyses(
                napAnalyses,
                priorityRecommendations,
                generalRecommendations
            )
        }
        
        // Gerar horário ideal para dormir e acordar
        val (idealBedtime, idealWakeTime) = calculateIdealSleepSchedule(lastSession, trendAnalysis)
        
        // Gerar horário ideal para sonecas
        val idealNapTime = calculateIdealNapTime(napAnalyses)
        
        // Selecionar um fato científico
        val selectedFact = if (scientificFacts.isNotEmpty()) {
            scientificFacts.random()
        } else {
            "O sono é dividido em ciclos de aproximadamente 90 minutos, cada um com estágios de sono leve, profundo e REM."
        }
        
        // Remover duplicatas e limitar o número de recomendações
        val uniquePriorityRecommendations = priorityRecommendations.distinct().take(3)
        val uniqueGeneralRecommendations = generalRecommendations.distinct()
            .filterNot { rec -> uniquePriorityRecommendations.any { it.contains(rec) || rec.contains(it) } }
            .take(5)
        
        val uniquePositiveReinforcements = positiveReinforcements.distinct().take(2)
        
        return SleepRecommendations(
            priorityRecommendations = uniquePriorityRecommendations,
            generalRecommendations = uniqueGeneralRecommendations,
            positiveReinforcements = uniquePositiveReinforcements,
            scientificFact = selectedFact,
            idealBedtime = idealBedtime,
            idealWakeTime = idealWakeTime,
            idealNapTime = idealNapTime
        )
    }

    /**
     * Gera recomendações específicas para uma única sessão de sono.
     *
     * @param session A sessão de sono a ser analisada.
     * @param qualityAnalysis A análise de qualidade para a sessão fornecida.
     * @return Lista de recomendações string para a sessão.
     */
    fun invokeForSingleSession(
        session: SleepSession,
        qualityAnalysis: SleepQualityAnalysis
    ): List<String> {
        val recommendations = mutableListOf<String>()

        val durationHours = session.duration.toHours()
        val durationMinutes = session.duration.toMinutesPart()
        val formattedDuration = "${durationHours}h${String.format("%02d", durationMinutes)}min"

        // Determinar se é uma soneca
        // Considerar uma sessão como soneca se durar menos de 3 horas
        // E não começar tarde da noite (ex: após 22h) nem muito cedo pela manhã (ex: antes das 2h, o que poderia ser continuação do sono noturno)
        val isNap = session.duration < Duration.ofHours(3) &&
                (session.startTime.atZone(ZoneId.systemDefault()).hour >= 2 && session.startTime.atZone(ZoneId.systemDefault()).hour < 22)

        // Análise de duração
        if (isNap) {
            // Recomendações para sonecas
            when {
                session.duration > Duration.ofMinutes(90) -> recommendations.add("Sua soneca foi longa ($formattedDuration). Sonecas ideais costumam durar entre 20-30 minutos ou um ciclo completo de 90 minutos para evitar grogue ao acordar.")
                session.duration < Duration.ofMinutes(20) -> recommendations.add("Sua soneca foi muito curta ($formattedDuration). Para obter benefícios restauradores, tente descansar por pelo menos 20-30 minutos.")
                else -> recommendations.add("Sua soneca teve uma boa duração ($formattedDuration). Isso pode ajudar a melhorar o alerta e o desempenho.")
            }
            if (session.startTime.atZone(ZoneId.systemDefault()).hour >= 15) {
                recommendations.add("Você cochilou às ${timeFormatter.format(session.startTime.atZone(ZoneId.systemDefault()))}. Cochilar tarde pode dificultar o sono noturno. Se precisar, tente cochilar mais cedo.")
            }
        } else {
            // Recomendações para sono noturno
            when {
                durationHours < 6 -> recommendations.add("Seu sono noturno foi curto ($formattedDuration). Adultos geralmente precisam de 7-9 horas de sono por noite para uma saúde e bem-estar ótimos.")
                durationHours > 9 -> recommendations.add("Seu sono noturno foi longo ($formattedDuration). Embora raro, dormir excessivamente de forma consistente pode estar associado a alguns problemas de saúde. Se isso for um padrão, observe como você se sente.")
                else -> recommendations.add("Sua duração de sono noturno ($formattedDuration) está dentro da faixa ideal de 7-9 horas. Excelente!")
            }
        }

        // Análise de qualidade (baseada na pontuação já calculada)
        val qualityMessage = when {
            qualityAnalysis.score >= 85 -> "Excelente qualidade de sono (${qualityAnalysis.score}/100)! Você provavelmente teve uma boa proporção de estágios de sono restauradores."
            qualityAnalysis.score >= 70 -> "Boa qualidade de sono (${qualityAnalysis.score}/100). Pequenos ajustes em sua rotina ou ambiente podem elevar ainda mais seu descanso."
            qualityAnalysis.score >= 50 -> "Qualidade de sono razoável (${qualityAnalysis.score}/100). Considere revisar seus hábitos pré-sono e o conforto do seu quarto."
            else -> "Qualidade de sono abaixo do ideal (${qualityAnalysis.score}/100). Pode ser útil focar em melhorar a consistência dos horários e o ambiente de sono."
        }
        recommendations.add(qualityMessage)

        // Recomendações baseadas em despertares
        if (!isNap) { // Despertares são mais relevantes para sono noturno
            when (session.wakeDuringNightCount) {
                0 -> recommendations.add("Você não teve despertares registrados durante a noite. Isso é ótimo para a continuidade do sono!")
                1, 2 -> recommendations.add("Você teve ${session.wakeDuringNightCount} despertar(es). É normal acordar brevemente, mas muitos despertares podem fragmentar o sono.")
                else -> recommendations.add("Você teve ${session.wakeDuringNightCount} despertares. Despertares frequentes podem impactar a qualidade do seu descanso. Verifique se há ruídos ou luzes perturbadoras.")
            }
        }
        
        // Adicionar recomendação específica da análise de qualidade original, se houver e for diferente das geradas
        qualityAnalysis.recommendations.firstOrNull()?.let { rec ->
            if (recommendations.none { it.contains(rec.take(20)) }) { // Evitar duplicatas parciais
                recommendations.add(rec)
            }
        }

        return recommendations.distinct().take(4) // Limitar a 3-4 recomendações mais relevantes
    }
    
    /**
     * Processa a análise de qualidade para extrair recomendações.
     */
    private fun processQualityAnalysis(
        qualityAnalysis: SleepQualityAnalysis,
        priorityRecommendations: MutableList<String>,
        generalRecommendations: MutableList<String>,
        positiveReinforcements: MutableList<String>,
        scientificFacts: MutableList<String>
    ) {
        // Adicionar recomendações da análise de qualidade
        qualityAnalysis.recommendations.forEach { recommendation ->
            if (priorityRecommendations.size < 2) {
                priorityRecommendations.add(recommendation)
            } else {
                generalRecommendations.add(recommendation)
            }
        }
        
        // Adicionar fato científico
        scientificFacts.add(qualityAnalysis.scientificFact)
        
        // Adicionar reforço positivo se a qualidade for boa
        if (qualityAnalysis.score >= 70) {
            positiveReinforcements.add("Sua última noite de sono teve uma qualidade ${qualityAnalysis.qualityLabel.lowercase()} (${qualityAnalysis.score}/100). Continue com os bons hábitos!")
        }
    }
    
    /**
     * Processa a análise de tendências para extrair recomendações.
     */
    private fun processTrendAnalysis(
        trendAnalysis: SleepTrendAnalysis,
        priorityRecommendations: MutableList<String>,
        generalRecommendations: MutableList<String>,
        positiveReinforcements: MutableList<String>
    ) {
        // Adicionar recomendações da análise de tendências
        trendAnalysis.recommendations?.forEach { recommendation ->
            if (recommendation.contains("jet lag social") || 
                recommendation.contains("consistência") ||
                recommendation.contains("diminuindo")) {
                priorityRecommendations.add(recommendation)
            } else {
                generalRecommendations.add(recommendation)
            }
        }
        
        // Adicionar reforço positivo se a consistência for boa
        trendAnalysis.consistencyScore?.let { score ->
            if (score >= 75) {
                val level = trendAnalysis.consistencyLevel?.lowercase() ?: "boa"
                positiveReinforcements.add("Sua consistência de sono é $level ($score/100). Manter horários regulares é fundamental para um sono de qualidade!")
            }
        }
        
        // Adicionar informação sobre duração média
        trendAnalysis.averageSleepDuration?.let { duration ->
            val hours = duration.toHours()
            val minutes = duration.toMinutesPart()
            
            if (hours >= 7 && hours <= 9) {
                positiveReinforcements.add("Sua duração média de sono é de ${hours}h${minutes}min, dentro da faixa recomendada de 7-9 horas para adultos.")
            }
        }
    }
    
    /**
     * Processa as análises de sonecas para extrair recomendações.
     */
    private fun processNapAnalyses(
        napAnalyses: List<NapAnalysis>,
        priorityRecommendations: MutableList<String>,
        generalRecommendations: MutableList<String>
    ) {
        // Se houver sonecas recentes
        if (napAnalyses.isNotEmpty()) {
            // Verificar se há sonecas com impacto negativo no sono noturno
            val negativeImpactNaps = napAnalyses.filter { 
                it.impactOnNightSleep.contains("significativamente") || 
                it.impactOnNightSleep.contains("interferir")
            }
            
            if (negativeImpactNaps.isNotEmpty()) {
                priorityRecommendations.add("Suas sonecas recentes podem estar afetando seu sono noturno. Tente limitar sonecas a 20-30 minutos e evite dormir após as 15:00.")
            }
            
            // Adicionar recomendações das sonecas
            napAnalyses.flatMap { it.recommendations }
                .distinct()
                .take(2)
                .forEach { generalRecommendations.add(it) }
        }
    }
    
    /**
     * Calcula o horário ideal para dormir e acordar.
     * Retorna um par com (horário ideal para dormir, horário ideal para acordar)
     */
    private fun calculateIdealSleepSchedule(
        lastSession: SleepSession?,
        trendAnalysis: SleepTrendAnalysis?
    ): Pair<String, String> {
        // Se houver análise de tendências, usar os horários médios como base
        if (trendAnalysis != null && 
            !trendAnalysis.averageBedtime.isNullOrEmpty() && 
            !trendAnalysis.averageWakeTime.isNullOrEmpty()) {
            // Ajustar para garantir 7-8 horas de sono
            try {
                val avgBedtime = LocalTime.parse(trendAnalysis.averageBedtime, timeFormatter)
                val avgWakeTime = LocalTime.parse(trendAnalysis.averageWakeTime, timeFormatter)
                
                // Calcular duração entre os horários médios
                var durationHours = if (avgWakeTime.isBefore(avgBedtime)) {
                    (24 - avgBedtime.hour) + avgWakeTime.hour
                } else {
                    avgWakeTime.hour - avgBedtime.hour
                }
                
                // Ajustar bedtime se necessário para garantir 7-8 horas
                val idealBedtime = if (durationHours < 7) {
                    avgWakeTime.minusHours(8)
                } else if (durationHours > 9) {
                    avgWakeTime.minusHours(8)
                } else {
                    avgBedtime
                }
                
                return Pair(
                    idealBedtime.format(timeFormatter),
                    avgWakeTime.format(timeFormatter)
                )
            } catch (e: Exception) {
                // Fallback para horários padrão
                return Pair("22:30", "06:30")
            }
        }
        
        // Se houver última sessão, usar como base
        if (lastSession != null) {
            val lastWakeTime = lastSession.endTime.atZone(ZoneId.systemDefault()).toLocalTime()
            val idealBedtime = lastWakeTime.minusHours(16).plusHours(8) // 16 horas após acordar
            
            return Pair(
                idealBedtime.format(timeFormatter),
                lastWakeTime.format(timeFormatter)
            )
        }
        
        // Horários padrão se não houver dados
        return Pair("22:30", "06:30")
    }
    
    /**
     * Calcula o horário ideal para sonecas.
     */
    private fun calculateIdealNapTime(napAnalyses: List<NapAnalysis>): String {
        // Se houver análises de sonecas, usar o horário ideal mais comum
        if (napAnalyses.isNotEmpty()) {
            return napAnalyses.first().idealTime
        }
        
        // Horário padrão se não houver dados
        return "13:00 - 15:00"
    }
}
