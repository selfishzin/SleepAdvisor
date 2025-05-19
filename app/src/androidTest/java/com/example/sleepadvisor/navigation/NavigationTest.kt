package com.example.sleepadvisor.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sleepadvisor.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testNavigationToAddScreen() {
        // DADO: Estamos na tela principal
        
        // Aguarda a tela principal carregar
        composeTestRule.waitForIdle()
        
        // QUANDO: Clicamos no botão de adicionar
        composeTestRule.onNodeWithContentDescription("Adicionar registro de sono").performClick()
        
        // ENTÃO: Navegamos para a tela de adicionar sono
        composeTestRule.onNodeWithText("Adicionar registro de sono").assertIsDisplayed()
        composeTestRule.onNodeWithText("Data").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hora de dormir").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hora de acordar").assertIsDisplayed()
    }

    @Test
    fun testNavigationBackFromAddScreen() {
        // DADO: Estamos na tela principal e navegamos para a tela de adicionar
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Adicionar registro de sono").performClick()
        
        // Confirma que estamos na tela de adicionar
        composeTestRule.onNodeWithText("Adicionar registro de sono").assertIsDisplayed()
        
        // QUANDO: Pressionamos o botão de voltar
        composeTestRule.onNodeWithContentDescription("Voltar").performClick()
        
        // ENTÃO: Retornamos à tela principal
        composeTestRule.onNodeWithText("Seu sono").assertIsDisplayed()
    }

    @Test
    fun testNavigationToEditScreenAndBack() {
        // NOTA: Este teste assume que já existe pelo menos uma sessão de sono
        // Para um teste completo, você precisaria primeiro adicionar uma sessão
        
        // DADO: Estamos na tela principal com pelo menos uma sessão
        composeTestRule.waitForIdle()
        
        // Se houver sessões, clicamos na primeira para editar
        // Caso contrário, adicionamos uma nova e depois editamos
        val hasSessions = try {
            composeTestRule.onAllNodesWithContentDescription("Editar sessão").fetchSemanticsNodes().isNotEmpty()
        } catch (e: Exception) {
            false
        }
        
        if (!hasSessions) {
            // Adiciona uma sessão primeiro
            composeTestRule.onNodeWithContentDescription("Adicionar registro de sono").performClick()
            composeTestRule.onNodeWithText("Salvar").performClick()
            composeTestRule.waitForIdle()
        }
        
        // QUANDO: Clicamos no botão de editar de uma sessão
        composeTestRule.onNodeWithContentDescription("Editar sessão").performClick()
        
        // ENTÃO: Navegamos para a tela de edição
        composeTestRule.onNodeWithText("Editar registro de sono").assertIsDisplayed()
        
        // QUANDO: Pressionamos o botão de voltar
        composeTestRule.onNodeWithContentDescription("Voltar").performClick()
        
        // ENTÃO: Retornamos à tela principal
        composeTestRule.onNodeWithText("Seu sono").assertIsDisplayed()
    }

    @Test
    fun testFullNavigationFlow() {
        // DADO: Estamos na tela principal
        composeTestRule.waitForIdle()
        
        // QUANDO: Navegamos para adicionar, voltamos, depois editamos, e voltamos
        
        // 1. Navega para adicionar
        composeTestRule.onNodeWithContentDescription("Adicionar registro de sono").performClick()
        composeTestRule.onNodeWithText("Adicionar registro de sono").assertIsDisplayed()
        
        // 2. Salva a sessão
        composeTestRule.onNodeWithText("Salvar").performClick()
        composeTestRule.waitForIdle()
        
        // 3. Voltamos à tela principal
        composeTestRule.onNodeWithText("Seu sono").assertIsDisplayed()
        
        // 4. Edita a sessão recém-criada
        composeTestRule.onNodeWithContentDescription("Editar sessão").performClick()
        composeTestRule.onNodeWithText("Editar registro de sono").assertIsDisplayed()
        
        // 5. Volta à tela principal
        composeTestRule.onNodeWithContentDescription("Voltar").performClick()
        
        // ENTÃO: Todo o fluxo de navegação funciona corretamente
        composeTestRule.onNodeWithText("Seu sono").assertIsDisplayed()
    }
    
    @Test
    fun testPermissionRequestNavigation() {
        // Este teste é mais complicado pois depende do estado real das permissões
        // do dispositivo. Vamos simular o básico.
        
        // DADO: Estamos na tela principal
        composeTestRule.waitForIdle()
        
        // Tenta encontrar o botão de permissões (só estará presente se as permissões não estiverem concedidas)
        val hasPermissionButton = try {
            composeTestRule.onNodeWithText("Solicitar permissões").fetchSemanticsNode().isValid()
        } catch (e: Exception) {
            false
        }
        
        if (hasPermissionButton) {
            // QUANDO: Clicamos no botão de solicitar permissões
            composeTestRule.onNodeWithText("Solicitar permissões").performClick()
            
            // ENTÃO: Devemos navegar para a tela de permissões do Health Connect
            // Nota: Esta parte é difícil de testar completamente pois envolve APIs do sistema
            // e interações com diálogos nativos do Android.
            
            // Aqui testamos apenas se o botão funciona
            composeTestRule.waitForIdle()
        } else {
            // Já temos permissões, verificamos se os dados estão sendo exibidos
            composeTestRule.onNodeWithText("Seu sono").assertIsDisplayed()
        }
    }
}
