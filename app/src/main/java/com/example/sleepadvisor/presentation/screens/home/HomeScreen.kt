package com.example.sleepadvisor.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sleepadvisor.presentation.screens.sleep.SleepViewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.sleepadvisor.ui.theme.LocalSpacing
import com.example.sleepadvisor.ui.components.SleepCard
import com.example.sleepadvisor.presentation.screens.home.components.SleepSessionCard
import com.example.sleepadvisor.domain.model.SleepSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun HomeScreen(
    viewModel: SleepViewModel,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToManualEntry: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacing = LocalSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Bem-vindo",
                        style = MaterialTheme.typography.headlineSmall
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ),
                actions = {
                    IconButton(onClick = { /* Abrir configurações */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToManualEntry(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar sono")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = spacing.medium, vertical = spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            // Seção de boas-vindas
            item {
                WelcomeCard()
            }
            // Seção de última noite de sono
            item {
                LastNightCard(
                    session = uiState.lastSession,
                    onAnalyzeClick = onNavigateToAnalysis,
                    onAddSleepClick = { onNavigateToManualEntry(null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Seção de histórico recente
            item {
                Text(
                    text = "Histórico recente",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = spacing.small)
                )
            }

            if (uiState.sleepSessions.isNotEmpty()) {
                items(uiState.sleepSessions.sortedByDescending { it.startTime }.take(3)) { session ->
                    SleepSessionCard(
                        session = session,
                        onEdit = { onNavigateToManualEntry(session.id) },
                        onDelete = { /* TODO: Implementar exclusão */ },
                        onClick = { /* TODO: Navegar para detalhes */ },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                item {
                    Text(
                        text = "Nenhum registro de sono encontrado. Toque no botão + para adicionar um.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.medium)
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard(
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    SleepCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Boa noite!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = "Hora de registrar seu sono e melhorar sua qualidade de vida.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LastNightCard(
    session: SleepSession?,
    onAnalyzeClick: () -> Unit,
    onAddSleepClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    SleepCard(
        modifier = modifier,
        onClick = { if (session != null) onAnalyzeClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Última noite",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (session != null) {
                    Text(
                        text = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(spacing.medium))
            
            if (session != null) {
                // Mostrar dados da última noite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val duration = Duration.between(session.startTime, session.endTime)
                        val hours = duration.toHours()
                        val minutes = duration.toMinutes() % 60
                        Text(
                            text = "${hours}h ${minutes}m",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Duração do sono",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Garante que a eficiência esteja entre 0 e 100
                    val efficiency = session.efficiency.coerceIn(0.0, 100.0)
                    SleepScoreIndicator(
                        score = efficiency.toInt(),
                        size = 80.dp
                    )
                }
                
                Spacer(modifier = Modifier.height(spacing.medium))
                
                // Ação para ver análise detalhada
                TextButton(
                    onClick = onAnalyzeClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Ver análise detalhada")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                // Estado quando não há dados
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nenhum dado de sono registrado para hoje",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(spacing.small))
                    Button(onClick = onAddSleepClick) {
                        Text("Registrar sono")
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepScoreIndicator(
    score: Int,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    // Garante que o score esteja entre 0 e 100
    val safeScore = score.coerceIn(0, 100)
    
    val color = when {
        safeScore >= 85 -> MaterialTheme.colorScheme.primary
        safeScore >= 70 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        // Círculo de fundo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = color.copy(alpha = 0.1f),
                    shape = CircleShape
                )
        )
        
        // Score
        Text(
            text = "$safeScore",
            style = MaterialTheme.typography.headlineLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
