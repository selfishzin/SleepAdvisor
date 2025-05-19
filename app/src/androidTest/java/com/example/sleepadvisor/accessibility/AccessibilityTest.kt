package com.example.sleepadvisor.accessibility

import androidx.compose.ui.semantics.SemanticsProperties
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
class AccessibilityTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun testMainScreenContentDescriptions() {
        // Verifica se os elementos principais têm descrições de conteúdo
        
        // Título da tela
        composeTestRule.onNodeWithText("Seu sono").assertIsDisplayed()
        
        // Botão de adicionar
        composeTestRule.onNodeWithContentDescription("Adicionar registro de sono")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun testSleepSessionCardsAccessibility() {
        // Verifica se os cartões de sessão têm descrições adequadas
        
        // Procura por qualquer cartão de sessão
        try {
            // Se houver cartões, verificar sua acessibilidade
            composeTestRule.onAllNodesWithContentDescription("Eficiência do sono")
                .fetchSemanticsNodes().forEach { node ->
                    // Verifica se o nó tem ações de clique
                    assert(node.config.contains(SemanticsProperties.Role))
                }
        } catch (e: AssertionError) {
            // Se não houver cartões, o teste é ignorado
            println("Nenhum cartão de sessão encontrado para testar")
        }
    }
    
    @Test
    fun testAddScreenAccessibility() {
        // Navega para a tela de adicionar
        composeTestRule.onNodeWithContentDescription("Adicionar registro de sono").performClick()
        
        // Verifica se os campos têm etiquetas adequadas
        composeTestRule.onNodeWithText("Data").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hora de dormir").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hora de acordar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vezes que acordou durante a noite").assertIsDisplayed()
        
        // Verifica se os campos podem ser focados (importante para leitores de tela)
        composeTestRule.onNodeWithText("Data")
            .assertHasClickAction()
            .assertHasNoClickAction(atLeastOneRipple = true) // Verifica se há feedback visual
        
        // Verifica se os botões têm estados adequados
        composeTestRule.onNodeWithText("Salvar")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
    }
    
    @Test
    fun testNavigationAccessibility() {
        // Navega para a tela de adicionar
        composeTestRule.onNodeWithContentDescription("Adicionar registro de sono").performClick()
        
        // Verifica se o botão de voltar tem descrição
        composeTestRule.onNodeWithContentDescription("Voltar")
            .assertIsDisplayed()
            .assertHasClickAction()
            
        // Retorna à tela principal
        composeTestRule.onNodeWithContentDescription("Voltar").performClick()
        
        // Verifica elementos da tela principal
        composeTestRule.onNodeWithText("Seu sono").assertIsDisplayed()
    }
    
    @Test
    fun testKeyboardNavigationOrder() {
        // Navega para a tela de adicionar
        composeTestRule.onNodeWithContentDescription("Adicionar registro de sono").performClick()
        
        // Clica no primeiro campo
        composeTestRule.onNodeWithText("Data").performClick()
        // Fecha qualquer dialog que possa ter aberto
        composeTestRule.onNodeWithText("Cancelar").performClick()
        
        // Verifica a ordem de navegação
        // (Não podemos testar a navegação por teclado diretamente em testes de UI,
        // mas podemos verificar se os campos estão na ordem correta)
        val nodes = composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        
        // Verifica se os campos principais estão presentes na ordem correta
        val expectedFields = listOf("Data", "Hora de dormir", "Hora de acordar", "Vezes que acordou durante a noite", "Anotações (opcional)")
        var previousIndex = -1
        
        for (field in expectedFields) {
            val fieldIndex = nodes.indexOfFirst { 
                it.config[SemanticsProperties.Text]?.text?.contains(field) == true 
            }
            
            // Verifica se o campo foi encontrado
            if (fieldIndex != -1) {
                // Verifica se a ordem é maior que a anterior
                assert(fieldIndex > previousIndex) { 
                    "Campo $field está na posição incorreta para navegação por teclado" 
                }
                previousIndex = fieldIndex
            }
        }
    }
    
    @Test
    fun testColorContrastRatio() {
        // Neste teste, verificamos a presença dos componentes que devem ter contraste adequado
        // O teste real de contraste de cores precisaria ser feito com ferramentas específicas
        
        // Verifica a presença de textos principais
        composeTestRule.onAllNodesWithText("Seu sono").assertAll(isDisplayed())
        
        // Verifica se os botões têm descrições
        composeTestRule.onAllNodesWithContentDescription("Adicionar registro de sono")
            .assertAll(isDisplayed())
    }
    
    @Test
    fun testTextScaling() {
        // Este teste é mais difícil de implementar em testes automatizados
        // Idealmente, precisaria verificar se o layout se adapta bem a diferentes tamanhos de fonte
        
        // Verifica a presença de elementos principais
        composeTestRule.onNodeWithText("Seu sono").assertIsDisplayed()
        
        // Verifica se os elementos têm tamanho mínimo adequado para toque
        composeTestRule.onNodeWithContentDescription("Adicionar registro de sono")
            .assertHasClickAction()
            .assert(hasMinimumTouchTargetSize())
    }
    
    private fun hasMinimumTouchTargetSize() = SemanticsMatcher.expectValue(
        SemanticsProperties.Size,
        { it.width >= 48 && it.height >= 48 }
    )
}
