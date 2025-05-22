package com.example.sleepadvisor.presentation.screens.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sleepadvisor.domain.model.NapAnalysis
import com.example.sleepadvisor.domain.model.SleepAdvice
import com.example.sleepadvisor.domain.model.SleepQualityAnalysis
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepTrendAnalysis
import com.example.sleepadvisor.presentation.viewmodel.SleepAnalysisUiState
import com.example.sleepadvisor.presentation.viewmodel.SleepAnalysisViewModel
import java.time.Duration
import java.time.format.DateTimeFormatter

/**
 * Tela de análise detalhada de sono.
 * Exibe análises de qualidade, tendências e sonecas, além de recomendações personalizadas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepAnalysisScreen(
    viewModel: SleepAnalysisViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSessionDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Log do estado da UI para depuração
    android.util.Log.d("SleepAnalysisScreen", "UI State: isLoading=${uiState.isLoading}, emptySessions=${uiState.emptySessions}, error=${uiState.error}")
    android.util.Log.d("SleepAnalysisScreen", "Análises disponíveis: qualityAnalysis=${uiState.qualityAnalysis != null}, trendAnalysis=${uiState.trendAnalysis != null}, recommendations=${uiState.recommendations != null}")
    uiState.recommendations?.let { recommendations ->
        android.util.Log.d("SleepAnalysisScreen", "Recomendações: prioritárias=${recommendations.priorityRecommendations.size}, gerais=${recommendations.generalRecommendations.size}")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análise Avançada de Sono") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Botão para filtrar apenas sonecas
                    IconButton(
                        onClick = { viewModel.showNapsOnly(!uiState.showNapsOnly) }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NightsStay,
                            contentDescription = "Filtrar sonecas",
                            tint = if (uiState.showNapsOnly) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    
                    // Botão para selecionar período
                    IconButton(onClick = { /* Abrir seletor de período */ }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Selecionar período")
                    }
                }
            )
        }
    ) { paddingValues ->
        SleepAnalysisContent(
            uiState = uiState,
            onSessionClick = { session -> 
                viewModel.analyzeSession(session)
                onNavigateToSessionDetail(session.id)
            },
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        )
    }
}

/**
 * Conteúdo principal da tela de análise de sono.
 */
@Composable
fun SleepAnalysisContent(
    uiState: SleepAnalysisUiState,
    onSessionClick: (SleepSession) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.emptySessions) {
            EmptyAnalysisMessage(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.error != null) {
            ErrorMessage(
                message = uiState.error,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Seção de recomendações de IA
                uiState.aiAdvice?.let { aiAdvice ->
                    item {
                        AIRecommendationsCard(sleepAdvice = aiAdvice)
                    }
                }
                
                // Seção de recomendações prioritárias
                uiState.recommendations?.let { recommendations ->
                    item {
                        PriorityRecommendationsCard(recommendations = recommendations)
                    }
                }
                
                // Seção de análise de tendências
                uiState.trendAnalysis?.let { trendAnalysis ->
                    item {
                        SleepTrendAnalysisCard(trendAnalysis = trendAnalysis)
                    }
                }
                
                // Seção de análise de qualidade da última sessão
                if (uiState.lastSession != null && uiState.qualityAnalysis != null) {
                    item {
                        LastSessionQualityCard(
                            session = uiState.lastSession,
                            qualityAnalysis = uiState.qualityAnalysis,
                            onClick = { onSessionClick(uiState.lastSession) }
                        )
                    }
                }
                
                // Seção de sonecas (se o filtro estiver ativado ou se houver sonecas)
                if (uiState.showNapsOnly && uiState.napAnalyses.isNotEmpty()) {
                    item {
                        Text(
                            text = "Sonecas (${uiState.napAnalyses.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    items(uiState.napAnalyses) { napAnalysis ->
                        NapAnalysisCard(
                            napAnalysis = napAnalysis,
                            onClick = { onSessionClick(napAnalysis.session) }
                        )
                    }
                } 
                // Sessões de sono (se o filtro de sonecas não estiver ativado)
                else if (!uiState.showNapsOnly) {
                    item {
                        Text(
                            text = "Histórico de Sono (${uiState.sessions.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    items(uiState.sessions) { session ->
                        SessionSummaryCard(
                            session = session,
                            onClick = { onSessionClick(session) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mensagem exibida quando não há sessões de sono registradas.
 */
@Composable
fun EmptyAnalysisMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.NightsStay,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Nenhum dado de sono registrado",
            style = MaterialTheme.typography.titleLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Registre seu sono por alguns dias para ver análises detalhadas.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * Mensagem de erro exibida quando ocorre um problema ao carregar os dados.
 */
@Composable
fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.NightsStay,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Erro ao carregar dados",
            style = MaterialTheme.typography.titleLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
