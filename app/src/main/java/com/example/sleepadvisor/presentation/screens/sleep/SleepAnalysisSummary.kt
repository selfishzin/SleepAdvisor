package com.example.sleepadvisor.presentation.screens.sleep

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
import com.example.sleepadvisor.domain.model.SleepRecommendations
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepTrendAnalysis
import java.time.format.DateTimeFormatter

/**
 * Componente que exibe um resumo compacto da análise de sono na tela principal.
 * Mostra apenas as informações mais importantes e um link para a tela detalhada.
 */
@Composable
fun SleepAnalysisSummary(
    lastSession: SleepSession?,
    weeklyScore: Int?,
    weeklyQuality: String?,
    recommendations: SleepRecommendations?,
    trendAnalysis: SleepTrendAnalysis?,
    onViewDetailedAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabeçalho com título e botão para ver análise detalhada
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Análise Semanal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                TextButton(
                    onClick = onViewDetailedAnalysis,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Ver detalhes",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Pontuação semanal e qualidade
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pontuação semanal
                if (weeklyScore != null && weeklyQuality != null) {
                    WeeklySleepQualityScore(
                        score = weeklyScore,
                        quality = weeklyQuality,
                        modifier = Modifier.weight(0.3f)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .weight(0.3f)
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sem dados suficientes",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // Recomendação prioritária
                if (recommendations != null && recommendations.priorityRecommendations.isNotEmpty()) {
                    TopRecommendation(
                        recommendation = recommendations.priorityRecommendations.first(),
                        modifier = Modifier.weight(0.7f)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .weight(0.7f)
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Registre seu sono por alguns dias para receber recomendações personalizadas.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Horários ideais
            if (recommendations != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IdealTimeItem(
                        icon = Icons.Outlined.Bedtime,
                        label = "Dormir",
                        time = recommendations.idealBedtime
                    )
                    
                    IdealTimeItem(
                        icon = Icons.Outlined.WbSunny,
                        label = "Acordar",
                        time = recommendations.idealWakeTime
                    )
                    
                    IdealTimeItem(
                        icon = Icons.Outlined.NightsStay,
                        label = "Soneca",
                        time = recommendations.idealNapTime
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Botão para ver análise detalhada
            Button(
                onClick = onViewDetailedAnalysis,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver Análise Detalhada")
            }
        }
    }
}

/**
 * Componente que exibe a pontuação semanal de qualidade do sono.
 */
@Composable
fun WeeklySleepQualityScore(
    score: Int,
    quality: String,
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
        Text(
            text = "Qualidade Média",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = quality,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Componente que exibe a recomendação prioritária.
 */
@Composable
fun TopRecommendation(
    recommendation: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(start = 16.dp)
    ) {
        Text(
            text = "Recomendação Prioritária",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = recommendation,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Componente que exibe um horário ideal.
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
            modifier = Modifier.size(20.dp)
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = time,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
