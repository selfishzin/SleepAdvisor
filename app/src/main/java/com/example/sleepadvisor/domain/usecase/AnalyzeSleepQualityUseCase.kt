package com.example.sleepadvisor.domain.usecase

import com.example.sleepadvisor.domain.model.SleepMetrics
import com.example.sleepadvisor.domain.model.SleepQualityAnalysis
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepStageType
import java.time.Duration
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Caso de uso responsável por analisar a qualidade do sono com base em algoritmos avançados.
 * Implementa fórmulas baseadas em evidências científicas para calcular a pontuação de sono.
 */
class AnalyzeSleepQualityUseCase @Inject constructor() {

    /**
     * Analisa uma sessão de sono e retorna uma análise detalhada da qualidade.
     * 
     * @param session A sessão de sono a ser analisada
     * @return Uma análise detalhada da qualidade do sono
     */
    operator fun invoke(session: SleepSession): SleepQualityAnalysis {
        // Calcular a pontuação de eficiência do sono (0-100)
        val sleepScore = calculateSleepScore(session)
        
        // Determinar a classificação verbal da qualidade do sono
        val qualityLabel = getSleepQualityLabel(sleepScore)
        
        // Analisar os estágios do sono
        val stageAnalysis = analyzeStages(session)
        
        // Analisar a duração do sono
        val durationAnalysis = analyzeDuration(session)
        
        // Analisar a continuidade do sono (despertares)
        val continuityAnalysis = analyzeContinuity(session)
        
        // Gerar recomendações personalizadas com base na análise
        val recommendations = generateRecommendations(
            session, 
            stageAnalysis, 
            durationAnalysis, 
            continuityAnalysis
        )
        
        // Gerar um fato científico personalizado
        val scientificFact = generateScientificFact(session)
        
        return SleepQualityAnalysis(
            score = sleepScore,
            qualityLabel = qualityLabel,
            stageAnalysis = stageAnalysis,
            durationAnalysis = durationAnalysis,
            continuityAnalysis = continuityAnalysis,
            recommendations = recommendations,
            scientificFact = scientificFact
        )
    }
    
