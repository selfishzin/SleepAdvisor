package com.example.sleepadvisor.integration

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sleepadvisor.data.local.AppDatabase
import com.example.sleepadvisor.data.local.dao.ManualSleepSessionDao
import com.example.sleepadvisor.data.repository.SleepRepositoryImpl
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.repository.SleepRepository
import com.example.sleepadvisor.domain.service.SleepAIService
import com.example.sleepadvisor.domain.service.SleepAdvice
import com.example.sleepadvisor.domain.usecase.GetSleepSessionsUseCase
import com.example.sleepadvisor.presentation.screens.sleep.SleepViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import java.time.ZonedDateTime
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ViewModelRepositoryIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var dao: ManualSleepSessionDao
    private lateinit var repository: SleepRepository
    private lateinit var getSleepSessionsUseCase: GetSleepSessionsUseCase
    private lateinit var viewModel: SleepViewModel
    private val mockHealthConnectClient = mock(androidx.health.connect.client.HealthConnectClient::class.java)
    private val mockAIService = mock(SleepAIService::class.java)

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        dao = database.manualSleepSessionDao()
        repository = SleepRepositoryImpl(mockHealthConnectClient, dao)
        getSleepSessionsUseCase = GetSleepSessionsUseCase(repository)
        
        // Configura o mock do HealthConnectClient para retornar permissões concedidas
        val permissions = setOf(
            androidx.health.connect.client.permission.HealthPermission.getReadPermission(
                androidx.health.connect.client.records.SleepSessionRecord::class
            ),
            androidx.health.connect.client.permission.HealthPermission.getWritePermission(
                androidx.health.connect.client.records.SleepSessionRecord::class
            )
        )
        `when`(mockHealthConnectClient.permissionController.getGrantedPermissions())
            .thenReturn(permissions)
            
        // Configura o mock do serviço de IA
        `when`(mockAIService.generateSleepAdvice(anyList()))
            .thenReturn(
                SleepAdvice(
                    tips = listOf("Mantenha um horário regular de sono"),
                    warnings = emptyList(),
                    positiveReinforcement = "Boa consistência nos horários de dormir!"
                )
            )
        
        viewModel = SleepViewModel(
            getSleepSessionsUseCase = getSleepSessionsUseCase,
            healthConnectClient = mockHealthConnectClient,
            repository = repository,
            sleepAIService = mockAIService
        )
    }

    @After
    fun cleanup() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testAddingManualSleepSession() = testScope.runTest {
        // DADO: Horários de início e fim para uma nova sessão
        val startTime = ZonedDateTime.now().minusHours(8)
        val endTime = ZonedDateTime.now()
        val notes = "Teste de integração"
        val wakeCount = 2

        // QUANDO: Adicionamos uma sessão através do ViewModel
        viewModel.addManualSleepSession(startTime, endTime, notes, wakeCount)
        advanceUntilIdle()  // Avança o tempo até que todas as corrotinas sejam concluídas

        // ENTÃO: A sessão deve ser salva e recuperada no repositório
        val sessions = repository.getSleepSessions(
            startTime.minusDays(1),
            endTime.plusDays(1)
        ).first()

        assertEquals(1, sessions.size)
        with(sessions[0]) {
            assertEquals(startTime.toInstant(), this.startTime.toInstant())
            assertEquals(endTime.toInstant(), this.endTime.toInstant())
            assertEquals(notes, this.notes)
            assertEquals(wakeCount, this.wakeDuringNightCount)
            assertTrue(this.isManualEntry)
        }
    }

    @Test
    fun testDuplicateEntryDetection() = testScope.runTest {
        // DADO: Uma sessão existente
        val today = ZonedDateTime.now()
        
        // Primeiro, salvamos a sessão diretamente no repositório
        repository.saveManualSleepSession(
            startTime = today.minusHours(8),
            endTime = today,
            lightSleepPercentage = 50.0,
            deepSleepPercentage = 30.0,
            remSleepPercentage = 20.0,
            notes = "Sessão original",
            wakeDuringNightCount = 1
        )
        
        // QUANDO: Tentamos adicionar outra sessão no mesmo dia através do ViewModel
        viewModel.addManualSleepSession(
            startTime = today.minusHours(7),
            endTime = today.plusHours(1),
            notes = "Sessão duplicada",
            wakeDuringNightCount = 2
        )
        advanceUntilIdle()

        // ENTÃO: O diálogo de duplicação deve ser ativado
        val uiState = viewModel.uiState.value
        assertTrue(uiState.showDuplicateEntryDialog)
    }

    @Test
    fun testConfirmOverwriteReplacesExistingSession() = testScope.runTest {
        // DADO: Uma sessão existente
        val today = ZonedDateTime.now()
        
        // Primeiro, salvamos a sessão diretamente no repositório
        repository.saveManualSleepSession(
            startTime = today.minusHours(8),
            endTime = today,
            lightSleepPercentage = 50.0,
            deepSleepPercentage = 30.0,
            remSleepPercentage = 20.0,
            notes = "Sessão original",
            wakeDuringNightCount = 1
        )
        
        // Pegamos a sessão existente para ter o ID
        val existingSession = repository.getSleepSessions(
            today.minusDays(1),
            today.plusDays(1)
        ).first().first()
        
        // Tentamos adicionar outra sessão no mesmo dia através do ViewModel
        viewModel.addManualSleepSession(
            startTime = today.minusHours(7),
            endTime = today.plusHours(1),
            notes = "Sessão nova",
            wakeDuringNightCount = 3
        )
        advanceUntilIdle()
        
        // QUANDO: Confirmamos a substituição
        viewModel.confirmOverwriteSleepSession()
        advanceUntilIdle()

        // ENTÃO: A sessão antiga deve ser substituída pela nova
        val updatedSessions = repository.getSleepSessions(
            today.minusDays(1),
            today.plusDays(1)
        ).first()
        
        assertEquals(1, updatedSessions.size)
        with(updatedSessions[0]) {
            assertNotEquals(existingSession.id, this.id) // ID deve ser diferente (nova entrada)
            assertEquals("Sessão nova", this.notes)
            assertEquals(3, this.wakeDuringNightCount)
        }
    }

    @Test
    fun testHealthConnectAndManualDataMerging() = testScope.runTest {
        // DADO: Sessões manuais no banco de dados
        val today = ZonedDateTime.now()
        val yesterday = today.minusDays(1)
        
        // Adiciona sessões manuais para dois dias
        repository.saveManualSleepSession(
            startTime = yesterday.minusHours(8),
            endTime = yesterday,
            lightSleepPercentage = 50.0,
            deepSleepPercentage = 30.0,
            remSleepPercentage = 20.0,
            notes = "Sessão manual dia 1",
            wakeDuringNightCount = 1
        )
        
        repository.saveManualSleepSession(
            startTime = today.minusHours(8),
            endTime = today,
            lightSleepPercentage = 50.0,
            deepSleepPercentage = 30.0,
            remSleepPercentage = 20.0,
            notes = "Sessão manual dia 2",
            wakeDuringNightCount = 2
        )
        
        // QUANDO: Carregamos os dados consolidados via ViewModel
        viewModel.loadConsolidatedSleepData()
        advanceUntilIdle()

        // ENTÃO: Os dados manuais devem estar presentes no estado da UI
        val uiState = viewModel.uiState.value
        assertEquals(2, uiState.sleepSessions.size)
        
        // Verifica se a sessão de hoje está presente
        val todaySession = uiState.sleepSessions.find { 
            it.startTime.toLocalDate() == today.toLocalDate() 
        }
        assertNotNull(todaySession)
        assertEquals("Sessão manual dia 2", todaySession?.notes)
        
        // Verifica se a IA foi chamada com os dados corretos
        verify(mockAIService).generateSleepAdvice(uiState.sleepSessions)
    }

    @Test
    fun testEditingExistingSession() = testScope.runTest {
        // DADO: Uma sessão existente
        val sessionId = UUID.randomUUID().toString()
        val today = ZonedDateTime.now()
        
        repository.saveManualSleepSession(
            id = sessionId,
            startTime = today.minusHours(8),
            endTime = today,
            lightSleepPercentage = 50.0,
            deepSleepPercentage = 30.0,
            remSleepPercentage = 20.0,
            notes = "Notas originais",
            wakeDuringNightCount = 1
        )
        
        // QUANDO: Carregamos a sessão para edição e a atualizamos
        viewModel.loadSleepSessionForEditing(sessionId)
        advanceUntilIdle()
        
        // Verifica se a sessão foi carregada corretamente
        assertNotNull(viewModel.uiState.value.currentEditingSession)
        assertEquals(sessionId, viewModel.uiState.value.currentEditingSession?.id)
        
        // Atualiza a sessão
        viewModel.updateManualSleepSession(
            id = sessionId,
            startTime = today.minusHours(7),  // Hora diferente
            endTime = today.plusHours(1),     // Hora diferente
            notes = "Notas atualizadas",
            wakeDuringNightCount = 3          // Valor diferente
        )
        advanceUntilIdle()

        // ENTÃO: A sessão deve ser atualizada no repositório
        val updatedSession = repository.getSleepSessionById(sessionId)
        assertNotNull(updatedSession)
        assertEquals("Notas atualizadas", updatedSession?.notes)
        assertEquals(3, updatedSession?.wakeDuringNightCount)
        assertEquals(today.minusHours(7).toInstant(), updatedSession?.startTime?.toInstant())
    }
}
