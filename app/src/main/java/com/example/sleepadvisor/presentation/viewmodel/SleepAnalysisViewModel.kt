package com.example.sleepadvisor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepadvisor.domain.model.NapAnalysis
import com.example.sleepadvisor.domain.model.SleepAdvice
import com.example.sleepadvisor.domain.model.SleepQualityAnalysis
import com.example.sleepadvisor.domain.model.SleepRecommendations
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepMetrics
import com.example.sleepadvisor.domain.model.SleepSource
import com.example.sleepadvisor.domain.model.SleepStage
import com.example.sleepadvisor.domain.model.SleepStageType
import com.example.sleepadvisor.domain.model.SleepTrendAnalysis
import com.example.sleepadvisor.domain.repository.SleepAnalysisRepository
import com.example.sleepadvisor.domain.usecase.AnalyzeSleepQualityUseCase
import com.example.sleepadvisor.domain.usecase.AnalyzeSleepTrendsUseCase
import com.example.sleepadvisor.domain.usecase.DetectNapsUseCase
import com.example.sleepadvisor.domain.usecase.GenerateSleepRecommendationsUseCase
import com.example.sleepadvisor.domain.usecase.GetSleepSessionDetailsUseCase
import com.example.sleepadvisor.domain.usecase.GetSleepSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * ViewModel responsável por gerenciar análises avançadas de sono.
 * Utiliza múltiplos casos de uso para fornecer análises detalhadas e recomendações personalizadas.
 */