    /**
     * Calcula a pontuação de qualidade do sono (0-100) baseada em múltiplos fatores:
     * - Eficiência real do sono (tempo dormindo / tempo total na cama)
     * - Distribuição dos estágios do sono (ideal: Profundo > 20%, REM > 20%, Leve ~55%)
     * - Duração total do sono (ideal: 7-9 horas)
     * - Número de despertares durante a noite
     * - Frequência cardíaca (quando disponível)
     */
    /**
     * Calcula a pontuação de qualidade do sono (0-100) baseada na abordagem do Mi Fitness.
     * O cálculo considera:
     * - Eficiência do sono (tempo dormindo / tempo na cama)
     * - Duração total do sono
     * - Distribuição dos estágios do sono
     * - Número de despertares
     */
    private fun calculateSleepScore(session: SleepSession): Int {
        if (session.duration.toMinutes() <= 0) return 0
        
        // 1. Calcular eficiência do sono (0-35 pontos)
        val efficiency = if (session.stages.isNotEmpty()) {
            SleepMetrics.calculateSleepEfficiency(session.stages, session.duration).toDouble()
        } else {
            // Se não houver estágios, estimar com base em despertares
            val totalMinutes = session.duration.toMinutes().toDouble()
            val awakeMinutes = session.wakeDuringNightCount * 10.0 // Estimativa de 10 minutos por despertar
            
            if (totalMinutes > 0) {
                ((totalMinutes - awakeMinutes) / totalMinutes) * 100
            } else 0.0
        }
        
        // Pontuação baseada na eficiência (0-35 pontos)
        val efficiencyScore = when {
            efficiency >= 98.0 -> 35.0
            efficiency >= 95.0 -> 33.0
            efficiency >= 90.0 -> 30.0
            efficiency >= 85.0 -> 25.0
            efficiency >= 80.0 -> 20.0
            efficiency >= 75.0 -> 15.0
            efficiency >= 70.0 -> 10.0
            efficiency >= 65.0 -> 5.0
            else -> 0.0
        }
        
        // 2. Pontuação baseada na duração do sono (0-35 pontos)
        val totalHours = session.duration.toHours().toDouble()
        val durationScore = when {
            totalHours >= 8.5 && totalHours <= 9.5 -> 35.0  // Ideal para adultos
            totalHours >= 7.5 && totalHours < 8.5 -> 32.0  // Muito bom
            totalHours >= 6.5 && totalHours < 7.5 -> 28.0  // Bom
            totalHours >= 5.5 && totalHours < 6.5 -> 22.0  // Regular
            totalHours >= 4.5 && totalHours < 5.5 -> 15.0  // Curto
            totalHours > 9.5 && totalHours <= 10.5 -> 30.0 // Longo, mas aceitável
            totalHours > 10.5 -> 25.0                      // Muito longo
            else -> 10.0                                   // Muito curto
        }
        
        // 3. Pontuação baseada nos estágios do sono (0-30 pontos)
        var stageScore = 0.0
        if (session.stages.isNotEmpty()) {
            // Pontuação para sono profundo (0-15 pontos)
            val deepSleepScore = when (session.deepSleepPercentage) {
                in 20.0..100.0 -> 15.0  // Excelente (20%+)
                in 15.0..20.0 -> 12.0   // Bom
                in 10.0..15.0 -> 8.0     // Regular
                in 5.0..10.0 -> 4.0      // Baixo
                else -> 0.0              // Muito baixo
            }
            
            // Pontuação para sono REM (0-15 pontos)
            val remSleepScore = when (session.remSleepPercentage) {
                in 25.0..100.0 -> 15.0  // Excelente (25%+)
                in 20.0..25.0 -> 12.0   // Bom
                in 15.0..20.0 -> 8.0     // Regular
                in 10.0..15.0 -> 4.0     // Baixo
                else -> 0.0              // Muito baixo
            }
            
            stageScore = deepSleepScore + remSleepScore
        } else {
            // Se não houver dados de estágios, dar uma pontuação média
            stageScore = 20.0
        }
        
        // 4. Consistência do horário de dormir/acordar (se disponível)
        val consistencyFactor = 0.0 // TODO: Implementar análise de consistência
        
        // 5. Frequência cardíaca noturna (se disponível)
        val heartRateFactor = 0.0 // TODO: Implementar análise de frequência cardíaca
        
        // 6. Número de despertares (se disponível)
        val wakeFactor = when (session.wakeDuringNightCount) {
            0 -> 100.0  // Sem despertares - pontuação máxima
            1 -> 80.0   // 1 despertar - bom
            2 -> 60.0   // 2 despertares - aceitável
            3 -> 40.0   // 3 despertares - abaixo da média
            else -> 20.0 // Muitos despertares - baixa pontuação
        }
        
        // Calcular pontuação final com pesos
        // Eficiência: 70%
        // Estágios do sono: 20%
        // Duração: 10%
        // Despertares: 10% (máximo de 10 pontos)
        val weightedScore = (efficiencyScore * 0.7) + 
                          (stageScore * 0.2) + 
                          (durationScore * 0.1) + 
                          (wakeFactor * 0.1)
        
        // Ajustar para garantir que a pontuação esteja entre 0 e 100
        val finalScore = weightedScore.coerceIn(0.0, 100.0)
        
        return finalScore.roundToInt()
    }
    
    /**
     * Determina a classificação verbal da qualidade do sono com base na pontuação.
     * @deprecated Use SleepMetrics.getSleepQualityLabel() em vez disso
     */
    @Deprecated("Use SleepMetrics.getSleepQualityLabel() instead")
    private fun getSleepQualityLabel(score: Int): String {
        return SleepMetrics.getSleepQualityLabel(score)
    }
    
