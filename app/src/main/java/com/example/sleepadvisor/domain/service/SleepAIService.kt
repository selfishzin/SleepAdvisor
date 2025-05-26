package com.example.sleepadvisor.domain.service

import com.example.sleepadvisor.domain.model.SleepMetrics
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepSource
import com.example.sleepadvisor.domain.model.SleepStage
import com.example.sleepadvisor.domain.model.SleepStageType
import com.example.sleepadvisor.domain.usecase.CalculateCustomSleepStagesUseCase
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import javax.inject.Inject
import javax.inject.Singleton

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Serviço que utiliza IA avançada para gerar análises detalhadas e dicas personalizadas de sono
 * Integra dados do Health Connect e utiliza algoritmos de simulação para estágios do sono
 */
@Singleton
class SleepAIService @Inject constructor(
    @Suppress("UNUSED_PARAMETER") private val okHttpClient: OkHttpClient,
    @Suppress("UNUSED_PARAMETER") private val gson: Gson,
    @Suppress("UNUSED_PARAMETER") private val calculateCustomSleepStagesUseCase: CalculateCustomSleepStagesUseCase
) {
    
    /**
     * Gera dicas personalizadas baseadas nos dados de sono da última semana
     * Inclui análises individualizadas por noite e simulação de ciclos de sono
     */
    suspend fun generateSleepAdvice(weekSessions: List<SleepSession>): SleepAdvice = withContext(Dispatchers.IO) {
        try {
            if (weekSessions.isEmpty()) {
                return@withContext SleepAdvice(
                    mainAdvice = "Registre seu sono por alguns dias para receber dicas personalizadas.",
                    supportingFacts = listOf("Sabia que o sono é dividido em 4-6 ciclos de 90 minutos cada? Cada ciclo passa por estágios leve, profundo e REM."),
                    customRecommendations = emptyList(),
                    qualityTrend = null
                )
            }
            
            // Preparar dados para análise
            val sleepData = weekSessions.map { session ->
                SleepDataDto(
                    date = dateFormatter.format(session.startTime.atZone(ZoneId.systemDefault())),
                    totalSleep = "${session.duration.toHours()}h${session.duration.toMinutesPart()}min",
                    deepSleepPercentage = session.deepSleepPercentage,
                    remSleepPercentage = session.remSleepPercentage,
                    lightSleepPercentage = session.lightSleepPercentage,
                    wakeCount = session.wakeDuringNightCount,
                    efficiency = session.efficiency,
                    heartRateAvg = calculateHeartRate(session),
                    isDataEstimated = session.stages.isEmpty()
                )
            }
            
            // Gerar análise detalhada para cada noite
            val dailyAnalyses = weekSessions.map { session ->
                val sleepStages = createSleepStagesDetail(session)
                val sleepScore = calculateSleepScore(session, sleepStages)
                
                val sessionData = SleepDataDto(
                    date = dateFormatter.format(session.startTime.atZone(ZoneId.systemDefault())),
                    totalSleep = "${session.duration.toHours()}h${session.duration.toMinutesPart()}min",
                    deepSleepPercentage = session.deepSleepPercentage,
                    remSleepPercentage = session.remSleepPercentage,
                    lightSleepPercentage = session.lightSleepPercentage,
                    wakeCount = session.wakeDuringNightCount,
                    efficiency = session.efficiency,
                    heartRateAvg = calculateHeartRate(session),
                    isDataEstimated = session.stages.isEmpty()
                )
                
                val dailyPrompt = buildDailyPrompt(sessionData)
                val dailyResponse = callAIApi(dailyPrompt)
                
                try {
                    val basicAnalysis = gson.fromJson(dailyResponse, DailyAnalysis::class.java)
                    
                    DailyAnalysis(
                        date = dateFormatter.format(session.startTime.atZone(ZoneId.systemDefault())),
                        analysis = basicAnalysis.analysis,
                        recommendations = basicAnalysis.recommendations,
                        sleepScore = sleepScore,
                        sleepQuality = getSleepQualityLabel(sleepScore),
                        sleepStages = sleepStages,
                        scientificFact = generateScientificFact(session, sleepStages)
                    )
                } catch (e: Exception) {
                    DailyAnalysis(
                        date = dateFormatter.format(session.startTime.atZone(ZoneId.systemDefault())),
                        analysis = "Análise detalhada não disponível para esta noite.",
                        recommendations = listOf("Tente registrar mais detalhes sobre seu sono."),
                        sleepScore = sleepScore,
                        sleepQuality = getSleepQualityLabel(sleepScore),
                        sleepStages = sleepStages,
                        scientificFact = generateScientificFact(session, sleepStages)
                    )
                }
            }
            
            // Calcular pontuação média semanal
            val weeklyScoreAvg = dailyAnalyses.mapNotNull { it.sleepScore }.average()
            val weeklyScore = if (weeklyScoreAvg.isNaN()) null else weeklyScoreAvg.toInt()
            val weeklyQuality = weeklyScore?.let { getSleepQualityLabel(it) }
            
            // Gerar análise geral da semana
            val prompt = buildPrompt(sleepData)
            val response = callAIApi(prompt)
            
            val weeklyAdvice = try {
                gson.fromJson(response, WeeklyAdvice::class.java)
            } catch (e: Exception) {
                WeeklyAdvice(
                    tips = listOf(
                        "Mantenha um horário regular para dormir e acordar.", 
                        "Reduza a exposição à luz azul antes de dormir."
                    ),
                    warnings = emptyList(),
                    positiveReinforcement = "Continue registrando seu sono para análises mais precisas.",
                    weeklyTrend = "Dados insuficientes para determinar tendência"
                )
            }
            
            // Gerar fato científico semanal
            val weeklyScientificFact = generateWeeklyScientificFact(weekSessions)
            
            // Combinar análise semanal com análises diárias
            SleepAdvice(
                mainAdvice = weeklyAdvice.tips.firstOrNull() ?: "Mantenha um horário regular para dormir e acordar.",
                supportingFacts = weeklyAdvice.tips.drop(1),
                customRecommendations = weeklyAdvice.warnings,
                qualityTrend = weeklyAdvice.weeklyTrend ?: detectSleepTrend(weekSessions)?.trendDescription,
                consistencyScore = null
            )
        } catch (e: Exception) {
            SleepAdvice(
                mainAdvice = "Mantenha um horário regular para dormir e acordar.",
                supportingFacts = listOf("Crie um ambiente propício para o sono no seu quarto.", 
                    "O sono REM é essencial para a consolidação da memória e regulação emocional."),
                customRecommendations = emptyList(),
                qualityTrend = "Continue registrando seu sono para análises mais precisas."
            )
        }
    }
    
    /**
     * Constrói um prompt para análise semanal de sono
     */
    private fun buildPrompt(sleepData: List<SleepDataDto>): String {
        val jsonData = gson.toJson(sleepData)
        return """
            Você é um especialista em medicina do sono e análise de dados de saúde. Analise detalhadamente os seguintes dados de sono da última semana e forneça:
            
            1. 3 dicas ALTAMENTE PERSONALIZADAS E ESPECÍFICAS para melhorar o sono baseadas nos padrões observados nos dados. 
               Mencione explicitamente os valores e padrões que você identificou nos dados (horários, duração, eficiência, estágios, etc).
               Suas dicas devem ser acionáveis e baseadas em evidências científicas.
            
            2. Avisos sobre padrões preocupantes específicos que você identificou nos dados (se houver).
               Mencione explicitamente os valores e padrões que você identificou como preocupantes.
            
            3. Reforço positivo detalhado se houver melhora ou bons padrões, citando especificamente quais métricas melhoraram.
            
            4. Tendência semanal do sono (melhorando, piorando ou estável) com detalhes quantitativos sobre as mudanças observadas.
            
            Dados:
            $jsonData
            
            Responda em formato JSON com as seguintes chaves:
            {
                "tips": ["dica1", "dica2", "dica3"],
                "warnings": ["aviso1", "aviso2", ...],
                "positiveReinforcement": "mensagem de reforço positivo ou null",
                "weeklyTrend": "descrição da tendência semanal"
            }
            
            IMPORTANTE: 
            - Seja EXTREMAMENTE específico e prático nas dicas, referenciando os dados reais do usuário.
            - Evite completamente recomendações genéricas que poderiam se aplicar a qualquer pessoa.
            - Cada dica deve mencionar explicitamente algum valor ou padrão observado nos dados do usuário.
            - Inclua números e percentuais específicos quando possível.
        """.trimIndent()
    }
    
    /**
     * Constrói um prompt para análise diária de sono
     */
    private fun buildDailyPrompt(sleepData: SleepDataDto): String {
        val jsonData = gson.toJson(sleepData)
        return """
            Você é um especialista em medicina do sono e análise de dados de saúde. Analise detalhadamente os seguintes dados de uma noite de sono:
            $jsonData
            
            Forneça:
            1. Uma análise detalhada e personalizada desta noite de sono, mencionando explicitamente os valores e padrões observados nos dados.
            2. 2-3 recomendações altamente personalizadas baseadas exclusivamente nesta noite, referenciando diretamente os valores e padrões observados.
            
            Considere e mencione explicitamente:
            - Eficiência do sono (ideal > 85%) - mencione o valor exato observado
            - Distribuição dos estágios do sono (ideal: Profundo > 20%, REM > 20%, Leve ~55%) - mencione os percentuais exatos observados
            - Número de despertares durante a noite - mencione o número exato observado
            - Duração total do sono (ideal: 7-9 horas) - mencione a duração exata observada
            - Horários de dormir e acordar - mencione os horários exatos observados
            
            Responda em formato JSON com as seguintes chaves:
            {
                "analysis": "Análise detalhada e personalizada desta noite",
                "recommendations": ["recomendação1", "recomendação2", "recomendação3"]
            }
            
            IMPORTANTE:
            - Seja EXTREMAMENTE específico e prático nas recomendações, referenciando os dados reais do usuário.
            - Evite completamente recomendações genéricas que poderiam se aplicar a qualquer pessoa.
            - Cada recomendação deve mencionar explicitamente algum valor ou padrão observado nos dados do usuário.
            - Inclua números e percentuais específicos quando possível.
        """.trimIndent()
    }
    
    /**
     * Simula frequência cardíaca durante o sono
     */
    private fun calculateHeartRate(session: SleepSession): Int {
        // Frequência cardíaca média durante o sono: tipicamente entre 50-70 bpm
        val baseAverage = 60
        
        // Ajustes baseados na eficiência do sono
        val efficiencyFactor = ((session.efficiency - 75) / 10).toInt()
        
        // Valor final
        return (baseAverage - efficiencyFactor).coerceIn(50, 80)
    }
    
    /**
     * Cria detalhes dos estágios de sono a partir de uma sessão.
     * Prioriza: Algoritmo customizado -> Dados do Health Connect -> Simulação.
     */
    private fun createSleepStagesDetail(session: SleepSession): SleepStagesDetail {
        var processedStages: List<SleepStage> = emptyList()
        var sourceHint: SleepSource = SleepSource.SIMULATION // Default para simulação
        var isActuallyEstimated = true

        // 1. Tentar com o algoritmo customizado
        val customStages = calculateCustomSleepStagesUseCase(session.startTime, session.endTime, session.heartRateSamples)
        if (customStages.isNotEmpty()) {
            processedStages = customStages
            sourceHint = SleepSource.MANUAL
            isActuallyEstimated = false // Calculado, não estimado por % fixas
        } else {
            // 2. Se o customizado falhar, tentar usar os estágios do Health Connect (se houver)
            if (session.stages.isNotEmpty()) {
                processedStages = session.stages // Estes já têm a source de SleepSource.HEALTH_CONNECT
                sourceHint = processedStages.firstOrNull()?.source ?: SleepSource.HEALTH_CONNECT
                isActuallyEstimated = false // Dados reais do HC
            }
            // 3. Se ambos falharem, recorrer à simulação (processedStages continua emptyList)
            // A lógica de simulação será acionada abaixo se processedStages estiver vazia
        }

        val totalMinutes = session.duration.toMinutes().toDouble()
        
        if (processedStages.isNotEmpty()) {
            // Usa o SleepMetrics para calcular as porcentagens de cada estágio
            val stagePercentages = SleepMetrics.calculateAllStagePercentages(processedStages)
            
            // Calcula os minutos de cada estágio
            val stageDurations = SleepMetrics.calculateTimeByStage(processedStages)
            
            val lightMinutes = stageDurations[SleepStageType.LIGHT]?.toMinutes() ?: 0L
            val deepMinutes = stageDurations[SleepStageType.DEEP]?.toMinutes() ?: 0L
            val remMinutes = stageDurations[SleepStageType.REM]?.toMinutes() ?: 0L
            val awakeMinutes = stageDurations[SleepStageType.AWAKE]?.toMinutes() ?: 0L
            
            val lightPercentage = stagePercentages[SleepStageType.LIGHT] ?: 0.0
            val deepPercentage = stagePercentages[SleepStageType.DEEP] ?: 0.0
            val remPercentage = stagePercentages[SleepStageType.REM] ?: 0.0
            val awakePercentage = stagePercentages[SleepStageType.AWAKE] ?: 0.0
            
            return SleepStagesDetail(
                light = formatDuration(Duration.ofMinutes(lightMinutes)),
                deep = formatDuration(Duration.ofMinutes(deepMinutes)),
                rem = formatDuration(Duration.ofMinutes(remMinutes)),
                awake = formatDuration(Duration.ofMinutes(awakeMinutes)),
                lightPercentage = lightPercentage,
                deepPercentage = deepPercentage,
                remPercentage = remPercentage,
                awakePercentage = awakePercentage,
                isEstimated = isActuallyEstimated, // Baseado se foi simulado ou não
                sourceHint = sourceHint
            )
        } else {
            // Simulação se nenhuma outra fonte de estágios estiver disponível
            val lightSimulatedMinutes = (totalMinutes * 0.55).toLong()
            val deepSimulatedMinutes = (totalMinutes * 0.20).toLong()
            val remSimulatedMinutes = (totalMinutes * 0.20).toLong()
            val awakeSimulatedMinutes = (totalMinutes * 0.05).coerceAtLeast(0.0).toLong()
            
            return SleepStagesDetail(
                light = formatDuration(Duration.ofMinutes(lightSimulatedMinutes)),
                deep = formatDuration(Duration.ofMinutes(deepSimulatedMinutes)),
                rem = formatDuration(Duration.ofMinutes(remSimulatedMinutes)),
                awake = formatDuration(Duration.ofMinutes(awakeSimulatedMinutes)),
                lightPercentage = 55.0,
                deepPercentage = 20.0,
                remPercentage = 20.0,
                awakePercentage = 5.0,
                isEstimated = true, // Definitivamente estimado aqui
                sourceHint = SleepSource.SIMULATION // Changed from "Simulated_Fallback"
            )
        }
    }
    
    /**
     * Formata uma duração em formato legível
     */
    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        return if (hours > 0) {
            "${hours}h${minutes}min"
        } else {
            "${minutes}min"
        }
    }
    
    /**
     * Calcula a pontuação de qualidade do sono (0-100)
     * Esta função agora usará os dados de session.deepSleepPercentage, etc.,
     * que por sua vez são derivados de createSleepStagesDetail (customizado, HC ou simulado).
     */
    private fun calculateSleepScore(session: SleepSession, stagesDetail: SleepStagesDetail): Int {
        var score = session.efficiency // A eficiência já está no SleepSession, calculada pelo repositório ou HC.
                                      // Se não, precisaria ser recalculada aqui com base nos estágios.
                                      // NOTA: A eficiência no SleepSession (0.0-100.0) é calculada no HealthConnectRepository ou precisa ser calculada no SleepAIService
                                      // se os estágios mudarem.
                                      // Por agora, vamos assumir que session.efficiency está correta ou é um bom ponto de partida.

        // Ajustes baseados nos estágios do sono (usando stagesDetail que reflete a melhor fonte de estágios)
        if (stagesDetail.deepPercentage > 20) score += 5 else if (stagesDetail.deepPercentage < 15) score -= 5
        if (stagesDetail.remPercentage > 20) score += 5 else if (stagesDetail.remPercentage < 15) score -= 5

        // Ajustes baseados na duração
        val hours = session.duration.toHours()
        when {
            hours < 6 -> score -= 10
            hours < 7 -> score -= 5
            hours > 9 -> score -= 5 // Dormir demais também pode não ser ideal
        }

        // Ajustes baseados em despertares
        // session.wakeDuringNightCount vem do SleepRepositoryImpl (calculado a partir dos estágios do HC ou do manual)
        // Se os estágios customizados forem diferentes, podemos recontar os despertares aqui.
        // Vamos usar a contagem de despertares derivada dos 'processedStages' em createSleepStagesDetail.
        // Se sourceHint for CustomAlgorithm ou HealthConnect, os estágios são mais confiáveis para isso.
        val wakeCountToUse = if (stagesDetail.sourceHint == SleepSource.MANUAL || stagesDetail.sourceHint == SleepSource.HEALTH_CONNECT) {
             // Tenta derivar despertares da duração 'awake'. Isso é uma heurística.
             // Uma contagem de 'AWAKE stages' seria mais precisa se o algoritmo customizado os gerar.
            val awakeDurationMinutes = stagesDetail.awakeDuration.toMinutes()
            if (awakeDurationMinutes > 0 && stagesDetail.awakePercentage > 2.0) { // Evitar contar despertares se o tempo acordado for mínimo
                 (awakeDurationMinutes / 5).toInt().coerceAtLeast(1) // Pelo menos 1 se houver tempo acordado significativo
            } else {
                0
            }
        } else {
            session.wakeDuringNightCount // Usa o que já veio (HC) ou foi estimado na simulação (geralmente 0 ou baixo)
        }

        when (wakeCountToUse) {
            0 -> score += 5
            // 1 despertar é normal, sem bônus nem penalidade
            2 -> score -= 5
            3 -> score -= 10
            else -> if (wakeCountToUse > 3) score -= 15 // Penalidade maior para muitos despertares
        }
        
        return score.toInt().coerceIn(0, 100)
    }

    /**
     * Obtém a classificação verbal da qualidade do sono
     * @deprecated Use SleepMetrics.getSleepQualityLabel() em vez disso
     */
    @Deprecated("Use SleepMetrics.getSleepQualityLabel() instead")
    private fun getSleepQualityLabel(score: Int): String {
        return SleepMetrics.getSleepQualityLabel(score)
    }

    /**
     * Gera um fato científico personalizado baseado nos dados de sono
     */
    private fun generateScientificFact(session: SleepSession, sleepStages: SleepStagesDetail): String {
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
        
        return when {
            sleepStages.remPercentage > 25 && sleepStages.sourceHint != SleepSource.SIMULATION -> 
                "Você teve ${sleepStages.rem} de sono REM (${sleepStages.remPercentage.toInt()}%), acima da média. O sono REM é essencial para a consolidação da memória e criatividade."
            sleepStages.deepPercentage > 25 && sleepStages.sourceHint != SleepSource.SIMULATION -> 
                "Você teve ${sleepStages.deep} de sono profundo (${sleepStages.deepPercentage.toInt()}%), acima da média. O sono profundo é quando ocorre a maior parte da recuperação física."
            sleepStages.deepPercentage < 15 && sleepStages.sourceHint != SleepSource.SIMULATION -> 
                "Você teve apenas ${sleepStages.deep} de sono profundo (${sleepStages.deepPercentage.toInt()}%). O sono profundo é essencial para a recuperação física e imunidade."
            sleepStages.remPercentage < 15 && sleepStages.sourceHint != SleepSource.SIMULATION -> 
                "Você teve apenas ${sleepStages.rem} de sono REM (${sleepStages.remPercentage.toInt()}%). O sono REM é importante para a saúde mental e processamento emocional."
            session.duration.toHours() > 9 -> 
                "Você dormiu mais de 9 horas. Embora o sono seja importante, dormir demais regularmente pode estar associado a problemas de saúde."
            session.duration.toHours() < 6 -> 
                "Você dormiu menos de 6 horas. A privação crônica de sono pode afetar negativamente a memória, humor e imunidade."
            else -> facts.random()
        }
    }

    /**
     * Gera um fato científico semanal baseado nos dados de sono
     */
    private fun generateWeeklyScientificFact(sessions: List<SleepSession>): String {
        if (sessions.isEmpty()) return "Durma bem para ter uma boa semana!"

        val avgHours = sessions.map { it.duration.toHours() }.average()
        val avgDeepPercentage = sessions.map { createSleepStagesDetail(it).deepPercentage }.average()
        val avgRemPercentage = sessions.map { createSleepStagesDetail(it).remPercentage }.average()

        return when {
            avgDeepPercentage < 15 -> "Nesta semana, seu sono profundo ficou abaixo de 15% em média. Tente melhorar a higiene do sono para noites mais restauradoras."
            avgRemPercentage < 15 -> "Seu sono REM ficou abaixo de 15% em média esta semana. O REM é crucial para o aprendizado e memória."
            avgHours < 6.5 -> "Você dormiu em média menos de 6.5 horas por noite esta semana. Priorize o descanso para mais energia e foco."
            sessions.any { it.duration.toHours() < 5 } -> "Houve noites esta semana com menos de 5 horas de sono. Isso pode impactar significativamente seu bem-estar."
            else -> "Manter uma rotina de sono consistente ajuda a regular seu relógio biológico, melhorando a qualidade do seu descanso."
        }
    }

    /**
     * Detecta a tendência de sono ao longo de um período
     */
    private fun detectSleepTrend(sessions: List<SleepSession>): SleepTrend? {
        if (sessions.size < 3) return null // Precisa de pelo menos 3 noites para uma tendência mínima

        val sortedSessions = sessions.sortedBy { it.startTime }
        val scores = sortedSessions.map { calculateSleepScore(it, createSleepStagesDetail(it)) }
        
        // Simples análise de tendência: compara a média da primeira metade com a segunda metade
        val firstHalfScores = scores.take(scores.size / 2)
        val secondHalfScores = scores.drop(scores.size / 2)

        if (firstHalfScores.isEmpty() || secondHalfScores.isEmpty()) return null

        val avgFirstHalf = firstHalfScores.average()
        val avgSecondHalf = secondHalfScores.average()

        val trendDescription = when {
            avgSecondHalf > avgFirstHalf + 5 -> "Sua qualidade de sono está melhorando ao longo da semana."
            avgSecondHalf < avgFirstHalf - 5 -> "Sua qualidade de sono parece estar diminuindo ao longo da semana."
            else -> "Sua qualidade de sono está estável esta semana."
        }
        val consistencyScore = (100 - (abs(avgFirstHalf - avgSecondHalf) * 2)).coerceIn(50.0, 100.0) // Simples medida de consistência

        return SleepTrend(trendDescription, consistencyScore.toInt())
    }

    /**
     * Gera recomendação de horário ideal para sonecas
     * Lógica simplificada: 6-7 horas após o despertar médio, se o sono noturno for < 7h
     */
    private fun generateRecommendedNapTime(): String {
        // Esta é uma lógica muito simplista e pode ser bastante aprimorada
        return "Evite sonecas longas ou muito tarde para não prejudicar seu sono noturno."
    }

    /**
     * Chamada à API de IA para análise de dados de sono
     * Implementação local que simula respostas da IA
     */
    private suspend fun callAIApi(prompt: String): String {
        // Simulação de resposta da API
        // Baseado no prompt, tenta dar uma resposta minimamente coerente
        if (prompt.contains("Análise da noite")) {
            return "{\"analysis\": \"Esta foi uma noite razoável. Tente relaxar mais antes de dormir.\", \"recommendations\": [\"Evite cafeína à noite\", \"Crie um ambiente escuro e silencioso\"]}"
        } else if (prompt.contains("Análise semanal")) {
            return "{\"summary\": \"Sua semana de sono teve altos e baixos. Mantenha a consistência.\", \"actionable_tips\": [\"Defina um horário regular para dormir e acordar\", \"Pratique atividades relaxantes antes de deitar\"]}"
        }
        return "{\"error\": \"Não foi possível processar a solicitação.\"}"
    }
}

