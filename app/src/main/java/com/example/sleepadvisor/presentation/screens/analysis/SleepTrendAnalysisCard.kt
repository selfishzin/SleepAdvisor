package com.example.sleepadvisor.presentation.screens.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.example.sleepadvisor.domain.model.SleepTrendAnalysis
import com.example.sleepadvisor.presentation.viewmodel.SleepAnalysisUiState
import java.time.ZonedDateTime
import java.time.Duration


/**
 * Componente que exibe a análise de tendências de sono
 * @param sleepUiState Estado atual da UI do sono
 * @param modifier Modificador para estilização
 * @param trendAnalysis Análise de tendências (opcional, pode ser nulo)
 */
@Composable
fun SleepTrendAnalysisCard(
    sleepUiState: SleepAnalysisUiState,
    modifier: Modifier = Modifier,
    trendAnalysis: SleepTrendAnalysis? = null
) {
    // Se não houver análise ou dados suficientes, não exibir o card
    val effectiveTrendAnalysis = trendAnalysis ?: return
    if (sleepUiState.sessions.isEmpty()) return
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Cabeçalho com ícone de tendência
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val (trendIcon, iconRotation, trendColor) = when {
                    effectiveTrendAnalysis.overallTrend.contains("melhorando") -> 
                        Triple(Icons.AutoMirrored.Filled.TrendingUp, 0f, MaterialTheme.colorScheme.tertiary)
                    effectiveTrendAnalysis.overallTrend.contains("piorando") -> 
                        Triple(Icons.AutoMirrored.Filled.TrendingDown, 0f, MaterialTheme.colorScheme.error)
                    else -> 
                        Triple(Icons.AutoMirrored.Filled.TrendingFlat, 0f, MaterialTheme.colorScheme.secondary)
                }
                
                Icon(
                    imageVector = trendIcon,
                    contentDescription = null,
                    tint = trendColor,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(iconRotation)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Análise de Tendências",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Exibir período de análise (últimos 7 dias)
                val endDate = ZonedDateTime.now()
                val startDate = endDate.minusDays(7)
                Text(
                    text = "${startDate.dayOfMonth}/${startDate.monthValue} - ${endDate.dayOfMonth}/${endDate.monthValue}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Estatísticas principais
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Linha 1: Duração Média
                effectiveTrendAnalysis.averageSleepDuration?.let { duration ->
                    StatisticRow(
                        label = "Duração Média",
                        value = formatDuration(duration),
                        trend = effectiveTrendAnalysis.durationTrend ?: "estável",
                        valueColor = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Linha 2: Qualidade do Sono
                effectiveTrendAnalysis.qualityTrend?.let { quality ->
                    StatisticRow(
                        label = "Qualidade Média",
                        value = quality.split(" ").firstOrNull() ?: "",
                        trend = quality,
                        valueColor = when {
                            quality.contains("melhorando") -> MaterialTheme.colorScheme.tertiary
                            quality.contains("piorando") -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
                
                // Linha 3: Consistência
                effectiveTrendAnalysis.consistencyScore?.let { score ->
                    StatisticRow(
                        label = "Consistência",
                        value = "$score%",
                        trend = effectiveTrendAnalysis.consistencyLevel ?: "",
                        valueColor = when (score) {
                            in 80..100 -> MaterialTheme.colorScheme.tertiary
                            in 60..79 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
                
                // Linha 4: Horários médios
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Média de Horários",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            effectiveTrendAnalysis.averageBedtime?.let { bedtime ->
                                Text(
                                    text = "Dormir: $bedtime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                                )
                            }
                            effectiveTrendAnalysis.averageWakeTime?.let { wakeTime ->
                                Text(
                                    text = "Acordar: $wakeTime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }    // Exibir o número de noites analisadas
                Text(
                    text = "${sleepUiState.sessions.size} noites",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Análise de dias úteis vs. fim de semana
            if (!effectiveTrendAnalysis.weekdayVsWeekendAnalysis.isNullOrBlank()) {
                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                
                Text(
                    text = "Análise por Dia da Semana",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = effectiveTrendAnalysis.weekdayVsWeekendAnalysis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f
                )
            }
            
            // Recomendações
            if (effectiveTrendAnalysis.recommendations.isNotEmpty()) {
                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                
                Text(
                    text = "Recomendações para Melhorar o Sono",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    effectiveTrendAnalysis.recommendations.take(3).forEach { recommendation ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = recommendation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Componente de linha para exibir uma estatística com rótulo e valor
 */
@Composable
private fun StatisticRow(
    label: String,
    value: String,
    trend: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                modifier = Modifier.padding(end = 8.dp)
            )
            
            // Exibir tendência se for relevante
            if (trend.isNotBlank() && trend != "estável") {
                val trendIcon = when {
                    trend.contains("aumentando") || trend.contains("melhorando") -> Icons.AutoMirrored.Filled.TrendingUp
                    trend.contains("diminuindo") || trend.contains("piorando") -> Icons.AutoMirrored.Filled.TrendingDown
                    else -> Icons.AutoMirrored.Filled.TrendingFlat
                }
                
                val trendColor = when {
                    trend.contains("aumentando") || trend.contains("melhorando") -> 
                        MaterialTheme.colorScheme.tertiary
                    trend.contains("diminuindo") || trend.contains("piorando") -> 
                        MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.secondary
                }
                
                Icon(
                    imageVector = trendIcon,
                    contentDescription = null,
                    tint = trendColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}



/**
 * Formata uma duração em um formato legível (ex: 7h 30min)
 */
private fun formatDuration(duration: Duration): String {
    val hours = duration.toHours()
    val minutes = duration.minusHours(hours).toMinutes()
    return String.format("%dh %02dmin", hours, minutes)
}