    /**
     * Analisa a distribuição dos estágios do sono e retorna uma análise detalhada.
     */
    private fun analyzeStages(session: SleepSession): String {
        if (session.stages.isEmpty()) {
            return "Dados de estágios do sono não disponíveis."
        }
        
        val analysis = StringBuilder()
        
        // Usar SleepMetrics para cálculos consistentes
        if (session.stages.isEmpty()) {
            return "Dados insuficientes para análise dos estágios do sono."
        }
        
        // Obter porcentagens usando SleepMetrics
        val stagePercentages = SleepMetrics.calculateAllStagePercentages(session.stages)
        val deepSleepPercentage = stagePercentages[SleepStageType.DEEP] ?: 0.0
        val remSleepPercentage = stagePercentages[SleepStageType.REM] ?: 0.0
        val lightSleepPercentage = stagePercentages[SleepStageType.LIGHT] ?: 0.0
        
        // Analisar sono profundo
        when {
            deepSleepPercentage >= 25.0 -> 
                analysis.append("Excelente quantidade de sono profundo (${deepSleepPercentage.toInt()}%), " +
                               "favorecendo a recuperação física e imunidade. ")
            deepSleepPercentage >= 20.0 -> 
                analysis.append("Boa quantidade de sono profundo (${deepSleepPercentage.toInt()}%), " +
                               "adequada para recuperação física. ")
            deepSleepPercentage >= 15.0 -> 
                analysis.append("Quantidade adequada de sono profundo (${deepSleepPercentage.toInt()}%), " +
                               "mas poderia ser melhor. ")
            else -> 
                analysis.append("Quantidade insuficiente de sono profundo (${deepSleepPercentage.toInt()}%), " +
                               "o que pode afetar sua recuperação física e imunidade. ")
        }
        
        // Analisar sono REM
        when {
            remSleepPercentage >= 25.0 -> 
                analysis.append("Excelente quantidade de sono REM (${remSleepPercentage.toInt()}%), " +
                               "favorecendo a consolidação da memória e regulação emocional. ")
            remSleepPercentage >= 20.0 -> 
                analysis.append("Boa quantidade de sono REM (${remSleepPercentage.toInt()}%), " +
                               "adequada para funções cognitivas. ")
            remSleepPercentage >= 15.0 -> 
                analysis.append("Quantidade adequada de sono REM (${remSleepPercentage.toInt()}%), " +
                               "mas poderia ser melhor. ")
            else -> 
                analysis.append("Quantidade insuficiente de sono REM (${remSleepPercentage.toInt()}%), " +
                               "o que pode afetar sua memória e processamento emocional. ")
        }
        
        // Analisar equilíbrio entre estágios
        if (session.lightSleepPercentage in 50.0..60.0 &&
            session.deepSleepPercentage >= 20.0 &&
            session.remSleepPercentage >= 20.0) {
            analysis.append("Distribuição ideal entre os estágios do sono, indicando um ciclo completo e saudável.")
        }
        
        return analysis.toString()
    }
    
    /**
     * Analisa a duração do sono e retorna uma análise detalhada.
     */
    private fun analyzeDuration(session: SleepSession): String {
        val totalHours = session.duration.toHours()
        val totalMinutes = session.duration.toMinutesPart()
        
        return when {
            totalHours >= 7 && totalHours <= 9 -> 
                "Duração ideal de sono: ${totalHours}h${totalMinutes}min. Está dentro da faixa recomendada de 7-9 horas para adultos."
            totalHours >= 6 && totalHours < 7 -> 
                "Duração um pouco abaixo do ideal: ${totalHours}h${totalMinutes}min. O recomendado para adultos é 7-9 horas."
            totalHours > 9 && totalHours <= 10 -> 
                "Duração um pouco acima do ideal: ${totalHours}h${totalMinutes}min. O recomendado para adultos é 7-9 horas."
            totalHours > 10 -> 
                "Duração excessiva: ${totalHours}h${totalMinutes}min. Dormir mais de 10 horas regularmente pode estar associado a problemas de saúde."
            totalHours >= 5 && totalHours < 6 -> 
                "Duração insuficiente: ${totalHours}h${totalMinutes}min. Está abaixo da recomendação mínima de 7 horas para adultos."
            else -> 
                "Duração muito insuficiente: ${totalHours}h${totalMinutes}min. A privação crônica de sono pode afetar seriamente sua saúde."
        }
    }
    
    /**
     * Analisa a continuidade do sono (despertares) e retorna uma análise detalhada.
     */
    private fun analyzeContinuity(session: SleepSession): String {
        return when (session.wakeDuringNightCount) {
            0 -> "Continuidade excelente: sem despertares durante a noite, permitindo ciclos de sono completos."
            1 -> "Continuidade muito boa: apenas um despertar durante a noite, o que é considerado normal."
            2 -> "Continuidade boa: dois despertares durante a noite, ainda dentro do normal para muitos adultos."
            3 -> "Continuidade regular: três despertares durante a noite, o que pode fragmentar os ciclos de sono."
            4 -> "Continuidade ruim: quatro despertares durante a noite, causando fragmentação significativa do sono."
            else -> "Continuidade muito ruim: ${session.wakeDuringNightCount} despertares durante a noite, " +
                   "resultando em sono altamente fragmentado, o que afeta a qualidade geral do descanso."
        }
    }
    
