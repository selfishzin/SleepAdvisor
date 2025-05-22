package com.example.sleepadvisor.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Espaçamentos padronizados para o aplicativo
 */
@Immutable
data class Spacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val extraExtraLarge: Dp = 48.dp,
    val extraExtraExtraLarge: Dp = 64.dp
)

/**
 * Fornece acesso aos espaçamentos em qualquer lugar do aplicativo
 */
val LocalSpacing = staticCompositionLocalOf { Spacing() }
