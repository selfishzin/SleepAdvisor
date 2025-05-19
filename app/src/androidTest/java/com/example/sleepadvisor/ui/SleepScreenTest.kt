package com.example.sleepadvisor.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepStage
import com.example.sleepadvisor.domain.model.SleepStageType
import com.example.sleepadvisor.domain.service.SleepAdvice
import com.example.sleepadvisor.presentation.screens.sleep.SleepScreen
import com.example.sleepadvisor.presentation.screens.sleep.SleepUiState
import com.example.sleepadvisor.presentation.screens.sleep.SleepViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Duration
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SleepScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var mockViewModel: SleepViewModel
    private val uiState = MutableStateFlow(SleepUiState())

    @Before
    fun setup() {
        // Mock do ViewModel
        mockViewModel = mock(SleepViewModel::class.java)
        `when`(mockViewModel.uiState).thenReturn(uiState as StateFlow<SleepUiState>)
    }

    @Test
    fun testEmptyStateDisplaysCorrectMessage() {
        // Configura o estado vazio
        uiState.value = SleepUiState(
            sleepSessions = emptyList(),
            isLoading = false,
            hasPermissions = true
        )

        // Define a composição
        composeRule.setContent {
            SleepScreen(
                viewModel = mockViewModel,
                onNavigateToAddSleep = {},
                onNavigateToEditSleep = {},
                onRequestPermissions = {}
            )
        }

        // Verifica se a mensagem de estado vazio é exibida
        composeRule.onNodeWithText("Nenhum dado de sono encontrado").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Adicionar registro de sono").assertIsDisplayed()
    }

    @Test
    fun testLoadingStateDisplaysProgressIndicator() {
        // Configura o estado de carregamento
        uiState.value = SleepUiState(
            isLoading = true,
            hasPermissions = true
        )

        // Define a composição
        composeRule.setContent {
            SleepScreen(
                viewModel = mockViewModel,
                onNavigateToAddSleep = {},
                onNavigateToEditSleep = {},
                onRequestPermissions = {}
            )
        }

        // Verifica se o indicador de progresso é exibido
        composeRule.onNodeWithContentDescription("Carregando dados").assertIsDisplayed()
    }

    @Test
    fun testSleepSessionsAreDisplayedCorrectly() {
        // Cria sessões de teste
        val sessions = listOf(
            createTestSleepSession(
                startTime = ZonedDateTime.now().minusDays(1),
                endTime = ZonedDateTime.now().minusDays(1).plusHours(8),
                efficiency = 85.0
            ),
            createTestSleepSession(
                startTime = ZonedDateTime.now().minusDays(2),
                endTime = ZonedDateTime.now().minusDays(2).plusHours(7),
                efficiency = 75.0
            )
        )

        // Configura o estado com dados
        uiState.value = SleepUiState(
            sleepSessions = sessions,
            isLoading = false,
            hasPermissions = true
        )

        // Define a composição
        composeRule.setContent {
            SleepScreen(
                viewModel = mockViewModel,
                onNavigateToAddSleep = {},
                onNavigateToEditSleep = {},
                onRequestPermissions = {}
            )
        }

        // Verifica se os cartões de sessão são exibidos
        composeRule.onNodeWithText("85%").assertIsDisplayed()
        composeRule.onNodeWithText("75%").assertIsDisplayed()
        
        // Verifica se o botão de adicionar está visível
        composeRule.onNodeWithContentDescription("Adicionar registro de sono").assertIsDisplayed()
    }

    @Test
    fun testAIAdviceIsDisplayedCorrectly() {
        // Cria sessões de teste
        val sessions = listOf(createTestSleepSession())
        
        // Cria conselhos da IA
        val sleepAdvice = SleepAdvice(
            tips = listOf("Mantenha um horário regular de sono", "Evite cafeína à noite"),
            warnings = listOf("Sono insuficiente detectado"),
            positiveReinforcement = "Boa consistência nos horários de dormir!"
        )

        // Configura o estado com dados e conselhos da IA
        uiState.value = SleepUiState(
            sleepSessions = sessions,
            isLoading = false,
            hasPermissions = true,
            sleepAdvice = sleepAdvice,
            aiAdviceLoading = false
        )

        // Define a composição
        composeRule.setContent {
            SleepScreen(
                viewModel = mockViewModel,
                onNavigateToAddSleep = {},
                onNavigateToEditSleep = {},
                onRequestPermissions = {}
            )
        }

        // Verifica se os conselhos da IA são exibidos
        composeRule.onNodeWithText("Mantenha um horário regular de sono").assertIsDisplayed()
        composeRule.onNodeWithText("Evite cafeína à noite").assertIsDisplayed()
        composeRule.onNodeWithText("Sono insuficiente detectado").assertIsDisplayed()
        composeRule.onNodeWithText("Boa consistência nos horários de dormir!").assertIsDisplayed()
    }

    @Test
    fun testPermissionNotGrantedShowsRequestButton() {
        // Configura o estado sem permissões
        uiState.value = SleepUiState(
            hasPermissions = false,
            isLoading = false
        )

        // Define a composição
        composeRule.setContent {
            SleepScreen(
                viewModel = mockViewModel,
                onNavigateToAddSleep = {},
                onNavigateToEditSleep = {},
                onRequestPermissions = {}
            )
        }

        // Verifica se o botão de solicitar permissões é exibido
        composeRule.onNodeWithText("Solicitar permissões").assertIsDisplayed()
    }

    @Test
    fun testScrollBehaviorWithMultipleSessions() {
        // Cria várias sessões de teste para testar o comportamento de rolagem
        val sessions = (1..10).map { i ->
            createTestSleepSession(
                startTime = ZonedDateTime.now().minusDays(i.toLong()),
                endTime = ZonedDateTime.now().minusDays(i.toLong()).plusHours(8),
                efficiency = 85.0 - i
            )
        }

        // Configura o estado com muitos dados
        uiState.value = SleepUiState(
            sleepSessions = sessions,
            isLoading = false,
            hasPermissions = true
        )

        // Define a composição
        composeRule.setContent {
            SleepScreen(
                viewModel = mockViewModel,
                onNavigateToAddSleep = {},
                onNavigateToEditSleep = {},
                onRequestPermissions = {}
            )
        }

        // Verifica se o primeiro item é exibido
        composeRule.onNodeWithText("85%").assertIsDisplayed()
        
        // Rola para baixo para ver mais itens
        composeRule.onRoot().performScrollToIndex(5)
        
        // Verifica se um item mais abaixo está visível após a rolagem
        composeRule.onNodeWithText("80%").assertIsDisplayed()
    }

    private fun createTestSleepSession(
        id: String = UUID.randomUUID().toString(),
        startTime: ZonedDateTime = ZonedDateTime.now().minusDays(1),
        endTime: ZonedDateTime = ZonedDateTime.now(),
        efficiency: Double = 85.0
    ): SleepSession {
        return SleepSession(
            id = id,
            startTime = startTime,
            endTime = endTime,
            stages = listOf(
                SleepStage(SleepStageType.LIGHT, Duration.ofHours(4)),
                SleepStage(SleepStageType.DEEP, Duration.ofHours(2)),
                SleepStage(SleepStageType.REM, Duration.ofHours(2))
            ),
            isManualEntry = true,
            notes = "Sessão de teste",
            wakeDuringNightCount = 1
        )
    }
}