/**
 * DTO com dados detalhados de sono para análise pela IA
 */
data class SleepDataDto(
    @SerializedName("date") val date: String,
    @SerializedName("totalSleep") val totalSleep: String,
    @SerializedName("deepSleepPercentage") val deepSleepPercentage: Double,
    @SerializedName("remSleepPercentage") val remSleepPercentage: Double,
    @SerializedName("lightSleepPercentage") val lightSleepPercentage: Double,
    @SerializedName("wakeCount") val wakeCount: Int,
    @SerializedName("efficiency") val efficiency: Double,
    @SerializedName("heartRateAvg") val heartRateAvg: Int? = null,
    @SerializedName("isDataEstimated") val isDataEstimated: Boolean = false
)

/**
 * Resposta da análise semanal de sono
 */
data class WeeklyAdvice(
    val tips: List<String>,
    val warnings: List<String>,
    val positiveReinforcement: String?,
    val weeklyTrend: String? = null
)

/**
 * Análise detalhada de uma noite de sono
 */
data class DailyAnalysis(
    val date: String,
    val analysis: String,
    val recommendations: List<String>,
    val sleepScore: Int? = null,
    val sleepQuality: String? = null,
    val sleepStages: SleepStagesDetail? = null,
    val scientificFact: String? = null
)

