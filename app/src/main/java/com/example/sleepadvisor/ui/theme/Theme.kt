package com.example.sleepadvisor.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkBlue,
    secondary = DarkPurple,
    tertiary = DarkPink,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkOnPrimary,
    onSecondary = DarkOnSecondary,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    error = ErrorColor,
    onError = DarkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = LightBlue,
    secondary = LightPurple,
    tertiary = LightPink,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = LightOnPrimary,
    onSecondary = LightOnSecondary,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    error = ErrorColor,
    onError = LightOnSurface
)

@Composable
fun SleepAdvisorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    // Configura a cor da barra de status e navegação
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        
        // Habilita o edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Configura a cor da barra de status
        window.statusBarColor = Color.Transparent.toArgb()
        
        // Configura a cor da barra de navegação
        window.navigationBarColor = colorScheme.surfaceColorAtElevation(3.dp).toArgb()
        
        // Configura o comportamento da barra de status e navegação
        val windowInsetsController = WindowCompat.getInsetsController(window, view)
        windowInsetsController.isAppearanceLightStatusBars = !darkTheme
        windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        
        // Habilita o comportamento de imersão
        windowInsetsController.systemBarsBehavior = 
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
    
    // Provedor de espaçamento personalizado
    val spacing = remember { Spacing() }
    
    // Configurações de tema personalizadas
    val customColors = remember(colorScheme) {
        CustomColors(
            // Cores de superfície com elevação
            surface1 = colorScheme.surfaceColorAtElevation(1.dp),
            surface2 = colorScheme.surfaceColorAtElevation(2.dp),
            surface3 = colorScheme.surfaceColorAtElevation(3.dp),
            surface4 = colorScheme.surfaceColorAtElevation(4.dp),
            surface5 = colorScheme.surfaceColorAtElevation(5.dp),
            
            // Cores de contêiner com opacidade
            primaryContainer = colorScheme.primaryContainer.copy(alpha = 0.2f),
            secondaryContainer = colorScheme.secondaryContainer.copy(alpha = 0.2f),
            tertiaryContainer = colorScheme.tertiaryContainer.copy(alpha = 0.2f),
            
            // Cores de texto com opacidade
            onSurfaceVariant = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            
            // Cores de feedback
            success = SuccessColor,
            onSuccess = OnSuccessColor,
            warning = WarningColor,
            onWarning = OnWarningColor,
            info = InfoColor,
            onInfo = OnInfoColor
        )
    }
    
    // Provedor de tema personalizado
    CompositionLocalProvider(
        LocalSpacing provides spacing,
        LocalCustomColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}