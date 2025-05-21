package com.example.sleepadvisor.domain.usecase

import com.example.sleepadvisor.domain.model.SleepQualityAnalysis
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepStageType
import java.time.Duration
import javax.inject.Inject

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
    private fun calculateSleepScore(session: SleepSession): Int {
        if (session.duration.toMinutes() <= 0) return 0
        
        // Base score começa em 75 (padrão médio)
        var score = 75.0
        
        // 1. Eficiência real do sono (tempo dormindo / tempo total na cama)
        val totalMinutes = session.duration.toMinutes().toDouble()
        val awakeMinutes = if (session.stages.isNotEmpty()) {
            session.stages
                .filter { it.type == SleepStageType.AWAKE }
                .fold(Duration.ZERO) { acc, stage -> acc.plus(stage.duration) }
                .toMinutes().toDouble()
        } else {
            // Estimativa baseada no número de despertares
            session.wakeDuringNightCount * 10.0 // Estimativa de 10 minutos por despertar
        }
        
        val sleepEfficiency = if (totalMinutes > 0) {
            ((totalMinutes - awakeMinutes) / totalMinutes) * 100
        } else 0.0
        
        // Ajustar score base pela eficiência real
        score = sleepEfficiency
        
        // 2. Distribuição dos estágios do sono
        // Ideal: Profundo > 20%, REM > 20%, Leve ~55%
        if (session.stages.isNotEmpty()) {
            // Bônus/penalidade para sono profundo
            val deepSleepFactor = when {
                session.deepSleepPercentage >= 25.0 -> 10.0  // Excelente
                session.deepSleepPercentage >= 20.0 -> 5.0   // Bom
                session.deepSleepPercentage >= 15.0 -> 0.0   // Adequado
                session.deepSleepPercentage >= 10.0 -> -5.0  // Abaixo do ideal
                else -> -10.0                                // Muito baixo
            }
            
            // Bônus/penalidade para sono REM
            val remSleepFactor = when {
                session.remSleepPercentage >= 25.0 -> 10.0   // Excelente
                session.remSleepPercentage >= 20.0 -> 5.0    // Bom
                session.remSleepPercentage >= 15.0 -> 0.0    // Adequado
                session.remSleepPercentage >= 10.0 -> -5.0   // Abaixo do ideal
                else -> -10.0                                // Muito baixo
            }
            
            // Bônus/penalidade para equilíbrio entre estágios
            val balanceFactor = if (
                session.lightSleepPercentage in 50.0..60.0 &&
                session.deepSleepPercentage >= 20.0 &&
                session.remSleepPercentage >= 20.0
            ) {
                5.0  // Distribuição ideal de estágios
            } else {
                0.0
            }
            
            score += deepSleepFactor + remSleepFactor + balanceFactor
        }
        
        // 3. Duração total do sono (ideal: 7-9 horas)
        val totalHours = session.duration.toHours().toDouble()
        val durationFactor = when {
            totalHours >= 7.0 && totalHours <= 9.0 -> 5.0   // Ideal
            totalHours >= 6.0 && totalHours < 7.0 -> 0.0    // Aceitável
            totalHours > 9.0 && totalHours <= 10.0 -> 0.0   // Aceitável
            totalHours > 10.0 -> -5.0                       // Muito longo
            totalHours >= 5.0 && totalHours < 6.0 -> -5.0   // Curto
            totalHours < 5.0 -> -10.0                       // Muito curto
            else -> 0.0
        }
        score += durationFactor
        
        // 4. Penalidade por despertares
        val wakeCountFactor = when (session.wakeDuringNightCount) {
            0 -> 5.0    // Excelente - sem despertares
            1 -> 2.0    // Muito bom - um despertar é normal
            2 -> 0.0    // Normal
            3 -> -3.0   // Abaixo do ideal
            4 -> -5.0   // Ruim
            else -> -10.0  // Muito ruim - sono muito fragmentado
        }
        score += wakeCountFactor
        
        // Garantir que a pontuação fique entre 0 e 100
        return score.coerceIn(0.0, 100.0).toInt()
    }
    
    /**
     * Determina a classificação verbal da qualidade do sono com base na pontuação.
     */
    private fun getSleepQualityLabel(score: Int): String {
        return when {
            score >= 90 -> "Excelente"
            score >= 80 -> "Muito Boa"
            score >= 70 -> "Boa"
            score >= 60 -> "Regular"
            score >= 50 -> "Razoável"
            score >= 40 -> "Insatisfatória"
            else -> "Ruim"
        }
    }
    
    /**
     * Analisa a distribuição dos estágios do sono e retorna uma análise detalhada.
     */
    private fun analyzeStages(session: SleepSession): String {
        if (session.stages.isEmpty()) {
            return "Dados de estágios do sono não disponíveis."
        }
        
        val analysis = StringBuilder()
        
        // Analisar sono profundo
        when {
            session.deepSleepPercentage >= 25.0 -> 
                analysis.append("Excelente quantidade de sono profundo (${session.deepSleepPercentage.toInt()}%), " +
                               "favorecendo a recuperação física e imunidade. ")
            session.deepSleepPercentage >= 20.0 -> 
                analysis.append("Boa quantidade de sono profundo (${session.deepSleepPercentage.toInt()}%), " +
                               "adequada para recuperação física. ")
            session.deepSleepPercentage >= 15.0 -> 
                analysis.append("Quantidade adequada de sono profundo (${session.deepSleepPercentage.toInt()}%), " +
                               "mas poderia ser melhor. ")
            session.deepSleepPercentage < 15.0 -> 
                analysis.append("Quantidade insuficiente de sono profundo (${session.deepSleepPercentage.toInt()}%), " +
                               "o que pode afetar sua recuperação física e imunidade. ")
        }
        
        // Analisar sono REM
        when {
            session.remSleepPercentage >= 25.0 -> 
                analysis.append("Excelente quantidade de sono REM (${session.remSleepPercentage.toInt()}%), " +
                               "favorecendo a consolidação da memória e regulação emocional. ")
            session.remSleepPercentage >= 20.0 -> 
                analysis.append("Boa quantidade de sono REM (${session.remSleepPercentage.toInt()}%), " +
                               "adequada para funções cognitivas. ")
            session.remSleepPercentage >= 15.0 -> 
                analysis.append("Quantidade adequada de sono REM (${session.remSleepPercentage.toInt()}%), " +
                               "mas poderia ser melhor. ")
            session.remSleepPercentage < 15.0 -> 
                analysis.append("Quantidade insuficiente de sono REM (${session.remSleepPercentage.toInt()}%), " +
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
