package com.example.sleepadvisor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepadvisor.domain.model.NapAnalysis
import com.example.sleepadvisor.domain.model.SleepQualityAnalysis
import com.example.sleepadvisor.domain.model.SleepRecommendations
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepTrendAnalysis
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
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * ViewModel responsável por gerenciar análises avançadas de sono.
 * Utiliza múltiplos casos de uso para fornecer análises detalhadas e recomendações personalizadas.
 */
@HiltViewModel
class SleepAnalysisViewModel @Inject constructor(
    private val getSleepSessionsUseCase: GetSleepSessionsUseCase,
    private val analyzeSleepQualityUseCase: AnalyzeSleepQualityUseCase,
    private val analyzeSleepTrendsUseCase: AnalyzeSleepTrendsUseCase,
    private val detectNapsUseCase: DetectNapsUseCase,
    private val generateSleepRecommendationsUseCase: GenerateSleepRecommendationsUseCase,
    private val getSleepSessionDetailsUseCase: GetSleepSessionDetailsUseCase
) : ViewModel() {

    // Estado da UI observável pela camada de apresentação
    private val _uiState = MutableStateFlow(SleepAnalysisUiState())
    val uiState: StateFlow<SleepAnalysisUiState> = _uiState

    init {
        loadData()
    }

    /**
     * Carrega todos os dados de sono e realiza análises avançadas.
     */
    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
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
                                
                // Gerar recomendações específicas para esta sessão usando o caso de uso
                val sessionSpecificRecommendations = generateSleepRecommendationsUseCase.invokeForSingleSession(
                    completeSession,
                    qualityAnalysis
                )
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        selectedSession = completeSession,
                        selectedSessionAnalysis = qualityAnalysis,
                        sessionSpecificRecommendations = sessionSpecificRecommendations,
                        error = null // Limpar qualquer erro anterior
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Erro ao analisar sessão: ${e.message}"
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
}

/**
 * Estado da UI para a tela de análise de sono.
 * Contém todos os dados necessários para renderizar a interface.
 */
data class SleepAnalysisUiState(
    val isLoading: Boolean = false,
    val emptySessions: Boolean = false,
    val sessions: List<SleepSession> = emptyList(),
    val lastSession: SleepSession? = null,
    val qualityAnalysis: SleepQualityAnalysis? = null,
    val trendAnalysis: SleepTrendAnalysis? = null,
    val napAnalyses: List<NapAnalysis> = emptyList(),
    val recommendations: SleepRecommendations? = null,
    val selectedSession: SleepSession? = null,
    val selectedSessionAnalysis: SleepQualityAnalysis? = null,
    val sessionSpecificRecommendations: List<String> = emptyList(),
    val showNapsOnly: Boolean = false,
    val analysisPeriodStart: ZonedDateTime = ZonedDateTime.now().minusDays(7),
    val analysisPeriodEnd: ZonedDateTime = ZonedDateTime.now(),
    val error: String? = null
)
