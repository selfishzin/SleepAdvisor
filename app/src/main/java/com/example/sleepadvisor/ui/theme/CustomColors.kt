package com.example.sleepadvisor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Cores personalizadas adicionais que não fazem parte do esquema de cores padrão do Material 3.
 * Isso inclui cores de elevação, superfícies adicionais e cores de feedback.
 */
@Immutable
data class CustomColors internal constructor(
    // Cores de superfície com elevação
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val surface4: Color,
    val surface5: Color,
    
    // Cores de contêiner com opacidade
    val primaryContainer: Color,
    val secondaryContainer: Color,
    val tertiaryContainer: Color,
    
    // Cores de texto com opacidade
    val onSurfaceVariant: Color,
    
    // Cores de feedback
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val info: Color,
    val onInfo: Color
) {
    // Funções de conveniência para cores de feedback
    fun feedbackForType(type: FeedbackType): Pair<Color, Color> {
        return when (type) {
            FeedbackType.SUCCESS -> success to onSuccess
            FeedbackType.WARNING -> warning to onWarning
            FeedbackType.INFO -> info to onInfo
            FeedbackType.ERROR -> ErrorColor to OnErrorColor
        }
    }
}

/**
 * Tipo de feedback para cores de feedback
 */
enum class FeedbackType {
    SUCCESS, WARNING, INFO, ERROR
}

/**
 * Provider de cores personalizadas que pode ser acessado em qualquer lugar do aplicativo
 */
val LocalCustomColors = staticCompositionLocalOf {
    CustomColors(
        surface1 = Color.Unspecified,
        surface2 = Color.Unspecified,
        surface3 = Color.Unspecified,
        surface4 = Color.Unspecified,
        surface5 = Color.Unspecified,
        primaryContainer = Color.Unspecified,
        secondaryContainer = Color.Unspecified,
        tertiaryContainer = Color.Unspecified,
        onSurfaceVariant = Color.Unspecified,
        success = Color.Unspecified,
        onSuccess = Color.Unspecified,
        warning = Color.Unspecified,
        onWarning = Color.Unspecified,
        info = Color.Unspecified,
        onInfo = Color.Unspecified
    )
}

/**
 * Extensão para acessar cores personalizadas do MaterialTheme
 */
val MaterialTheme.customColors: CustomColors
    @Composable
    get() = LocalCustomColors.current
