package com.example.sleepadvisor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sleepadvisor.ui.theme.LocalSpacing

/**
 * Card estilizado para o SleepAdvisor
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Dp = 2.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable () -> Unit
) {
    val spacing = LocalSpacing.current
    
    Card(
        onClick = { onClick?.invoke() },
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = shape,
        enabled = onClick != null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
        ) {
            content()
        }
    }
}

/**
 * Card estilizado para conteúdo de destaque
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    content: @Composable () -> Unit
) {
    SleepCard(
        modifier = modifier,
        onClick = onClick,
        containerColor = backgroundColor,
        contentColor = contentColor,
        elevation = 0.dp,
        shape = MaterialTheme.shapes.large
    ) {
        content()
    }
}

/**
 * Card estilizado para conteúdo secundário
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    SleepCard(
        modifier = modifier,
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        elevation = 0.dp
    ) {
        content()
    }
}