@HiltViewModel
class SleepAnalysisViewModel @Inject constructor(
    @Suppress("UNUSED_PARAMETER") private val getSleepSessionsUseCase: GetSleepSessionsUseCase,
    private val analyzeSleepQualityUseCase: AnalyzeSleepQualityUseCase,
    private val analyzeSleepTrendsUseCase: AnalyzeSleepTrendsUseCase,
    @Suppress("UNUSED_PARAMETER") private val detectNapsUseCase: DetectNapsUseCase,
    private val generateSleepRecommendationsUseCase: GenerateSleepRecommendationsUseCase,
    @Suppress("UNUSED_PARAMETER") private val getSleepSessionDetailsUseCase: GetSleepSessionDetailsUseCase,
    @Suppress("UNUSED_PARAMETER") private val sleepAnalysisRepository: SleepAnalysisRepository
) : ViewModel() {

    // Estado da UI observável pela camada de apresentação
    private val _uiState = MutableStateFlow(SleepAnalysisUiState())
    val uiState: StateFlow<SleepAnalysisUiState> = _uiState

    init {
        loadData()
        getAIRecommendations()
    }
    
    /**
     * Obtém recomendações personalizadas de IA baseadas na última sessão de sono
     */
    private fun getAIRecommendations() {
        viewModelScope.launch {
            try {
                // Aguardar o carregamento dos dados
                val lastSession = _uiState.value.lastSession
                
                if (lastSession != null) {
                    // Validar dados da sessão
                    if (isValidSleepSession(lastSession)) {
                        android.util.Log.d("SleepAnalysisVM", "Sessão válida encontrada. ID: ${lastSession.id}, Data: ${lastSession.startTime}")
                        
                        // Obter recomendações de IA para a última sessão
                        android.util.Log.d("SleepAnalysisVM", "Solicitando recomendações de IA para sessão: ${lastSession.id}")
                        android.util.Log.d("SleepAnalysisVM", "Dados da sessão: " +
                                "Duração: ${lastSession.duration.toHours()}h${lastSession.duration.toMinutesPart()}min, " +
                                "Eficiência: ${lastSession.efficiency}%, " +
                                "Sono profundo: ${lastSession.deepSleepPercentage}%, " +
                                "Sono REM: ${lastSession.remSleepPercentage}%")
                        
                        val aiAdvice = sleepAnalysisRepository.analyzeSleepSession(lastSession)
                        
                        // Validar recomendações recebidas
                        if (aiAdvice.customRecommendations.isNotEmpty()) {
                            android.util.Log.d("SleepAnalysisVM", "Recomendações de IA recebidas: ${aiAdvice.mainAdvice}")
                            android.util.Log.d("SleepAnalysisVM", "Total de recomendações: ${aiAdvice.customRecommendations.size}")
                            
                            // Atualizar o estado da UI com as recomendações de IA
                            _uiState.update { currentState ->
                                currentState.copy(
                                    aiAdvice = aiAdvice
                                )
                            }
                            android.util.Log.d("SleepAnalysisVM", "Recomendações de IA atualizadas na UI")
                        } else {
                            android.util.Log.w("SleepAnalysisVM", "Nenhuma recomendação personalizada retornada pela IA")
                            _uiState.update { it.copy(error = "Não foi possível gerar recomendações personalizadas") }
                        }
                    } else {
                        android.util.Log.w("SleepAnalysisVM", "Sessão de sono inválida para análise de IA")
                        _uiState.update { it.copy(error = "Dados de sono insuficientes para análise") }
                    }
                } else {
                    android.util.Log.d("SleepAnalysisVM", "Não foi possível obter recomendações de IA: sessão não disponível")
                    _uiState.update { it.copy(error = "Nenhuma sessão de sono recente encontrada") }
                }
            } catch (e: Exception) {
                val errorMsg = "Erro ao obter recomendações de IA: ${e.message}"
                android.util.Log.e("SleepAnalysisVM", errorMsg, e)
                _uiState.update { it.copy(error = errorMsg) }
            }
        }
    }
    
    /**
     * Valida se uma sessão de sono contém dados suficientes para análise
     */
    private fun isValidSleepSession(session: SleepSession): Boolean {
        // Verifica se a eficiência está dentro da faixa válida (0-100)
        val isEfficiencyValid = session.efficiency in 0.0..100.0
        
        return session.duration.toHours() > 0 && // Pelo menos 1 hora de sono
               isEfficiencyValid && // Eficiência entre 0 e 100
               session.deepSleepPercentage >= 0 && // Porcentagens válidas
               session.remSleepPercentage >= 0 &&
               session.lightSleepPercentage <= 100.0 && // Não pode ser maior que 100%
               session.deepSleepPercentage <= 100.0 && // Não pode ser maior que 100%
               session.remSleepPercentage <= 100.0 && // Não pode ser maior que 100%
               session.wakeDuringNightCount >= 0
    }

    /**
     * Carrega todos os dados de sono e realiza análises avançadas.
     */
    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Obter sessões de sono consolidadas dos últimos 7 dias
                android.util.Log.d("SleepAnalysisVM", "Iniciando carregamento de dados de sono")
                getSleepSessionsUseCase.getConsolidatedSleepSessions()
                    .catch { e ->
                        android.util.Log.e("SleepAnalysisVM", "Erro ao carregar dados: ${e.message}", e)
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = "Erro ao carregar dados: ${e.message}"
                            )
                        }
                    }
                    .collectLatest { sessions ->
                        android.util.Log.d("SleepAnalysisVM", "Sessões carregadas: ${sessions.size}")
                        if (sessions.isEmpty()) {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    emptySessions = true
                                )
                            }
                            return@collectLatest
                        }
                        
                        // Ordenar sessões por data (mais recente primeiro)
                        val sortedSessions = sessions.sortedByDescending { it.startTime }
                        
                        // Obter última sessão
                        val lastSession = sortedSessions.firstOrNull()
                        
                        // Realizar análises
                        android.util.Log.d("SleepAnalysisVM", "Iniciando análises de sono")
                        val qualityAnalysis = lastSession?.let { 
                            android.util.Log.d("SleepAnalysisVM", "Analisando qualidade da última sessão: ${it.id}")
                            analyzeSleepQualityUseCase(it) 
                        }
                        android.util.Log.d("SleepAnalysisVM", "Qualidade analisada: ${qualityAnalysis != null}")
                        
                        val trendAnalysis = analyzeSleepTrendsUseCase(sortedSessions)
                        android.util.Log.d("SleepAnalysisVM", "Tendências analisadas: ${trendAnalysis != null}")
                        
                        val napAnalyses = detectNapsUseCase(sortedSessions)
                        android.util.Log.d("SleepAnalysisVM", "Sonecas detectadas: ${napAnalyses.size}")
                        
                        // Gerar recomendações personalizadas
                        android.util.Log.d("SleepAnalysisVM", "Gerando recomendações personalizadas")
                        val recommendations = generateSleepRecommendationsUseCase(
                            lastSession,
                            qualityAnalysis,
                            trendAnalysis,
                            napAnalyses
                        )
                        android.util.Log.d("SleepAnalysisVM", "Recomendações geradas: ${recommendations != null}, prioridades: ${recommendations?.priorityRecommendations?.size ?: 0}")
                        
                        // Atualizar estado da UI
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                emptySessions = false,
                                sessions = sortedSessions,
                                lastSession = lastSession,
                                qualityAnalysis = qualityAnalysis,
                                trendAnalysis = trendAnalysis,
                                napAnalyses = napAnalyses,
                                recommendations = recommendations,
                                error = null // Limpar erro ao carregar com sucesso
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Erro inesperado: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Testa o modelo de IA com dados de exemplo para verificar se está funcionando corretamente
     */
    fun testModelWithSampleData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                // Criar uma sessão de exemplo para teste
                val now = ZonedDateTime.now()
                val startTime = now.minusHours(8).toInstant()
                val endTime = now.toInstant()
                
                // Criar estágios de sono de exemplo
                val stages = listOf(
                    SleepStage(
                        startTime = startTime,
                        endTime = startTime.plusSeconds(3600), // 1 hora de sono profundo
                        type = SleepStageType.DEEP,
                        source = SleepSource.SIMULATION
                    ),
                    SleepStage(
                        startTime = startTime.plusSeconds(3600),
                        endTime = startTime.plusSeconds(5400), // 30 minutos de REM
                        type = SleepStageType.REM,
                        source = SleepSource.SIMULATION
                    ),
                    SleepStage(
                        startTime = startTime.plusSeconds(5400),
                        endTime = endTime, // Restante do tempo em sono leve
                        type = SleepStageType.LIGHT,
                        source = SleepSource.SIMULATION
                    )
                )
                
                val sampleSession = SleepSession(
                    id = "test-session-001",
                    startTime = startTime,
                    endTime = endTime,
                    title = "Sessão de Teste",
                    source = SleepSource.SIMULATION,
                    stages = stages,
                    wakeDuringNightCount = 2,
                    efficiency = 90.0,
                    deepSleepPercentage = 20.0,
                    remSleepPercentage = 25.0,
                    lightSleepPercentage = 50.0
                )
                
                android.util.Log.d("SleepAnalysisVM", "Testando modelo com dados de exemplo...")
                
                // Validar a sessão de exemplo
                if (isValidSleepSession(sampleSession)) {
                    android.util.Log.d("SleepAnalysisVM", "Sessão de exemplo válida. Iniciando análise...")
                    
                    // Analisar a sessão de exemplo
                    val aiAdvice = sleepAnalysisRepository.analyzeSleepSession(sampleSession)
                    
                    if (aiAdvice.customRecommendations.isNotEmpty()) {
                        android.util.Log.d("SleepAnalysisVM", "Teste do modelo bem-sucedido!")
                        android.util.Log.d("SleepAnalysisVM", "Recomendação principal: ${aiAdvice.mainAdvice}")
                        android.util.Log.d("SleepAnalysisVM", "Total de recomendações: ${aiAdvice.customRecommendations.size}")
                        
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                testResult = "Teste bem-sucedido! ${aiAdvice.customRecommendations.size} recomendações geradas.",
                                testSuccess = true
                            )
                        }
                    } else {
                        val errorMsg = "O modelo foi executado, mas não gerou recomendações"
                        android.util.Log.w("SleepAnalysisVM", errorMsg)
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                testResult = errorMsg,
                                testSuccess = false
                            )
                        }
                    }
                } else {
                    val errorMsg = "Sessão de exemplo inválida para teste"
                    android.util.Log.e("SleepAnalysisVM", errorMsg)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            testResult = errorMsg,
                            testSuccess = false
                        )
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Erro ao testar o modelo: ${e.message}"
                android.util.Log.e("SleepAnalysisVM", errorMsg, e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        testResult = errorMsg,
                        testSuccess = false,
                        error = errorMsg
                    )
                }
            }
        }
    }
    
    /**
     * Atualiza a análise de qualidade para uma sessão específica.
     * Carrega os detalhes completos da sessão e realiza análises detalhadas.
     */
    fun analyzeSession(session: SleepSession) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Carregar a sessão completa usando o caso de uso
                val completeSession = getSleepSessionDetailsUseCase(session.id) ?: session
                
                // Realizar análise de qualidade
                val qualityAnalysis = analyzeSleepQualityUseCase(completeSession)
                
                try {
                    // Gerar recomendações específicas para esta sessão
                    val recommendations = generateSleepRecommendationsUseCase.invokeForSingleSession(
                        completeSession,
                        qualityAnalysis
                    )
                    
                    // Gerar análise detalhada
                    val detailedAnalysis = generateDetailedAnalysis(completeSession)
                    val customRecommendations = generateSleepRecommendations(completeSession)
                    
                    // Atualizar o estado com todas as análises
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            selectedSession = completeSession,
                            selectedSessionAnalysis = qualityAnalysis.copyWithMetrics(detailedAnalysis),
                            sessionSpecificRecommendations = recommendations + customRecommendations
                        )
                    }
                } catch (e: Exception) {
                    // Se houver erro ao gerar recomendações, usar apenas as recomendações padrão
                    val customRecommendations = generateSleepRecommendations(completeSession)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            selectedSession = completeSession,
                            selectedSessionAnalysis = qualityAnalysis,
                            sessionSpecificRecommendations = customRecommendations,
                            error = "Algumas recomendações podem não estar disponíveis: ${e.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Erro ao analisar a sessão: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Limpa a sessão selecionada e suas análises.
     */
    fun clearSelectedSession() {
        _uiState.update {
            it.copy(
                selectedSession = null,
                selectedSessionAnalysis = null,
                sessionSpecificRecommendations = emptyList()
            )
        }
    }
    
    /**
     * Filtra sonecas para exibição.
     */
    fun showNapsOnly(show: Boolean) {
        _uiState.update { it.copy(showNapsOnly = show) }
    }
    
    /**
     * Atualiza o período de análise.
     */
    fun updateAnalysisPeriod(startDate: ZonedDateTime, endDate: ZonedDateTime) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Obter sessões de sono para o período selecionado
                getSleepSessionsUseCase(startDate, endDate)
                    .catch { e ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = "Erro ao carregar dados: ${e.message}"
                            )
                        }
                    }
                    .collectLatest { sessions ->
                        // Usar sessões já consolidadas
                        val consolidatedSessions = sessions.sortedByDescending { it.startTime }
                        
                        // Realizar análises
                        val lastSession = consolidatedSessions.firstOrNull()
                        val qualityAnalysis = lastSession?.let { analyzeSleepQualityUseCase(it) }
                        val trendAnalysis = analyzeSleepTrendsUseCase(consolidatedSessions)
                        val napAnalyses = detectNapsUseCase(consolidatedSessions)
                        
                        // Gerar recomendações
                        val recommendations = generateSleepRecommendationsUseCase(
                            lastSession,
                            qualityAnalysis,
                            trendAnalysis,
                            napAnalyses
                        )
                        
                        // Atualizar estado
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                emptySessions = consolidatedSessions.isEmpty(),
                                sessions = consolidatedSessions,
                                lastSession = lastSession,
                                qualityAnalysis = qualityAnalysis,
                                trendAnalysis = trendAnalysis,
                                napAnalyses = napAnalyses,
                                recommendations = recommendations,
                                analysisPeriodStart = startDate,
                                analysisPeriodEnd = endDate
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Erro inesperado: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Gera uma análise detalhada de uma sessão de sono.
     * @param session Sessão de sono a ser analisada
     * @return Mapa com métricas detalhadas
     */
    private fun generateDetailedAnalysis(session: SleepSession): Map<String, Any> {
        val metrics = mutableMapOf<String, Any>()
        
        // Cálculo de métricas básicas
        metrics["sleepEfficiency"] = calculateSleepEfficiency(session)
        metrics["timeByStage"] = getTimeByStage(session)
        metrics["isBedtimeOptimal"] = isBedtimeOptimal(session)
        metrics["sleepLatency"] = calculateSleepLatency(session) ?: Duration.ZERO
        metrics["remEfficiency"] = calculateREMEfficiency(session)
        metrics["awakenings"] = session.wakeDuringNightCount
        metrics["hasValidStages"] = session.hasValidStages()
        
        return metrics
    }
    
    private fun calculateSleepEfficiency(session: SleepSession): Int {
        return SleepMetrics.calculateSleepEfficiency(session.stages, session.duration)
    }
    
    private fun getTimeByStage(session: SleepSession): Map<SleepStageType, Duration> {
        return SleepMetrics.calculateTimeByStage(session.stages)
    }
    
    private fun isBedtimeOptimal(session: SleepSession): Boolean {
        val localDateTime = session.startTime.atZone(ZoneId.systemDefault()).toLocalDateTime()
        val bedHour = localDateTime.hour
        return bedHour in 20..23
    }
    
    private fun calculateSleepLatency(session: SleepSession): Duration? {
        return SleepMetrics.calculateSleepLatency(session.stages, session.startTime)
    }
    
    private fun calculateREMEfficiency(session: SleepSession): Int {
        if (session.stages.isEmpty()) return 0
        
        val remPercentage = SleepMetrics.calculateStagePercentage(
            stages = session.stages,
            targetStage = SleepStageType.REM
        )
        
        return remPercentage.toInt()
    }
    
    /**
     * Gera recomendações personalizadas com base nos padrões de sono.
     * @param session Sessão de sono a ser analisada
     * @return Lista de recomendações
     */
    private fun generateSleepRecommendations(session: SleepSession): List<String> {
        val recommendations = mutableListOf<String>()
        val metrics = generateDetailedAnalysis(session)
        
        // Recomendações baseadas na eficiência do sono
        val efficiency = metrics["sleepEfficiency"] as? Int ?: 0
        when {
            efficiency < 85 -> recommendations.add("Sua eficiência de sono está baixa (${efficiency}%). Tente criar um ambiente mais propício para o sono.")
            efficiency < 90 -> recommendations.add("Sua eficiência de sono está boa (${efficiency}%), mas pode melhorar.")
            else -> recommendations.add("Ótima eficiência de sono (${efficiency}%)! Continue mantendo esses hábitos.")
        }
        
        // Recomendações baseadas no horário de dormir
        val isOptimal = metrics["isBedtimeOptimal"] as? Boolean ?: false
        if (!isOptimal) {
            recommendations.add("Tente dormir entre 20h e 23h para um sono mais reparador.")
        }
        
        // Recomendações baseadas na latência do sono
        val latency = metrics["sleepLatency"] as? Duration ?: Duration.ZERO
        if (latency > Duration.ofMinutes(30)) {
            recommendations.add("Você está demorando muito para adormecer (${latency.toMinutes()} minutos). Considere práticas de relaxamento antes de dormir.")
        }
        
        // Recomendações baseadas no sono REM
        val remEfficiency = metrics["remEfficiency"] as? Int ?: 0
        when {
            remEfficiency < 15 -> recommendations.add("Seu tempo de sono REM está baixo. Tente dormir mais cedo para aumentar o sono REM.")
            remEfficiency > 30 -> recommendations.add("Seu tempo de sono REM está alto, o que pode indicar privação de sono. Tente dormir mais horas por noite.")
        }
        
        // Recomendações baseadas em despertares noturnos
        val awakenings = metrics["awakenings"] as? Int ?: 0
        if (awakenings > 2) {
            recommendations.add("Você está acordando muitas vezes durante a noite. Evite cafeína e telas antes de dormir.")
        }
        
        return recommendations
    }
}

/**
 * Estado da UI para a tela de análise de sono.
 * Contém todos os dados necessários para renderizar a interface.
 */
data class SleepAnalysisUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val emptySessions: Boolean = false,
    val sessions: List<SleepSession> = emptyList(),
    val lastSession: SleepSession? = null,
    val qualityAnalysis: SleepQualityAnalysis? = null,
    val trendAnalysis: SleepTrendAnalysis? = null,
    val napAnalyses: List<NapAnalysis> = emptyList(),
    val recommendations: SleepRecommendations? = null,
    val aiAdvice: SleepAdvice? = null,
    val testResult: String? = null,
    val testSuccess: Boolean? = null,
    val selectedSession: SleepSession? = null,
    val selectedSessionAnalysis: SleepQualityAnalysis? = null,
    val sessionSpecificRecommendations: List<String> = emptyList(),
    val showNapsOnly: Boolean = false,
    val analysisPeriodStart: ZonedDateTime = ZonedDateTime.now().minusDays(7),
    val analysisPeriodEnd: ZonedDateTime = ZonedDateTime.now()
)