    /**
     * Gera recomendações personalizadas com base na análise do sono.
     */
    private fun generateRecommendations(
        session: SleepSession,
        stageAnalysis: String,
        durationAnalysis: String,
        continuityAnalysis: String
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Recomendações baseadas nos estágios do sono
        // Verifica se os estágios do sono são válidos (não são zero quando deveriam ter valores)
        val hasValidStages = session.stages.isNotEmpty() || 
                           (session.deepSleepPercentage > 0 && session.remSleepPercentage > 0 && session.lightSleepPercentage > 0)
        
        if (hasValidStages && session.deepSleepPercentage < 15.0) {
            recommendations.add("Para aumentar o sono profundo: mantenha o quarto fresco (18-20°C), evite álcool antes de dormir e considere exercícios físicos durante o dia.")
        }
        
        if (hasValidStages && session.remSleepPercentage < 15.0) {
            recommendations.add("Para melhorar o sono REM: mantenha horários regulares de sono, evite cafeína após o meio-dia e pratique técnicas de relaxamento antes de dormir.")
        }
        
        // Recomendações baseadas na duração
        val totalHours = session.duration.toHours()
        if (totalHours < 7) {
            recommendations.add("Tente aumentar seu tempo total de sono para pelo menos 7 horas, indo para a cama 30-60 minutos mais cedo.")
        } else if (totalHours > 9) {
            recommendations.add("Considere reduzir ligeiramente seu tempo na cama. Embora dormir seja importante, dormir mais de 9 horas regularmente pode estar associado a problemas de saúde.")
        }
        
        // Recomendações baseadas na continuidade
        if (session.wakeDuringNightCount > 2) {
            recommendations.add("Para reduzir despertares noturnos: limite a ingestão de líquidos 2 horas antes de dormir, mantenha o quarto escuro e silencioso, e evite telas antes de dormir.")
        }
        
        // Recomendações gerais se a lista estiver vazia
        if (recommendations.isEmpty()) {
            recommendations.add("Continue mantendo seus bons hábitos de sono. Horários regulares e ambiente adequado são essenciais para um sono de qualidade.")
        }
        
        return recommendations
    }
    
    /**
     * Gera um fato científico personalizado com base na sessão de sono.
     */
    private fun generateScientificFact(session: SleepSession): String {
        // Lista de fatos científicos sobre o sono
        val facts = listOf(
            "O sono REM está ligado à regulação emocional e ao processamento de memórias.",
            "Durante o sono profundo, o cérebro consolida memórias e o corpo libera hormônio do crescimento.",
            "Um ciclo completo de sono dura cerca de 90 minutos, passando por estágios leve, profundo e REM.",
            "A melatonina, hormônio que regula o sono, é suprimida pela luz azul de telas.",
            "Adultos precisam em média de 7-9 horas de sono por noite para funções cognitivas ótimas.",
            "O sono profundo é essencial para a recuperação física do corpo.",
            "A temperatura ideal do quarto para dormir é entre 18-20°C.",
            "Durante o sono REM, o corpo fica temporariamente paralisado para evitar que atuemos nossos sonhos."
        )
        
        // Seleciona um fato baseado nos dados de sono
        return when {
            // Verifica se os estágios do sono são válidos (não são zero quando deveriam ter valores)
            session.hasValidStages() && session.remSleepPercentage > 25 -> 
                "Você teve uma excelente quantidade de sono REM (${session.remSleepPercentage.toInt()}%). O sono REM é essencial para a consolidação da memória e criatividade."
            
            session.hasValidStages() && session.deepSleepPercentage > 25 -> 
                "Você teve uma excelente quantidade de sono profundo (${session.deepSleepPercentage.toInt()}%). O sono profundo é quando ocorre a maior parte da recuperação física."
            
            session.hasValidStages() && session.deepSleepPercentage < 15 && session.deepSleepPercentage > 0 -> 
                "Você teve apenas ${session.deepSleepPercentage.toInt()}% de sono profundo. O sono profundo é essencial para a recuperação física e imunidade."
            
            session.hasValidStages() && session.remSleepPercentage < 15 && session.remSleepPercentage > 0 -> 
                "Você teve apenas ${session.remSleepPercentage.toInt()}% de sono REM. O sono REM é importante para a saúde mental e processamento emocional."
            
            session.duration.toHours() > 9 -> 
                "Você dormiu mais de 9 horas. Embora o sono seja importante, dormir demais regularmente pode estar associado a problemas de saúde."
            
            session.duration.toHours() < 6 -> 
                "Você dormiu menos de 6 horas. A privação crônica de sono pode afetar negativamente a memória, humor e imunidade."
            
            else -> facts.random()
        }
    }
}
