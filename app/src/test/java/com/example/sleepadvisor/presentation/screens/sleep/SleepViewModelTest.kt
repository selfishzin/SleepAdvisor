package com.example.sleepadvisor.presentation.screens.sleep

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepStage
import com.example.sleepadvisor.domain.model.SleepStageType
import com.example.sleepadvisor.domain.repository.SleepRepository
import com.example.sleepadvisor.domain.service.SleepAIService
import com.example.sleepadvisor.domain.service.SleepAdvice
import com.example.sleepadvisor.domain.usecase.GetSleepSessionsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class SleepViewModelTest {

    @Mock
    private lateinit var mockGetSleepSessionsUseCase: GetSleepSessionsUseCase

    @Mock
    private lateinit var mockHealthConnectClient: HealthConnectClient

    @Mock
    private lateinit var mockRepository: SleepRepository

    @Mock
    private lateinit var mockSleepAIService: SleepAIService

    private lateinit var viewModel: SleepViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Configura o mock do HealthConnectClient para retornar permissões concedidas
        val permissions = setOf(
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getWritePermission(SleepSessionRecord::class)
        )
        `when`(mockHealthConnectClient.permissionController.getGrantedPermissions())
            .thenReturn(permissions)
        
        viewModel = SleepViewModel(
            getSleepSessionsUseCase = mockGetSleepSessionsUseCase,
            healthConnectClient = mockHealthConnectClient,
            repository = mockRepository,
            sleepAIService = mockSleepAIService
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ao inicializar, deve carregar os dados de sono consolidados`() = testScope.runTest {
        // Dado
        val testSessions = listOf(createTestSleepSession())
        `when`(mockGetSleepSessionsUseCase.getConsolidatedSleepSessions())
            .thenReturn(flowOf(testSessions))
        
        // Quando: O ViewModel é inicializado no setup
        
        // Então: Deve carregar os dados
        verify(mockGetSleepSessionsUseCase).getConsolidatedSleepSessions()
        
        // Avança as corrotinas para garantir que o estado seja atualizado
        advanceUntilIdle()
        
        // Verifica se o estado foi atualizado corretamente
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(testSessions, uiState.sleepSessions)
        assertEquals(testSessions.first(), uiState.lastSession)
    }

    @Test
    fun `ao adicionar uma sessão manual, deve salvar no repositório`() = testScope.runTest {
        // Dado
        val startTime = ZonedDateTime.now().minusHours(8)
        val endTime = ZonedDateTime.now()
        val notes = "Notas de teste"
        val wakeCount = 2
        
        // Configura o mock para retornar uma lista vazia (sem duplicatas)
        `when`(mockRepository.getSleepSessions(any(), any()))
            .thenReturn(flowOf(emptyList()))
        
        // Quando
        viewModel.addManualSleepSession(startTime, endTime, notes, wakeCount)
        
        // Avança as corrotinas
        advanceUntilIdle()
        
        // Então
        verify(mockRepository).saveManualSleepSession(
            startTime = startTime,
            endTime = endTime,
            lightSleepPercentage = eq(0.0),
            deepSleepPercentage = eq(0.0),
            remSleepPercentage = eq(0.0),
            notes = eq(notes),
            wakeDuringNightCount = eq(wakeCount)
        )
    }

    @Test
    fun `ao tentar adicionar uma sessão duplicada, deve abrir diálogo de confirmação`() = testScope.runTest {
        // Dado: Já existe uma sessão no mesmo dia
        val existingSession = createTestSleepSession()
        val startTime = existingSession.startTime.plusHours(1)
        val endTime = existingSession.endTime.plusHours(1)
        
        `when`(mockRepository.getSleepSessions(any(), any()))
            .thenReturn(flowOf(listOf(existingSession)))
        
        // Quando
        viewModel.addManualSleepSession(startTime, endTime, "Notas", 0)
        advanceUntilIdle()
        
        // Então
        assertTrue(viewModel.uiState.value.showDuplicateEntryDialog)
    }

    @Test
    fun `ao confirmar sobrescrita, deve remover a sessão existente e adicionar a nova`() = testScope.runTest {
        // Dado
        val existingSession = createTestSleepSession()
        val newStartTime = existingSession.startTime.plusHours(1)
        val newEndTime = existingSession.endTime.plusHours(1)
        
        `when`(mockRepository.getSleepSessions(any(), any()))
            .thenReturn(flowOf(listOf(existingSession)))
        
        // Quando
        viewModel.addManualSleepSession(newStartTime, newEndTime, "Nova nota", 1)
        advanceUntilIdle()
        
        // Confirma a sobrescrita
        viewModel.confirmOverwriteSleepSession()
        advanceUntilIdle()
        
        // Então
        verify(mockRepository).deleteSleepSession(existingSession.id)
        verify(mockRepository).saveManualSleepSession(
            startTime = eq(newStartTime),
            endTime = eq(newEndTime),
            lightSleepPercentage = any(),
            deepSleepPercentage = any(),
            remSleepPercentage = any(),
            notes = eq("Nova nota"),
            wakeDuringNightCount = eq(1)
        )
    }

    @Test
    fun `ao gerar conselhos de IA, deve atualizar o estado com o resultado`() = testScope.runTest {
        // Dado
        val testSessions = listOf(createTestSleepSession())
        val testAdvice = SleepAdvice(
            advice = "Durma mais cedo",
            score = 85,
            recommendations = listOf("Manter horários regulares", "Evitar cafeína à noite")
        )
        
        `when`(mockSleepAIService.generateSleepAdvice(testSessions))
            .thenReturn(testAdvice)
        
        // Quando
        viewModel.generateAIAdvice(testSessions)
        advanceUntilIdle()
        
        // Então
        assertEquals(testAdvice, viewModel.uiState.value.sleepAdvice)
        assertFalse(viewModel.uiState.value.aiAdviceLoading)
    }

    @Test
    fun `ao ocorrer um erro ao carregar dados, deve atualizar o estado de erro`() = testScope.runTest {
        // Dado
        val errorMessage = "Erro de teste"
        `when`(mockGetSleepSessionsUseCase.getConsolidatedSleepSessions())
            .thenThrow(RuntimeException(errorMessage))
        
        // Quando
        viewModel.loadConsolidatedSleepData()
        advanceUntilIdle()
        
        // Então
        assertTrue(viewModel.uiState.value.error?.contains(errorMessage) == true)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private fun createTestSleepSession(): SleepSession {
        val startTime = ZonedDateTime.now().minusHours(8)
        return SleepSession(
            id = UUID.randomUUID().toString(),
            startTime = startTime.toInstant(),
            endTime = startTime.plusHours(7).toInstant(),
            stages = listOf(
                SleepStage(
                    startTime = startTime.toInstant(),
                    endTime = startTime.plusHours(4).toInstant(),
                    type = SleepStageType.LIGHT,
                    source = "Test"
                ),
                SleepStage(
                    startTime = startTime.plusHours(4).toInstant(),
                    endTime = startTime.plusHours(5).plusMinutes(30).toInstant(),
                    type = SleepStageType.DEEP,
                    source = "Test"
                ),
                SleepStage(
                    startTime = startTime.plusHours(5).plusMinutes(30).toInstant(),
                    endTime = startTime.plusHours(7).toInstant(),
                    type = SleepStageType.REM,
                    source = "Test"
                )
            ),
            notes = "Notas de teste",
            wakeDuringNightCount = 1,
            source = "Test"
        )
    }
}
