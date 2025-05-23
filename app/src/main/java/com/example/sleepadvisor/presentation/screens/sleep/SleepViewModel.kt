package com.example.sleepadvisor.presentation.screens.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepStageType
import com.example.sleepadvisor.domain.model.toDailyAnalysis
import com.example.sleepadvisor.domain.model.DailyAnalysis
import com.example.sleepadvisor.domain.usecase.GetSleepSessionsUseCase
import com.example.sleepadvisor.domain.service.SleepAIService
import com.example.sleepadvisor.domain.service.SleepAdvice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.Duration
import java.util.UUID
import javax.inject.Inject
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import com.example.sleepadvisor.domain.repository.SleepRepository
import kotlinx.coroutines.flow.catch
import android.util.Log
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoField

/**
 * ViewModel que gerencia os dados de sono e a interação com a UI
 * Combina dados do Health Connect e entradas manuais
 */
@HiltViewModel
class SleepViewModel @Inject constructor(
    private val getSleepSessionsUseCase: GetSleepSessionsUseCase,
    private val healthConnectClient: HealthConnectClient,
    private val repository: SleepRepository,
    private val sleepAIService: SleepAIService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SleepUiState())
    val uiState: StateFlow<SleepUiState> = _uiState

    val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(androidx.health.connect.client.records.HeartRateRecord::class) // Adicionado para FC
    )

    init {
        checkPermissionsAndLoadData()
    }

    private fun checkPermissionsAndLoadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                if (granted.containsAll(permissions)) {
                    _uiState.update { it.copy(hasPermissions = true) }
                    loadConsolidatedSleepData()
                } else {
                    _uiState.update { it.copy(hasPermissions = false, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao verificar permissões: ${e.message}", isLoading = false) }
            }
        }
    }

    fun onPermissionsResult(grantedPermissions: Set<String>) {
        viewModelScope.launch {
            val allPermissionsGranted = permissions.all { perm ->
                grantedPermissions.contains(perm.toString())
            }
            if (allPermissionsGranted) {
                _uiState.update { it.copy(hasPermissions = true, isLoading = true) }
                loadConsolidatedSleepData()
            } else {
                _uiState.update { it.copy(
                    hasPermissions = false, 
                    isLoading = false,
                    error = "Permissões necessárias não concedidas."
                ) }
            }
        }
    }

    fun onPermissionsGranted() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(hasPermissions = true, isLoading = true) }
                loadSleepSessions()
                loadConsolidatedSleepData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao carregar dados: ${e.message}", isLoading = false) }
            }
        }
    }

    fun onPermissionsDenied() {
        _uiState.update { it.copy(
            hasPermissions = false,
            isLoading = false,
            error = "Permissões necessárias não foram concedidas. Algumas funcionalidades podem não estar disponíveis."
        ) }
    }

    private fun loadConsolidatedSleepData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                getSleepSessionsUseCase.getConsolidatedSleepSessions()
                    .catch { error ->
                        _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar dados consolidados: ${error.message}") }
                    }
                    .collect { consolidatedSessions ->
                        val sessionAnalyses = generateSessionAnalyses(consolidatedSessions)
                        val lastNightSession = consolidatedSessions
                            .filter { isNightSleep(it) }
                            .maxByOrNull { it.endTime }

                        _uiState.update { currentState ->
                            currentState.copy(
                                sleepSessions = consolidatedSessions.sortedByDescending { it.startTime },
                                isLoading = false,
                                lastSession = lastNightSession,
                                error = null,
                                sessionAnalyses = sessionAnalyses
                            )
                        }
                        if (consolidatedSessions.isNotEmpty()) {
                            generateAIAdvice(consolidatedSessions)
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Erro ao carregar dados de sono: ${e.message}") }
            }
        }
    }

    private fun generateSessionAnalyses(sessions: List<SleepSession>): Map<String, DailyAnalysis> {
        return sessions.associate { session ->
            session.id to session.toDailyAnalysis()
        }
    }

    private fun generateAIAdvice(sessions: List<SleepSession>) {
        viewModelScope.launch {
            if (sessions.isEmpty()) {
                _uiState.update { it.copy(sleepAdvice = null, aiAdviceLoading = false) }
                return@launch
            }
            _uiState.update { it.copy(aiAdviceLoading = true) }
            try {
                val advice = sleepAIService.generateSleepAdvice(sessions)
                _uiState.update { it.copy(sleepAdvice = advice, aiAdviceLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao gerar dicas de IA: ${e.message}", aiAdviceLoading = false) }
            }
        }
    }

    fun refreshData() {
        checkPermissionsAndLoadData()
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        // Potencialmente carregar dados específicos para esta data se necessário
    }

    fun onTimeSet(hour: Int, minute: Int, isStartTime: Boolean) {
        val currentSelectedDate = _uiState.value.selectedDate ?: LocalDate.now()
        val time = LocalTime.of(hour, minute)
        val dateTime = LocalDateTime.of(currentSelectedDate, time).atZone(ZoneId.systemDefault())

        if (isStartTime) {
            _uiState.update { it.copy(selectedStartTime = dateTime) }
        } else {
            _uiState.update { it.copy(selectedEndTime = dateTime) }
        }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(manualEntryNotes = notes) }
    }

    fun saveManualSleepSession() {
        viewModelScope.launch {
            val startTimeToSave = _uiState.value.selectedStartTime
            val endTimeToSave = _uiState.value.selectedEndTime
            val notes = _uiState.value.manualEntryNotes

            if (startTimeToSave == null || endTimeToSave == null) {
                _uiState.update { it.copy(error = "Horário de início e fim devem ser definidos.") }
                return@launch
            }

            if (endTimeToSave.isBefore(startTimeToSave) || endTimeToSave == startTimeToSave) {
                _uiState.update { it.copy(error = "Horário de fim deve ser após o horário de início.") }
                return@launch
            }

            val sessionToEdit = _uiState.value.editingSession

            val newSession = SleepSession(
                id = sessionToEdit?.id ?: UUID.randomUUID().toString(),
                startTime = startTimeToSave.toInstant(),
                endTime = endTimeToSave.toInstant(),
                notes = notes,
                title = "Sono Manual", // Ou permitir que o usuário defina
                stages = emptyList(), // Estágios não são definidos manualmente desta forma
                efficiency = 0.0, // Pode ser calculado ou deixado como padrão
                source = "Manual"
            )

            try {
                if (sessionToEdit == null) {
                    repository.addManualSleepSession(
                        startTime = newSession.startTimeZoned,
                        endTime = newSession.endTimeZoned,
                        sleepType = null,
                        notes = newSession.notes,
                        wakeCount = newSession.wakeDuringNightCount
                    )
                } else {
                    repository.updateManualSleepSession(newSession)
                }
                _uiState.update { it.copy(showManualEntryDialog = false, editingSession = null, manualEntryNotes = "") } // Limpar estado
                loadConsolidatedSleepData() // Recarregar dados
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao salvar sessão: ${e.message}") }
            }
        }
    }

    fun deleteSleepSession(session: SleepSession) {
        viewModelScope.launch {
            try {
                if (session.source == "Health Connect") {
                    // Implementar a exclusão do Health Connect se necessário, ou impedir
                    _uiState.update { it.copy(error = "Não é possível excluir sessões do Health Connect por aqui.") }
                    return@launch
                } else {
                    repository.deleteManualSleepSession(session)
                }
                loadConsolidatedSleepData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao excluir sessão: ${e.message}") }
            }
        }
    }

    fun onEditSession(session: SleepSession) {
        if (session.source == "Manual") {
            _uiState.update { it.copy(
                editingSession = session,
                selectedStartTime = session.startTime.atZone(ZoneId.systemDefault()),
                selectedEndTime = session.endTime.atZone(ZoneId.systemDefault()),
                manualEntryNotes = session.notes ?: "",
                showManualEntryDialog = true
            ) }
        } else {
            _uiState.update { it.copy(error = "Apenas sessões manuais podem ser editadas.") }
        }
    }

    fun onDismissManualEntryDialog() {
        _uiState.update { it.copy(showManualEntryDialog = false, editingSession = null, manualEntryNotes = "", selectedStartTime = null, selectedEndTime = null) }
    }

    fun onShowManualEntryDialog(show: Boolean) {
        if (!show) {
            onDismissManualEntryDialog() // Limpa o estado se estiver fechando
        } else {
             // Prepara para uma nova entrada, ou pode ser preenchido por onEditSession
            val now = ZonedDateTime.now()
            val defaultStartTime = now.minusHours(8) // Sugestão inicial
            _uiState.update { it.copy(
                showManualEntryDialog = true,
                editingSession = null, // Garante que é uma nova entrada
                selectedDate = defaultStartTime.toLocalDate(),
                selectedStartTime = defaultStartTime,
                selectedEndTime = now, // Sugestão inicial
                manualEntryNotes = ""
            ) }
        }
    }

    private fun isNightSleep(session: SleepSession): Boolean {
        val durationHours = session.duration.toHours()
        val endHour = session.endTime.atZone(ZoneId.systemDefault()).hour
        // Considera sono noturno se durar mais de 4 horas e terminar de manhã (ex: antes das 10h)
        // ou se durar mais de 2h e terminar muito cedo (ex: antes das 5h - cochilo noturno)
        return (durationHours >= 4 && endHour < 12) || (durationHours >=2 && endHour < 5)
    }

    // Outras funções de UI (navegação, mostradores de diálogo etc.) podem ser adicionadas aqui
    
    /**
     * Limpa a mensagem de erro no estado da UI
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Adiciona uma sessão de sono manual ao repositório
     */
    fun addManualSleepSession(startDateTime: ZonedDateTime, endDateTime: ZonedDateTime, notes: String?, wakeCount: Int = 0) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingManualSession = true) }
            try {
                // Verificar se já existe uma sessão neste intervalo de tempo
                val existingSession = _uiState.value.sleepSessions.find { session ->
                    (startDateTime.isAfter(session.startTime.atZone(ZoneId.systemDefault())) && startDateTime.isBefore(session.endTime.atZone(ZoneId.systemDefault()))) ||
                    (endDateTime.isAfter(session.startTime.atZone(ZoneId.systemDefault())) && endDateTime.isBefore(session.endTime.atZone(ZoneId.systemDefault()))) ||
                    (startDateTime.isBefore(session.startTime.atZone(ZoneId.systemDefault())) && endDateTime.isAfter(session.endTime.atZone(ZoneId.systemDefault()))) ||
                    (startDateTime.isEqual(session.startTime.atZone(ZoneId.systemDefault())) && endDateTime.isEqual(session.endTime.atZone(ZoneId.systemDefault())))
                }
                
                if (existingSession != null) {
                    _uiState.update { it.copy(showDuplicateEntryDialog = true) }
                    return@launch
                }
                
                // Criar nova sessão de sono
                val newSession = SleepSession(
                    id = UUID.randomUUID().toString(),
                    startTime = startDateTime.toInstant(),
                    endTime = endDateTime.toInstant(),
                    notes = notes,
                    source = "Manual",
                    wakeDuringNightCount = wakeCount,
                    efficiency = 0.0
                )
                
                // Adicionar ao repositório
                addSleepSession(newSession)
                _uiState.update { it.copy(addSessionSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao adicionar sessão: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isAddingManualSession = false) }
            }
        }
    }
    
    /**
     * Confirma sobrescrever uma sessão de sono existente
     */
    fun confirmOverwriteSleepSession(startDateTime: ZonedDateTime, endDateTime: ZonedDateTime, notes: String?, wakeCount: Int = 0) {
        _uiState.update { it.copy(showDuplicateEntryDialog = false) }
        addManualSleepSession(startDateTime, endDateTime, notes, wakeCount)
    }
    
    /**
     * Cancela a operação de sobrescrever uma sessão de sono
     */
    fun cancelOverwriteSleepSession() {
        _uiState.update { it.copy(showDuplicateEntryDialog = false) }
    }
    
    /**
     * Reseta o sinalizador de sucesso de adição de sessão
     */
    fun resetAddSessionSuccess() {
        _uiState.update { it.copy(addSessionSuccess = false) }
    }
    
    /**
     * Carrega uma sessão para edição
     */
    /**
     * Adiciona uma sessão de sono ao repositório
     */
    /**
     * Carrega as sessões de sono do repositório
     */
    private fun loadSleepSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Usar um intervalo de tempo amplo para obter todas as sessões
                val endTime = ZonedDateTime.now()
                val startTime = endTime.minusYears(1) // Buscar sessões do último ano
                
                repository.getSleepSessions(startTime, endTime).collect { sessions ->
                    _uiState.update { it.copy(
                        sleepSessions = sessions,
                        lastSession = sessions.maxByOrNull { it.startTime },
                        sessionAnalyses = generateSessionAnalyses(sessions),
                        isLoading = false
                    ) }
                    // Gerar recomendações de IA
                    generateAIAdvice(sessions)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = "Erro ao carregar sessões: ${e.message}",
                    isLoading = false
                ) }
            }
        }
    }
    
    /**
     * Adiciona uma sessão de sono ao repositório
     */
    private fun addSleepSession(session: SleepSession) {
        viewModelScope.launch {
            try {
                // Extrair valores da sessão para passar para o método do repositório
                val startTime = session.startTime.atZone(ZoneId.systemDefault())
                val endTime = session.endTime.atZone(ZoneId.systemDefault())
                val notes = session.notes
                val wakeCount = session.wakeDuringNightCount
                
                repository.addManualSleepSession(
                    startTime = startTime,
                    endTime = endTime,
                    notes = notes,
                    wakeCount = wakeCount
                )
                loadSleepSessions()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao adicionar sessão: ${e.message}") }
            }
        }
    }
    
    /**
     * Atualiza uma sessão de sono existente no repositório
     */
    private fun updateSleepSession(session: SleepSession) {
        viewModelScope.launch {
            try {
                repository.updateManualSleepSession(session)
                loadSleepSessions()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao atualizar sessão: ${e.message}") }
            }
        }
    }
    
    fun loadSleepSessionForEditing(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingEditingSession = true) }
            try {
                val session = _uiState.value.sleepSessions.find { it.id == sessionId }
                if (session != null) {
                    _uiState.update { it.copy(
                        currentEditingSession = session,
                        selectedDate = session.startTime.atZone(ZoneId.systemDefault()).toLocalDate(),
                        selectedStartTime = session.startTime.atZone(ZoneId.systemDefault()),
                        selectedEndTime = session.endTime.atZone(ZoneId.systemDefault()),
                        manualEntryNotes = session.notes ?: ""
                    ) }
                } else {
                    _uiState.update { it.copy(error = "Sessão não encontrada") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao carregar sessão: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoadingEditingSession = false) }
            }
        }
    }
    
    /**
     * Limpa a sessão atualmente em edição
     */
    fun clearEditingSession() {
        _uiState.update { it.copy(
            currentEditingSession = null,
            selectedDate = LocalDate.now(),
            selectedStartTime = null,
            selectedEndTime = null,
            manualEntryNotes = ""
        ) }
    }
    
    /**
     * Atualiza uma sessão de sono manual existente
     */
    fun updateManualSleepSession(sessionId: String, startDateTime: ZonedDateTime, endDateTime: ZonedDateTime, notes: String?, wakeCount: Int = 0) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingManualSession = true) }
            try {
                // Buscar a sessão original
                val originalSession = _uiState.value.sleepSessions.find { it.id == sessionId }
                if (originalSession == null) {
                    _uiState.update { it.copy(error = "Sessão não encontrada para atualização") }
                    return@launch
                }
                
                // Criar sessão atualizada
                val updatedSession = originalSession.copy(
                    startTime = startDateTime.toInstant(),
                    endTime = endDateTime.toInstant(),
                    notes = notes,
                    wakeDuringNightCount = wakeCount
                )
                
                // Atualizar no repositório
                updateSleepSession(updatedSession)
                _uiState.update { it.copy(addSessionSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao atualizar sessão: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isAddingManualSession = false) }
            }
        }
    }
}

