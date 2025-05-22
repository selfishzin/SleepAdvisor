package com.example.sleepadvisor.presentation.screens.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sleepadvisor.domain.model.*
import com.example.sleepadvisor.presentation.screens.sleep.SleepStageInfo
import java.time.format.DateTimeFormatter

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
                
                // TODO: Implementar ou importar o componente SleepQualityScore
                Text(
                    text = "${qualityAnalysis.score}/100 - ${qualityAnalysis.qualityLabel}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
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
                    icon = Icons.Outlined.NightsStay,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Análise de estágios
            Text(
                text = "Distribuição dos Estágios do Sono",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Gráfico de estágios de sono (usando o mesmo componente da tela inicial)
            SleepStageInfo(
                lightSleepPercentage = session.lightSleepPercentage,
                deepSleepPercentage = session.deepSleepPercentage,
                remSleepPercentage = session.remSleepPercentage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                isEstimated = session.hasEstimatedStages()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Análise de texto como antes
            Text(
                text = qualityAnalysis.stageAnalysis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Item que exibe uma métrica de sono.
 *
 * @param label Nome da métrica
 * @param value Valor da métrica
 * @param icon Ícone ilustrativo
 * @param modifier Modificador para estilização
 */
@Composable
fun SleepMetricItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
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
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Card que exibe um resumo de uma sessão de sono.
 *
 * @param session Dados da sessão de sono
 * @param onClick Callback chamado quando o card é clicado
 * @param modifier Modificador para estilização
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
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Card que exibe a análise de uma soneca.
 *
 * @param napAnalysis Análise da soneca
 * @param onClick Callback chamado quando o card é clicado
 * @param modifier Modificador para estilização
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
 *
 * @param quality Qualidade da soneca (Excelente, Boa, Regular, etc)
 * @param modifier Modificador para estilização
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
