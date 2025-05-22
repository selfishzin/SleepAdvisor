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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sleepadvisor.domain.model.*
import com.example.sleepadvisor.presentation.screens.sleep.SleepStageInfo
import java.time.format.DateTimeFormatter

// Cores para os estágios do sono
private val DeepSleepColor = Color(0xFF4CAF50) // Verde
private val REMSleepColor = Color(0xFF2196F3)  // Azul
private val LightSleepColor = Color(0xFFFFC107) // Âmbar
private val AwakeColor = Color(0xFF9E9E9E)     // Cinza

// Componente SleepStagesCard simplificado
@Composable
private fun SleepStagesCard(
    lightSleep: Float,
    deepSleep: Float,
    remSleep: Float,
    awake: Float,
    modifier: Modifier = Modifier
) {
    val total = (lightSleep + deepSleep + remSleep + awake).coerceAtLeast(1f) // Evitar divisão por zero
    
    // Normalizar valores para garantir que a soma seja 100%
    val normalizedLight = (lightSleep / total * 100).coerceIn(0f, 100f)
    val normalizedDeep = (deepSleep / total * 100).coerceIn(0f, 100f)
    val normalizedRem = (remSleep / total * 100).coerceIn(0f, 100f)
    val normalizedAwake = (awake / total * 100).coerceIn(0f, 100f)
    
    // Usar Card do Material3 em vez de SleepCard
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Estágios do sono",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Gráfico de barras simplificado
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sono profundo
                SleepStageBar(
                    label = "Sono profundo",
                    percentage = normalizedDeep,
                    color = DeepSleepColor
                )
                
                // Sono REM
                SleepStageBar(
                    label = "Sono REM",
                    percentage = normalizedRem,
                    color = REMSleepColor
                )
                
                // Sono leve
                SleepStageBar(
                    label = "Sono leve",
                    percentage = normalizedLight,
                    color = LightSleepColor
                )
                
                // Acordado
                SleepStageBar(
                    label = "Acordado",
                    percentage = normalizedAwake,
                    color = AwakeColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
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
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Barra de progresso
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
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
            if (session.hasValidStages()) {
                // Garantir que a soma não ultrapasse 100%
                val total = (session.lightSleepPercentage + session.deepSleepPercentage + session.remSleepPercentage).coerceAtMost(100.0)
                val awake = (100.0 - total).coerceIn(0.0, 100.0)
                
                SleepStagesCard(
                    lightSleep = session.lightSleepPercentage.toFloat(),
                    deepSleep = session.deepSleepPercentage.toFloat(),
                    remSleep = session.remSleepPercentage.toFloat(),
                    awake = awake.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                // Mostrar mensagem quando não houver dados de estágios
                Text(
                    text = "Nenhum dado de estágios de sono disponível para esta sessão.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
            
            // Análise de texto
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