/**
 * Estado da UI para a tela de sono
 * Contém todos os dados necessários para renderizar a interface
 */
data class SleepUiState(
    val isLoading: Boolean = true,
    val sleepSessions: List<SleepSession> = emptyList(),
    val lastSession: SleepSession? = null,
    val error: String? = null,
    val hasPermissions: Boolean = true,
    val sleepAdvice: SleepAdvice? = null, // Para as dicas geradas pela IA
    val aiAdviceLoading: Boolean = false,
    val sessionAnalyses: Map<String, DailyAnalysis> = emptyMap(), // Análises individuais
    val showManualEntryDialog: Boolean = false,
    val selectedDate: LocalDate? = LocalDate.now(),
    val selectedStartTime: ZonedDateTime? = null,
    val selectedEndTime: ZonedDateTime? = null,
    val manualEntryNotes: String = "",
    val editingSession: SleepSession? = null, // Para editar uma sessão manual existente
    val addSessionSuccess: Boolean = false, // Indica se uma sessão foi adicionada com sucesso
    val showDuplicateEntryDialog: Boolean = false, // Indica se deve mostrar diálogo de entrada duplicada
    val isAddingManualSession: Boolean = false, // Indica se está no processo de adicionar uma sessão manual
    val isLoadingEditingSession: Boolean = false, // Indica se está carregando uma sessão para edição
    val currentEditingSession: SleepSession? = null // Sessão atual em edição
)