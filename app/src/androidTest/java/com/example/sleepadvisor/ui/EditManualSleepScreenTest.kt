package com.example.sleepadvisor.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepStage
import com.example.sleepadvisor.domain.model.SleepStageType
import com.example.sleepadvisor.presentation.screens.sleep.EditManualSleepScreen
import com.example.sleepadvisor.presentation.screens.sleep.SleepUiState
import com.example.sleepadvisor.presentation.screens.sleep.SleepViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EditManualSleepScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var mockViewModel: SleepViewModel
    private val uiState = MutableStateFlow(SleepUiState())
    private val testSessionId = "test-session-id-123"
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    @Before
    fun setup() {
        // Mock do ViewModel
        mockViewModel = mock(SleepViewModel::class.java)
        `when`(mockViewModel.uiState).thenReturn(uiState as StateFlow<SleepUiState>)
    }

    @Test
    fun testSessionLoadingOnScreenStart() {
        // Define a composição
        composeRule.setContent {
            EditManualSleepScreen(
                sessionId = testSessionId,
                viewModel = mockViewModel,
                onNavigateBack = {}
            )
        }

        // Verifica se o método para carregar a sessão foi chamado
        verify(mockViewModel).loadSleepSessionForEditing(testSessionId)
    }

    @Test
    fun testFormFieldsArePopulatedWithSessionData() {
        // Cria uma sessão de teste
        val now = ZonedDateTime.now()
        val testSession = SleepSession(
            id = testSessionId,
            startTime = now.minusHours(8),
            endTime = now,
            stages = listOf(
                SleepStage(SleepStageType.LIGHT, Duration.ofHours(4)),
                SleepStage(SleepStageType.DEEP, Duration.ofHours(2)),
                SleepStage(SleepStageType.REM, Duration.ofHours(2))
            ),
            isManualEntry = true,
            notes = "Notas de teste",
            wakeDuringNightCount = 2
        )

        // Configura o estado com a sessão para edição
        uiState.value = SleepUiState(
            currentEditingSession = testSession
        )

        // Define a composição
        composeRule.setContent {
            EditManualSleepScreen(
                sessionId = testSessionId,
                viewModel = mockViewModel,
                onNavigateBack = {}
            )
        }

        // Verifica se os campos estão preenchidos com os dados da sessão
        composeRule.onNodeWithText(testSession.startTime.format(dateFormatter)).assertIsDisplayed()
        composeRule.onNodeWithText(testSession.startTime.format(timeFormatter)).assertIsDisplayed()
        composeRule.onNodeWithText(testSession.endTime.format(timeFormatter)).assertIsDisplayed()
        composeRule.onNodeWithText("2").assertIsDisplayed() // Número de vezes que acordou
        composeRule.onNodeWithText("Notas de teste").assertIsDisplayed()
    }

    @Test
    fun testSaveButtonUpdatesSession() {
        // Cria uma sessão de teste
        val testSession = SleepSession(
            id = testSessionId,
            startTime = ZonedDateTime.now().minusHours(8),
            endTime = ZonedDateTime.now(),
            isManualEntry = true,
            notes = "Notas originais",
            wakeDuringNightCount = 1
        )

        // Configura o estado com a sessão para edição
        uiState.value = SleepUiState(
            currentEditingSession = testSession
        )

        // Define a composição
        composeRule.setContent {
            EditManualSleepScreen(
                sessionId = testSessionId,
                viewModel = mockViewModel,
                onNavigateBack = {}
            )
        }

        // Altera o valor do campo de vezes que acordou
        composeRule.onNodeWithText("1").performTextReplacement("3")
        
        // Altera o valor das notas
        composeRule.onNodeWithText("Notas originais").performTextReplacement("Notas atualizadas")

        // Clica no botão Salvar
        composeRule.onNodeWithText("Salvar").performClick()
        
        // Verifica se o método do ViewModel foi chamado
        verify(mockViewModel).updateManualSleepSession(
            eq(testSessionId),
            any(),
            any(),
            eq("Notas atualizadas"),
            eq(3)
        )
    }

    @Test
    fun testNavigateBackOnSuccessfulUpdate() {
        // Configura o callback de navegação
        val navigateBackCalled = mutableListOf(false)
        val onNavigateBack = { navigateBackCalled[0] = true }
        
        // Cria uma sessão de teste
        val testSession = SleepSession(
            id = testSessionId,
            startTime = ZonedDateTime.now().minusHours(8),
            endTime = ZonedDateTime.now(),
            isManualEntry = true
        )

        // Configura o estado para sucesso na atualização
        uiState.value = SleepUiState(
            currentEditingSession = testSession,
            addSessionSuccess = true
        )

        // Define a composição
        composeRule.setContent {
            EditManualSleepScreen(
                sessionId = testSessionId,
                viewModel = mockViewModel,
                onNavigateBack = onNavigateBack
            )
        }

        // Verifica se o método de reset foi chamado
        verify(mockViewModel).resetAddSessionSuccess()
        
        // Verifica se a navegação para trás foi chamada
        assert(navigateBackCalled[0])
    }

    @Test
    fun testClearSessionOnDispose() {
        // Define a composição em um bloco para poder descartá-la
        composeRule.setContent {
            EditManualSleepScreen(
                sessionId = testSessionId,
                viewModel = mockViewModel,
                onNavigateBack = {}
            )
        }
        
        // Força a disposição recriando o conteúdo
        composeRule.setContent { }
        
        // Verifica se o método para limpar a sessão de edição foi chamado
        verify(mockViewModel).clearEditingSession()
    }

    @Test
    fun testInvalidTimeShowsErrorMessage() {
        // Cria uma sessão de teste
        val testSession = SleepSession(
            id = testSessionId,
            startTime = ZonedDateTime.now().minusHours(8),
            endTime = ZonedDateTime.now(),
            isManualEntry = true
        )

        // Configura o estado com a sessão para edição e um erro
        uiState.value = SleepUiState(
            currentEditingSession = testSession,
            error = "Horário de fim deve ser posterior ao horário de início"
        )

        // Define a composição
        composeRule.setContent {
            EditManualSleepScreen(
                sessionId = testSessionId,
                viewModel = mockViewModel,
                onNavigateBack = {}
            )
        }

        // Verifica se a mensagem de erro é exibida
        composeRule.onNodeWithText("Horário de fim deve ser posterior ao horário de início").assertIsDisplayed()
    }

    @Test
    fun testDatePickerShowsCorrectDate() {
        // Cria uma sessão de teste com uma data específica
        val testDate = ZonedDateTime.now().minusDays(5)
        val testSession = SleepSession(
            id = testSessionId,
            startTime = testDate.minusHours(8),
            endTime = testDate,
            isManualEntry = true
        )

        // Configura o estado com a sessão para edição
        uiState.value = SleepUiState(
            currentEditingSession = testSession
        )

        // Define a composição
        composeRule.setContent {
            EditManualSleepScreen(
                sessionId = testSessionId,
                viewModel = mockViewModel,
                onNavigateBack = {}
            )
        }

        // Verifica se a data correta é exibida
        composeRule.onNodeWithText(testSession.startTime.format(dateFormatter)).assertIsDisplayed()
    }
}
