package com.example.sleepadvisor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sleepadvisor.domain.model.SleepStageType
import com.example.sleepadvisor.ui.theme.LocalSpacing

/**
 * Item de menu personalizado para o drawer
 */
@Composable
fun DrawerItem(
    text: String,
    icon: ImageVector,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = if (selected) colors.primaryContainer else Color.Transparent,
        shape = MaterialTheme.shapes.small,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
            )
        }
    }
}

/**
 * Indicador de progresso circular com valor
 */
@Composable
fun CircularProgressWithValue(
    value: Float,
    maxValue: Float = 100f,
    size: Dp = 120.dp,
    strokeWidth: Dp = 8.dp,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    backgroundColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit = {}
) {
    val progress = (value / maxValue).coerceIn(0f, 1f)
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        // Círculo de fundo
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(size),
            color = backgroundColor,
            strokeWidth = strokeWidth
        )
        
        // Progresso
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(size),
            color = color,
            strokeWidth = strokeWidth,
            strokeCap = StrokeCap.Round
        )
        
        // Conteúdo central
        content()
    }
}

/**
 * Item de informação com ícone
 */
@Composable
fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(spacing.medium))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Barra de progresso personalizada
 */
@Composable
fun CustomProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    backgroundColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 8.dp,
    cornerRadius: Dp = 4.dp
) {
    val progressCoerced = progress.coerceIn(0f, 1f)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundColor, MaterialTheme.shapes.small)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progressCoerced)
                .fillMaxHeight()
                .background(color, MaterialTheme.shapes.small)
        )
    }
}

/**
 * Toggle personalizado com ícone
 */
@Composable
fun IconToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkedIcon: ImageVector,
    uncheckedIcon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedContentDescription: String? = null,
    uncheckedContentDescription: String? = null,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    IconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled
    ) {
        Icon(
            imageVector = if (checked) checkedIcon else uncheckedIcon,
            contentDescription = if (checked) checkedContentDescription else uncheckedContentDescription,
            tint = tint
        )
    }
}

/**
 * Badge personalizado
 */
@Composable
fun CustomBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimary,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.small
) {
    Surface(
        color = color,
        shape = shape,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Exibe uma mensagem de erro estilizada
 */
@Composable
fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    val spacing = LocalSpacing.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(spacing.medium))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        onRetry?.let { retry ->
            Spacer(modifier = Modifier.height(spacing.medium))
            Button(onClick = retry) {
                Text("Tentar novamente")
            }
        }
    }
}

/**
 * Exibe um estado de carregamento
 */
@Composable
fun LoadingState(
    message: String = "Carregando...",
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(spacing.medium))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Exibe um estado vazio
 */
@Composable
fun EmptyState(
    message: String,
    icon: ImageVector = Icons.Default.NightlightRound,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    val spacing = LocalSpacing.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(spacing.medium))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        action?.let { 
            Spacer(modifier = Modifier.height(spacing.medium))
            it()
        }
    }
}
