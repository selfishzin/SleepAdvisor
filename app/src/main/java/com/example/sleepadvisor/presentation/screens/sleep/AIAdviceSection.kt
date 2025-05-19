package com.example.sleepadvisor.presentation.screens.sleep

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sleepadvisor.domain.service.SleepAdvice

/**
 * Extension function para obter o texto principal do conselho de forma segura
 */
fun SleepAdvice.getAdviceText(): String {
    return try {
        // Acessar campo por reflexão se mainAdvice não for acessível diretamente
        val field = this::class.java.getDeclaredField("mainAdvice")
        field.isAccessible = true
        field.get(this) as? String ?: toString()
    } catch (e: Exception) {
        // Fallback para usar o toString() do objeto
        "Dicas de sono personalizadas disponíveis"
    }
}

/**
 * Componente que exibe uma seção com conselhos de IA para o usuário.
 * 
 * @param advice O objeto de conselho gerado pela IA para exibir
 * @param isLoading Se está carregando os conselhos
 * @param modifier Modificador opcional para customizar o layout
 */
@Composable
fun AIAdviceSection(
    advice: SleepAdvice?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Conselho de IA",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Conselho para Melhorar seu Sono",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Analisando seus padrões de sono...",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            } else if (advice != null) {
                Text(
                    text = advice.getAdviceText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                Text(
                    text = "Registre mais noites de sono para receber conselhos personalizados.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
