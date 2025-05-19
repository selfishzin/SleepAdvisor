package com.example.sleepadvisor.presentation.screens.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleepadvisor.domain.model.NapAnalysis
import com.example.sleepadvisor.domain.model.SleepQualityAnalysis
import com.example.sleepadvisor.domain.model.SleepRecommendations
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepTrendAnalysis
import java.time.format.DateTimeFormatter

/**
 * Card que exibe as recomendações prioritárias de sono.
 */
@Composable
fun PriorityRecommendationsCard(
    recommendations: SleepRecommendations,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Título do card
            Text(
                text = "Recomendações Personalizadas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Recomendações prioritárias
            recommendations.priorityRecommendations.forEach { recommendation ->
                PriorityRecommendationItem(recommendation = recommendation)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Horários ideais
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IdealTimeItem(
                    icon = Icons.Outlined.Bedtime,
                    label = "Dormir",
                    time = recommendations.idealBedtime,
                    modifier = Modifier.weight(1f)
                )
                
                IdealTimeItem(
                    icon = Icons.Outlined.WbSunny,
                    label = "Acordar",
                    time = recommendations.idealWakeTime,
                    modifier = Modifier.weight(1f)
                )
                
                IdealTimeItem(
                    icon = Icons.Outlined.NightsStay,
                    label = "Soneca",
                    time = recommendations.idealNapTime,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Fato científico
            ScientificFactItem(fact = recommendations.scientificFact)
        }
    }
}

/**
 * Item que exibe uma recomendação prioritária.
 */
@Composable
fun PriorityRecommendationItem(
    recommendation: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = recommendation,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Item que exibe um horário ideal.
 */
@Composable
fun IdealTimeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    time: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = time,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Item que exibe um fato científico sobre o sono.
 */
@Composable
fun ScientificFactItem(
    fact: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Science,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = fact,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Card que exibe a análise de tendências de sono.
 */
@Composable
fun SleepTrendAnalysisCard(
    trendAnalysis: SleepTrendAnalysis,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Título do card
            Text(
                text = "Tendências de Sono",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tendência geral
            TrendItem(
                label = "Tendência Geral",
                value = trendAnalysis.overallTrend
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Consistência
            ConsistencyScoreItem(
                score = trendAnalysis.consistencyScore,
                level = trendAnalysis.consistencyLevel
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Duração média
            val hours = trendAnalysis.averageSleepDuration.toHours()
            val minutes = trendAnalysis.averageSleepDuration.toMinutesPart()
            TrendItem(
                label = "Duração Média",
                value = "${hours}h${minutes}min"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Horários médios
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TrendTimeItem(
                    label = "Horário Médio de Dormir",
                    time = trendAnalysis.averageBedtime,
                    modifier = Modifier.weight(1f)
                )
                
                TrendTimeItem(
                    label = "Horário Médio de Acordar",
                    time = trendAnalysis.averageWakeTime,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Comparação dias de semana vs. fim de semana
            TrendItem(
                label = "Dias de Semana vs. Fim de Semana",
                value = trendAnalysis.weekdayVsWeekendAnalysis
            )
        }
    }
}

/**
 * Item que exibe uma tendência de sono.
 */
@Composable
fun TrendItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Item que exibe a pontuação de consistência.
 */
@Composable
fun ConsistencyScoreItem(
    score: Int,
    level: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Consistência",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Barra de progresso
            LinearProgressIndicator(
                progress = score / 100f,
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    score >= 75 -> Color(0xFF4CAF50) // Verde
                    score >= 50 -> Color(0xFFFFC107) // Amarelo
                    else -> Color(0xFFF44336) // Vermelho
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Pontuação e nível
            Text(
                text = "$score ($level)",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Item que exibe um horário médio.
 */
@Composable
fun TrendTimeItem(
    label: String,
    time: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = time,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Card que exibe a análise de qualidade da última sessão de sono.
 */
@Composable
fun LastSessionQualityCard(
    session: SleepSession,
    qualityAnalysis: SleepQualityAnalysis,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabeçalho com data e pontuação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Última Noite",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = dateFormatter.format(session.startTime.atZone(java.time.ZoneId.systemDefault())),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                SleepQualityScore(
                    score = qualityAnalysis.score,
                    label = qualityAnalysis.qualityLabel
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Duração e eficiência
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SleepMetricItem(
                    label = "Duração",
                    value = "${session.duration.toHours()}h${session.duration.toMinutesPart()}min",
                    icon = Icons.Outlined.AccessTime,
                    modifier = Modifier.weight(1f)
                )
                
                SleepMetricItem(
                    label = "Eficiência",
                    value = "${session.efficiency.toInt()}%",
                    icon = Icons.Outlined.Speed,
                    modifier = Modifier.weight(1f)
                )
                
                SleepMetricItem(
                    label = "Despertares",
                    value = "${session.wakeDuringNightCount}",
                    icon = Icons.Outlined.WbTwilight,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Análise de estágios
            Text(
                text = "Análise de Estágios",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = qualityAnalysis.stageAnalysis,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Botão para ver detalhes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ver detalhes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Componente que exibe a pontuação de qualidade do sono.
 */
@Composable
fun SleepQualityScore(
    score: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    val color = when {
        score >= 80 -> Color(0xFF4CAF50) // Verde
        score >= 60 -> Color(0xFFFFC107) // Amarelo
        else -> Color(0xFFF44336) // Vermelho
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

/**
 * Item que exibe uma métrica de sono.
 */
@Composable
fun SleepMetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Card que exibe um resumo de uma sessão de sono.
 */
@Composable
fun SessionSummaryCard(
    session: SleepSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Data e horários
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = dateFormatter.format(session.startTime.atZone(java.time.ZoneId.systemDefault())),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "${timeFormatter.format(session.startTime.atZone(java.time.ZoneId.systemDefault()))} - ${timeFormatter.format(session.endTime.atZone(java.time.ZoneId.systemDefault()))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Duração e eficiência
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${session.duration.toHours()}h${session.duration.toMinutesPart()}min",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Eficiência: ${session.efficiency.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Ícone de seta
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Card que exibe a análise de uma soneca.
 */
@Composable
fun NapAnalysisCard(
    napAnalysis: NapAnalysis,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val session = napAnalysis.session
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabeçalho com data, horário e qualidade
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Soneca - ${dateFormatter.format(session.startTime.atZone(java.time.ZoneId.systemDefault()))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "${timeFormatter.format(session.startTime.atZone(java.time.ZoneId.systemDefault()))} - ${timeFormatter.format(session.endTime.atZone(java.time.ZoneId.systemDefault()))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                NapQualityBadge(quality = napAnalysis.quality)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Duração
            Text(
                text = "Duração: ${session.duration.toMinutes()} minutos",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Impacto no sono noturno
            Text(
                text = napAnalysis.impactOnNightSleep,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Recomendação principal
            if (napAnalysis.recommendations.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = napAnalysis.recommendations.first(),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Badge que exibe a qualidade de uma soneca.
 */
@Composable
fun NapQualityBadge(
    quality: String,
    modifier: Modifier = Modifier
) {
    val color = when (quality) {
        "Excelente" -> Color(0xFF4CAF50) // Verde
        "Boa" -> Color(0xFF8BC34A) // Verde claro
        "Regular" -> Color(0xFFFFC107) // Amarelo
        else -> Color(0xFFF44336) // Vermelho
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = quality,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
