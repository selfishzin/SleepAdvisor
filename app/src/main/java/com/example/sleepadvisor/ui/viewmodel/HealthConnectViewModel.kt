package com.example.sleepadvisor.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepadvisor.data.HealthConnectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HealthConnectUiState(
    val isLoading: Boolean = false,
    val isAvailable: Boolean = false,
    val isInstalled: Boolean = false,
    val hasPermissions: Boolean = false,
    val isRequestingPermissions: Boolean = false,
    val showPermissionRequest: Boolean = false,
    val steps: List<StepsRecord> = emptyList(),
    val error: String? = null
)

class HealthConnectViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HealthConnectRepository(application)
    private val _uiState = MutableStateFlow(HealthConnectUiState(isLoading = true))
    val uiState: StateFlow<HealthConnectUiState> = _uiState

    init {
        checkAvailability()
    }

    private fun checkAvailability() {
        viewModelScope.launch {
            try {
                val isInstalled = repository.isProviderInstalled()
                val availability = repository.checkAvailability()
                
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        isInstalled = isInstalled,
                        isAvailable = availability == HealthConnectClient.SDK_AVAILABLE,
                        showPermissionRequest = availability == HealthConnectClient.SDK_AVAILABLE && !currentState.hasPermissions,
                        error = when {
                            !isInstalled -> "Health Connect não está instalado. Por favor, instale para continuar."
                            availability == HealthConnectClient.SDK_UNAVAILABLE -> 
                                "Health Connect não está disponível neste dispositivo"
                            availability == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> 
                                "Atualização do Health Connect necessária"
                            else -> null
                        }
                    )
                }

                if (availability == HealthConnectClient.SDK_AVAILABLE) {
                    repository.initializeClient()
                    checkPermissions()
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = "Erro ao verificar disponibilidade: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun checkPermissions() {
        val hasPermissions = repository.hasAllPermissions()
        _uiState.update { it.copy(
            hasPermissions = hasPermissions,
            showPermissionRequest = !hasPermissions
        ) }
        if (hasPermissions) {
            loadStepsData()
        }
    }

    fun getPermissions() = repository.permissions

    fun getInstallIntent(): Intent = repository.getHealthConnectIntent()

    fun requestPermissions(onRequest: (Set<String>) -> Unit) {
        if (!_uiState.value.isRequestingPermissions) {
            _uiState.update { it.copy(
                isRequestingPermissions = true,
                showPermissionRequest = true
            ) }
            onRequest(repository.permissions)
        }
    }

    fun onPermissionsGranted() {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isRequestingPermissions = false,
                showPermissionRequest = false,
                hasPermissions = true,
                error = null
            ) }
            loadStepsData()
        }
    }

    fun onPermissionsDenied() {
        _uiState.update { it.copy(
            isRequestingPermissions = false,
            showPermissionRequest = true,
            hasPermissions = false,
            error = "Para usar o app, você precisa conceder as permissões necessárias."
        ) }
    }

    private fun loadStepsData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.readStepsData()
                    .collect { steps ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                steps = steps,
                                error = if (steps.isEmpty()) "Nenhum dado de passos disponível" else null
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Erro ao carregar dados: ${e.message}"
                    )
                }
            }
        }
    }

    fun retryOperation() {
        if (!_uiState.value.isAvailable) {
            checkAvailability()
        } else if (!_uiState.value.hasPermissions) {
            _uiState.update { it.copy(showPermissionRequest = true) }
        } else {
            loadStepsData()
        }
    }
} 