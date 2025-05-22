package com.example.sleepadvisor.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepStageType
import com.example.sleepadvisor.presentation.screens.sleep.SleepViewModel
import com.example.sleepadvisor.presentation.screens.home.components.SleepSessionCard
import com.example.sleepadvisor.ui.components.SleepCard
import com.example.sleepadvisor.ui.theme.LocalSpacing
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
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

            // Seção de estágios do sono
            item {
                val lastSession = uiState.lastSession
                if (lastSession != null && lastSession.hasValidStages()) {
                    // Garantir que a soma não ultrapasse 100%
                    val total = (lastSession.lightSleepPercentage + lastSession.deepSleepPercentage + lastSession.remSleepPercentage).coerceAtMost(100.0)
                    val awake = (100.0 - total).coerceIn(0.0, 100.0)
                    
                    SleepStagesCard(
                        lightSleep = lastSession.lightSleepPercentage.toFloat(),
                        deepSleep = lastSession.deepSleepPercentage.toFloat(),
                        remSleep = lastSession.remSleepPercentage.toFloat(),
                        awake = awake.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Mostrar mensagem quando não houver dados de estágios
                    SleepCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { /* Nada a fazer */ }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Estágios do Sono",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Nenhum dado de estágios de sono disponível para a última sessão.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (lastSession == null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { onNavigateToManualEntry(null) }) {
                                    Text("Registrar sono")
                                }
                            }
                        }
                    }
                }
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
                    
                    // Score de qualidade do sono (simplificado)
                    val sleepScore = (session.efficiency ?: 0.8 * 100).toInt()
                    SleepScoreIndicator(
                        score = sleepScore,
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
                        imageVector = Icons.Default.ArrowForward,
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
private fun SleepStagesCard(
    lightSleep: Float,
    deepSleep: Float,
    remSleep: Float,
    awake: Float,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val total = (lightSleep + deepSleep + remSleep + awake).coerceAtLeast(1f) // Evitar divisão por zero
    
    // Normalizar valores para garantir que a soma seja 100%
    val normalizedLight = (lightSleep / total * 100).coerceIn(0f, 100f)
    val normalizedDeep = (deepSleep / total * 100).coerceIn(0f, 100f)
    val normalizedRem = (remSleep / total * 100).coerceIn(0f, 100f)
    val normalizedAwake = (awake / total * 100).coerceIn(0f, 100f)
    
    SleepCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Estágios do sono",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(spacing.medium))
            
            // Gráfico de barras simplificado
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium)
            ) {
                // Legenda e porcentagem para cada estágio
                SleepStageBar(
                    label = "Sono profundo",
                    percentage = normalizedDeep,
                    color = Color(0xFF4CAF50), // Verde
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SleepStageBar(
                    label = "Sono REM",
                    percentage = normalizedRem,
                    color = Color(0xFF2196F3), // Azul
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SleepStageBar(
                    label = "Sono leve",
                    percentage = normalizedLight,
                    color = Color(0xFFFFC107), // Âmbar
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SleepStageBar(
                    label = "Acordado",
                    percentage = normalizedAwake,
                    color = Color(0xFF9E9E9E), // Cinza
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(spacing.medium))
            
            // Legenda
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                SleepStageLegend(
                    type = SleepStageType.LIGHT,
                    percentage = lightSleep,
                    color = MaterialTheme.colorScheme.secondary
                )
                SleepStageLegend(
                    type = SleepStageType.DEEP,
                    percentage = deepSleep,
                    color = MaterialTheme.colorScheme.primary
                )
                SleepStageLegend(
                    type = SleepStageType.REM,
                    percentage = remSleep,
                    color = MaterialTheme.colorScheme.tertiary
                )
                SleepStageLegend(
                    type = SleepStageType.AWAKE,
                    percentage = awake,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SleepStageBar(
    label: String,
    percentage: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Cor indicativa do estágio
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, CircleShape)
        )
        
        // Nome do estágio e porcentagem
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(100.dp)
        )
        
        // Barra de progresso
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = percentage / 100f)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
        
        // Porcentagem
        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun SleepStageLegend(
    type: SleepStageType,
    percentage: Float,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = type.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SleepScoreIndicator(
    score: Int,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val color = when {
        score >= 85 -> MaterialTheme.colorScheme.primary
        score >= 70 -> MaterialTheme.colorScheme.secondary
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
            text = "$score",
            style = MaterialTheme.typography.headlineLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
