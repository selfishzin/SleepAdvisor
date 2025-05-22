package com.example.sleepadvisor.presentation.screens.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import com.example.sleepadvisor.domain.model.DailyAnalysis
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepSource
import com.example.sleepadvisor.domain.model.SleepStageType
import com.example.sleepadvisor.domain.model.calculateAndUpdateStagePercentages
import com.example.sleepadvisor.domain.model.toDailyAnalysis
import com.example.sleepadvisor.domain.service.SleepAIService
import com.example.sleepadvisor.domain.service.SleepAdvice
import com.example.sleepadvisor.domain.usecase.GetSleepSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.Duration
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.sleepadvisor.domain.repository.SleepRepository
import kotlinx.coroutines.flow.catch
import android.util.Log
import java.time.LocalDateTime
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

                        // Garantir que todos os percentuais de estágios de sono sejam calculados
                        val sessionsWithCalculatedPercentages = consolidatedSessions.map { session ->
                            session.calculateAndUpdateStagePercentages()
                        }
                        
                        // Atualizar o lastNightSession para usar a versão com percentuais calculados
                        val updatedLastNightSession = lastNightSession?.let { session ->
                            sessionsWithCalculatedPercentages.find { it.id == session.id }
                        }
                        
                        _uiState.update { currentState ->
                            currentState.copy(
                                sleepSessions = sessionsWithCalculatedPercentages.sortedByDescending { it.startTime },
                                isLoading = false,
                                lastSession = updatedLastNightSession,
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
            val notesToSave = _uiState.value.manualEntryNotes
            val wakeCountToSave = _uiState.value.wakeCount
            val sessionToEdit = _uiState.value.editingSession

            if (startTimeToSave == null || endTimeToSave == null) {
                _uiState.update { it.copy(error = "Horário de início e fim devem ser definidos.") }
                return@launch
            }

            if (endTimeToSave.isBefore(startTimeToSave) || endTimeToSave == startTimeToSave) {
                _uiState.update { it.copy(error = "Horário de fim deve ser após o horário de início.") }
                return@launch
            }

            try {
                val newSession = SleepSession(
                    id = sessionToEdit?.id ?: UUID.randomUUID().toString(),
                    startTime = startTimeToSave.toInstant(),
                    endTime = endTimeToSave.toInstant(),
                    notes = notesToSave.ifBlank { null }, // Salvar null se vazio
                    source = SleepSource.MANUAL,
                    wakeDuringNightCount = wakeCountToSave,
                    // title, stages, efficiency, etc., podem ter valores padrão ou não serem definidos para manual
                    title = sessionToEdit?.title ?: "Sono Manual",
                    stages = sessionToEdit?.stages ?: emptyList(),
                    efficiency = sessionToEdit?.efficiency ?: 0.0,
                    deepSleepPercentage = sessionToEdit?.deepSleepPercentage ?: 0.0,
                    remSleepPercentage = sessionToEdit?.remSleepPercentage ?: 0.0,
                    lightSleepPercentage = sessionToEdit?.lightSleepPercentage ?: 0.0,
                    heartRateSamples = sessionToEdit?.heartRateSamples ?: emptyList()
                )

                if (sessionToEdit == null) {
                    addSleepSession(newSession) // ViewModel's internal method
                } else {
                    updateSleepSession(newSession) // ViewModel's internal method
                }
                _uiState.update { it.copy(
                    showManualEntryDialog = false,
                    editingSession = null,
                    selectedStartTime = null,
                    selectedEndTime = null,
                    manualEntryNotes = "",
                    wakeCount = 0,
                    addSessionSuccess = sessionToEdit == null,
                    updateSessionSuccess = sessionToEdit != null,
                    error = null // Limpar erro em caso de sucesso
                ) }
                loadConsolidatedSleepData() // Recarregar dados
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao salvar sessão: ${e.message}") }
            }
        }
    }

    fun deleteSleepSession(session: SleepSession) {
        viewModelScope.launch {
            try {
                if (session.source == SleepSource.HEALTH_CONNECT) {
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
        if (session.source == SleepSource.MANUAL) {
            _uiState.update { it.copy(
                editingSession = session,
                selectedDate = session.startTime.atZone(ZoneId.systemDefault()).toLocalDate(),
                selectedStartTime = session.startTime.atZone(ZoneId.systemDefault()),
                selectedEndTime = session.endTime.atZone(ZoneId.systemDefault()),
                manualEntryNotes = session.notes ?: "",
                wakeCount = session.wakeDuringNightCount,
                showManualEntryDialog = true,
                error = null // Limpar qualquer erro anterior ao abrir o diálogo
            ) }
        } else {
            _uiState.update { it.copy(error = "Apenas sessões manuais podem ser editadas.") }
        }
    }

    fun onDismissManualEntryDialog() {
        // Limpa o estado relacionado ao diálogo de entrada manual
        _uiState.update {
            it.copy(
                showManualEntryDialog = false,
                editingSession = null,
                selectedStartTime = null,
                selectedEndTime = null,
                manualEntryNotes = "",
                wakeCount = 0,
                selectedDate = _uiState.value.selectedDate ?: LocalDate.now() // Manter selectedDate ou resetar?
                // Resetar selectedDate para hoje pode ser mais consistente: selectedDate = LocalDate.now()
            )
        }
    }

    fun onShowManualEntryDialog(show: Boolean) {
        if (!show) {
            onDismissManualEntryDialog() // Limpa o estado se estiver fechando
        } else {
            // Prepara para uma nova entrada, ou pode ser preenchido por onEditSession
            // Se onEditSession foi chamado antes, editingSession já estará populado.
            // Se não, é uma nova entrada.
            if (_uiState.value.editingSession == null) { // Garante que é uma nova entrada se não estiver editando
                val now = ZonedDateTime.now()
                val defaultStartTime = now.minusHours(8) // Sugestão inicial
                _uiState.update { it.copy(
                    showManualEntryDialog = true,
                    editingSession = null, // Garante que é uma nova entrada
                    selectedDate = defaultStartTime.toLocalDate(),
                    selectedStartTime = defaultStartTime,
                    selectedEndTime = now, // Sugestão inicial
                    manualEntryNotes = "",
                    wakeCount = 0,
                    error = null // Limpar qualquer erro anterior
                ) }
            } else {
                // Se editingSession não for nulo, significa que onEditSession já preparou o estado.
                // Apenas garantimos que o diálogo seja exibido.
                _uiState.update { it.copy(showManualEntryDialog = true, error = null) }
            }
        }
    }

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
                    source = SleepSource.MANUAL,
                    title = "Sono Manual",
                    notes = notes,
                    wakeDuringNightCount = wakeCount,
                    stages = emptyList(),
                    efficiency = 0.0,
                    deepSleepPercentage = 0.0,
                    remSleepPercentage = 0.0,
                    lightSleepPercentage = 0.0,
                    heartRateSamples = emptyList()
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
    private suspend fun addSleepSession(session: SleepSession) {
        try {
            val addedSession = repository.addManualSleepSession(
                startTime = session.startTimeZoned,
                endTime = session.endTimeZoned,
                sleepType = session.title,
                notes = session.notes,
                wakeCount = session.wakeDuringNightCount
            )
            _uiState.update { it.copy(addSessionSuccess = true) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Erro ao adicionar sessão: ${e.message}") }
        }
    }
    
    /**
     * Atualiza uma sessão de sono existente no repositório
     */
    private suspend fun updateSleepSession(session: SleepSession) {
        try {
            val updatedSession = repository.updateManualSleepSession(session)
            _uiState.update { it.copy(updateSessionSuccess = true) }
            loadSleepSessions()
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Erro ao atualizar sessão: ${e.message}") }
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

    private fun addManualSleepSessionFromDialog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingManualSession = true) }
            try {
                val startDateTime = _uiState.value.selectedStartTime
                val endDateTime = _uiState.value.selectedEndTime
                val notes = _uiState.value.manualEntryNotes
                val wakeCount = _uiState.value.wakeCount

                if (startDateTime == null || endDateTime == null) {
                    _uiState.update { it.copy(error = "Horário de início e fim devem ser definidos.", isAddingManualSession = false) }
                    return@launch
                }

                if (endDateTime.isBefore(startDateTime) || endDateTime.isEqual(startDateTime)) {
                    _uiState.update { it.copy(error = "O horário de término deve ser posterior ao horário de início.", isAddingManualSession = false) }
                    return@launch
                }

                val editingSession = _uiState.value.editingSession
                val newSession = SleepSession(
                    id = editingSession?.id ?: UUID.randomUUID().toString(),
                    startTime = startDateTime.toInstant(),
                    endTime = endDateTime.toInstant(),
                    source = SleepSource.MANUAL,
                    title = editingSession?.title ?: "Sono Manual",
                    notes = notes,
                    wakeDuringNightCount = wakeCount,
                    stages = editingSession?.stages ?: emptyList(),
                    efficiency = editingSession?.efficiency ?: 0.0,
                    deepSleepPercentage = editingSession?.deepSleepPercentage ?: 0.0,
                    remSleepPercentage = editingSession?.remSleepPercentage ?: 0.0,
                    lightSleepPercentage = editingSession?.lightSleepPercentage ?: 0.0,
                    heartRateSamples = editingSession?.heartRateSamples ?: emptyList()
                )

                if (editingSession == null) {
                    // Adicionar nova sessão
                    repository.addManualSleepSession(
                        startTime = startDateTime, // ZonedDateTime
                        endTime = endDateTime,   // ZonedDateTime
                        sleepType = newSession.title ?: "Sono Manual", // String?
                        notes = newSession.notes, // String?
                        wakeCount = newSession.wakeDuringNightCount // Int
                    )
                    _uiState.update { it.copy(addSessionSuccess = true) }
                } else {
                    // Atualizar sessão existente
                    repository.updateManualSleepSession(newSession) // Espera SleepSession
                    _uiState.update { it.copy(updateSessionSuccess = true) }
                }
                
                _uiState.update { 
                    it.copy(
                        showManualEntryDialog = false, 
                        editingSession = null, 
                        manualEntryNotes = "", 
                        selectedStartTime = null, 
                        selectedEndTime = null, 
                        wakeCount = 0
                    )
                }
                loadConsolidatedSleepData() // Recarregar dados
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao salvar sessão: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isAddingManualSession = false) }
            }
        }
    }

    private fun isNightSleep(session: SleepSession): Boolean {
        // Considers night sleep if duration is more than 3 hours.
        // You might want to add more sophisticated logic, e.g., checking start/end times.
        val durationHours = session.duration.toHours()
        return durationHours >= 3
    }

    data class SleepUiState(
        val isLoading: Boolean = true,
        val sleepSessions: List<SleepSession> = emptyList(),
        val lastSession: SleepSession? = null, 
        val error: String? = null,
        val hasPermissions: Boolean = true, 
        val sleepAdvice: SleepAdvice? = null, 
        val aiAdviceLoading: Boolean = false,
        val sessionAnalyses: Map<String, DailyAnalysis> = emptyMap(), 
        val showManualEntryDialog: Boolean = false,
        val selectedDate: LocalDate? = LocalDate.now(), 
        val selectedStartTime: ZonedDateTime? = null, 
        val selectedEndTime: ZonedDateTime? = null, 
        val manualEntryNotes: String = "", 
        val wakeCount: Int = 0, 
        val editingSession: SleepSession? = null, 
        val addSessionSuccess: Boolean = false, 
        val updateSessionSuccess: Boolean = false, 
        val showDuplicateEntryDialog: Boolean = false, 
        val isAddingManualSession: Boolean = false, 
        val isLoadingEditingSession: Boolean = false, 
        val currentEditingSession: SleepSession? = null 
    )
}