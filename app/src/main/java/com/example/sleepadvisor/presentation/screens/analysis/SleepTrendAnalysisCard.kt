package com.example.sleepadvisor.presentation.screens.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.example.sleepadvisor.domain.model.SleepTrendAnalysis

/**
 * Componente que exibe a análise de tendências de sono
 */
@Composable
fun SleepTrendAnalysisCard(
    trendAnalysis: SleepTrendAnalysis,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Cabeçalho
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(if (trendAnalysis.overallTrend.contains("melhorando")) 0f else 180f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Tendências de Sono",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Estatísticas principais
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Consistência
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${trendAnalysis.consistencyScore}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Consistência",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = trendAnalysis.consistencyLevel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                
                // Duração Média
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = formatDuration(trendAnalysis.averageSleepDuration),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Duração Média",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = trendAnalysis.durationTrend,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            trendAnalysis.durationTrend.contains("aumentando") -> MaterialTheme.colorScheme.tertiary
                            trendAnalysis.durationTrend.contains("diminuindo") -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onTertiaryContainer
                        }
                    )
                }
                
                // Qualidade
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = trendAnalysis.qualityTrend.replace(" ".toRegex(), "\n"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            trendAnalysis.qualityTrend.contains("melhorando") -> MaterialTheme.colorScheme.tertiary
                            trendAnalysis.qualityTrend.contains("piorando") -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onTertiaryContainer
                        },
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Qualidade",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Análise de dias úteis vs. fim de semana
            if (trendAnalysis.weekdayVsWeekendAnalysis.isNotBlank()) {
                Text(
                    text = "Dias Úteis vs. Fim de Semana:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trendAnalysis.weekdayVsWeekendAnalysis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Recomendações
            if (trendAnalysis.recommendations.isNotEmpty()) {
                Text(
                    text = "Sugestões para melhorar:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                trendAnalysis.recommendations.take(3).forEach { recommendation ->
                    Text(
                        text = "• $recommendation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

/**
 * Formata uma duração em um formato legível (horas e minutos)
 */
private fun formatDuration(duration: java.time.Duration): String {
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    return String.format("%dh%02d", hours, minutes)
}