/**
 * Detalhes sobre os estágios do sono
 */
data class SleepStagesDetail(
    val light: String,
    val deep: String,
    val rem: String,
    val awake: String,
    val lightPercentage: Double,
    val deepPercentage: Double,
    val remPercentage: Double,
    val awakePercentage: Double,
    val isEstimated: Boolean = false,
    val sourceHint: SleepSource? = null
) {
    // Propriedade para calcular a duração de 'awake' a partir da string formatada.
    // Idealmente, SleepStagesDetail armazenaria Duration diretamente para evitar parsing.
    val awakeDuration: Duration by lazy {
        try {
            var duration = Duration.ZERO
            if (awake.contains("h")) {
                val parts = awake.split("h")
                duration = duration.plusHours(parts[0].toLong())
                if (parts.size > 1 && parts[1].contains("min")) {
                    duration = duration.plusMinutes(parts[1].replace("min", "").toLong())
                }
            } else if (awake.contains("min")) {
                duration = duration.plusMinutes(awake.replace("min", "").toLong())
            }
            duration
        } catch (e: NumberFormatException) {
            // Log.e(TAG, "Erro ao parsear awakeDuration de '$awake'", e)
            Duration.ZERO
        }
    }
}

/**
 * Resposta completa da análise de sono com dicas e análises diárias
 */
