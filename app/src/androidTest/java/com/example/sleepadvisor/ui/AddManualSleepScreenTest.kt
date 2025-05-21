package com.example.sleepadvisor.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sleepadvisor.presentation.screens.sleep.AddManualSleepScreen
import com.example.sleepadvisor.presentation.screens.sleep.SleepUiState
import com.example.sleepadvisor.presentation.screens.sleep.SleepViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class AddManualSleepScreenTest {

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
    fun testAddScreenShowsCorrectFields() {
        // Define a composição
        composeRule.setContent {
            AddManualSleepScreen(
                viewModel = mockViewModel,
                onNavigateBack = {}
            )
        }

        // Verifica se os campos estão presentes
        composeRule.onNodeWithText("Data").assertIsDisplayed()
        composeRule.onNodeWithText("Hora de dormir").assertIsDisplayed()
        composeRule.onNodeWithText("Hora de acordar").assertIsDisplayed()
        composeRule.onNodeWithText("Vezes que acordou durante a noite").assertIsDisplayed()
        composeRule.onNodeWithText("Anotações (opcional)").assertIsDisplayed()
        composeRule.onNodeWithText("Salvar").assertIsDisplayed()
    }

    @Test
    fun testDatePickerDialogAppears() {
        // Define a composição
        composeRule.setContent {
            AddManualSleepScreen(
                viewModel = mockViewModel,
                onNavigateBack = {}
            )
        }

        // Clica no campo de data
        composeRule.onNodeWithText("Data").performClick()

        // Verifica se o seletor de data aparece
        composeRule.onNodeWithText("Selecione a data").assertIsDisplayed()
        
        // Clica em cancelar
        composeRule.onNodeWithText("Cancelar").performClick()
    }

    @Test
    fun testTimePickerDialogAppears() {
        // Define a composição
        composeRule.setContent {
            AddManualSleepScreen(
                viewModel = mockViewModel,
                onNavigateBack = {}
            )
        }

        // Clica no campo de hora de dormir
        composeRule.onNodeWithText("Hora de dormir").performClick()

        // Verifica se o seletor de hora aparece
        composeRule.onNodeWithText("Selecione a hora").assertIsDisplayed()
        
        // Clica em cancelar
        composeRule.onNodeWithText("Cancelar").performClick()
    }

    @Test
    fun testWakeCountValidation() {
        // Define a composição
        composeRule.setContent {
            AddManualSleepScreen(
                viewModel = mockViewModel,
                onNavigateBack = {}
            )
        }

        // Tenta inserir um valor inválido no campo de vezes que acordou
        composeRule.onNodeWithText("0").performTextReplacement("abc")
        
        // Verifica se o campo mantém um valor válido (texto não numérico é ignorado)
        composeRule.onNode(hasText("0")).assertExists()
        
        // Insere um valor válido
        composeRule.onNodeWithText("0").performTextReplacement("3")
        
        // Verifica se o valor foi aceito
        composeRule.onNode(hasText("3")).assertExists()
    }

    @Test
    fun testSaveButtonInteraction() {
        // Define a composição
        composeRule.setContent {
            AddManualSleepScreen(
                viewModel = mockViewModel,
                onNavigateBack = {}
            )
        }

        // Preenche os campos necessários
        // (Como os campos têm valores padrão, não precisamos preenchê-los para este teste)
        
        // Clica no botão Salvar
        composeRule.onNodeWithText("Salvar").performClick()
        
        // Verifica se o método do ViewModel foi chamado
        verify(mockViewModel).addManualSleepSession(
            any(ZonedDateTime::class.java),
            any(ZonedDateTime::class.java),
            anyString(),
            anyInt()
        )
    }

    @Test
    fun testDuplicateEntryDialogAppears() {
        // Configura o estado para mostrar o diálogo de entrada duplicada
        uiState.value = SleepUiState(
            showDuplicateEntryDialog = true
        )

        // Define a composição
        composeRule.setContent {
            AddManualSleepScreen(
                viewModel = mockViewModel,
                onNavigateBack = {}
            )
        }

        // Verifica se o diálogo é exibido
        composeRule.onNodeWithText("Registro existente").assertIsDisplayed()
        composeRule.onNodeWithText("Já existe um registro para este dia. Deseja substituí-lo?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").assertIsDisplayed()
        composeRule.onNodeWithText("Substituir").assertIsDisplayed()
    }

    @Test
    fun testConfirmDuplicateEntry() {
        // Configura o estado para mostrar o diálogo de entrada duplicada
        uiState.value = SleepUiState(
            showDuplicateEntryDialog = true
        )

        // Define a composição
        composeRule.setContent {
            AddManualSleepScreen(
                viewModel = mockViewModel,
                onNavigateBack = {}
            )
        }

        // Clica no botão Substituir
        composeRule.onNodeWithText("Substituir").performClick()
        
        // Verifica se o método do ViewModel foi chamado
        verify(mockViewModel).confirmOverwriteSleepSession()
    }

    @Test
    fun testCancelDuplicateEntry() {
        // Configura o estado para mostrar o diálogo de entrada duplicada
        uiState.value = SleepUiState(
            showDuplicateEntryDialog = true
        )

        // Define a composição
        composeRule.setContent {
            AddManualSleepScreen(
                viewModel = mockViewModel,
                onNavigateBack = {}
            )
        }

        // Clica no botão Cancelar
        composeRule.onNodeWithText("Cancelar").performClick()
        
        // Verifica se o método do ViewModel foi chamado
        verify(mockViewModel).cancelOverwriteSleepSession()
    }

    @Test
    fun testNavigateBackOnSuccessfulSave() {
        // Configura o callback de navegação
        val navigateBackCalled = mutableListOf(false)
        val onNavigateBack = { navigateBackCalled[0] = true }
        
        // Configura o estado para sucesso na adição
        uiState.value = SleepUiState(
            addSessionSuccess = true
        )

        // Define a composição
        composeRule.setContent {
            AddManualSleepScreen(
                viewModel = mockViewModel,
                onNavigateBack = onNavigateBack
            )
        }

        // Verifica se o método de reset foi chamado
        verify(mockViewModel).resetAddSessionSuccess()
        
        // Verifica se a navegação para trás foi chamada
        assert(navigateBackCalled[0])
    }
}