data class SleepAdviceResponse(
    val tips: List<String>,
    val warnings: List<String>,
    val positiveReinforcement: String?,
    val dailyAnalysis: List<DailyAnalysis>,
    val weeklyScore: Int? = null,
    val weeklyQuality: String? = null,
    val weeklyTrend: String? = null,
    val scientificFact: String? = null,
    val recommendedNapTime: String? = null
)

/**
 * Representa a tendência de sono ao longo de um período
 */
data class SleepTrend(
    val trendDescription: String,
    val consistencyScore: Int // 0-100, quão consistente é o sono
)

/**
 * Gera um fato científico personalizado baseado nos dados de sono
 */
private fun generateScientificFact(session: SleepSession, sleepStages: SleepStagesDetail): String {
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
    
    // Correção do uso de SleepSource como String
    if (session.source == SleepSource.MANUAL) {
        return "Scientific fact related to manually tracked sleep: Consistent tracking helps identify patterns!"
    } else if (session.source == SleepSource.HEALTH_CONNECT) {
        return "Scientific fact related to Health Connect data: Detailed sleep stages help understand sleep quality."
    }
    
    // Restante da lógica para gerar o fato científico
    return when {
        sleepStages.remPercentage > 25 && sleepStages.sourceHint != SleepSource.SIMULATION -> 
            "Você teve ${sleepStages.rem} de sono REM (${sleepStages.remPercentage.toInt()}%), acima da média. O sono REM é essencial para a consolidação da memória e criatividade."
        sleepStages.deepPercentage > 25 && sleepStages.sourceHint != SleepSource.SIMULATION -> 
            "Você teve ${sleepStages.deep} de sono profundo (${sleepStages.deepPercentage.toInt()}%), acima da média. O sono profundo é quando ocorre a maior parte da recuperação física."
        sleepStages.deepPercentage < 15 && sleepStages.sourceHint != SleepSource.SIMULATION -> 
            "Você teve apenas ${sleepStages.deep} de sono profundo (${sleepStages.deepPercentage.toInt()}%). O sono profundo é essencial para a recuperação física e imunidade."
        sleepStages.remPercentage < 15 && sleepStages.sourceHint != SleepSource.SIMULATION -> 
            "Você teve apenas ${sleepStages.rem} de sono REM (${sleepStages.remPercentage.toInt()}%). O sono REM é importante para a saúde mental e processamento emocional."
        session.duration.toHours() > 9 -> 
            "Você dormiu mais de 9 horas. Embora o sono seja importante, dormir demais regularmente pode estar associado a problemas de saúde."
        session.duration.toHours() < 6 -> 
            "Você dormiu menos de 6 horas. A privação crônica de sono pode afetar negativamente a memória, humor e imunidade."
        else -> facts.random()
    }
}
