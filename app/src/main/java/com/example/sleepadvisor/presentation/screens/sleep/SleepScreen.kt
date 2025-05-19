package com.example.sleepadvisor.presentation.screens.sleep

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.sleepadvisor.domain.model.getStagePercentage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.health.connect.client.PermissionController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepStageType
import com.example.sleepadvisor.domain.service.SleepAdvice
import com.example.sleepadvisor.domain.model.DailyAnalysis
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.axis.vertical.VerticalAxis
import com.patrykandpatrick.vico.core.component.text.TextComponent
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import android.util.Log
import com.example.sleepadvisor.presentation.screens.sleep.SleepAnalysisSummary

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
    onNavigateToManualEntry: (String?) -> Unit, 
    onNavigateToAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        viewModel.onPermissionsResult(grantResults.filter { it.value }.keys)
    }

    LaunchedEffect(Unit) {
        if (!uiState.hasPermissions && uiState.error == null) { 
            permissionLauncher.launch(viewModel.permissions.toTypedArray())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sleep Advisor") },
                actions = {
                    IconButton(onClick = { viewModel.onShowManualEntryDialog(true) }) { 
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Adicionar registro manual"
                        )
                    }
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.sleepSessions.isNotEmpty()) {
                FloatingActionButton(onClick = onNavigateToAnalysis) {
                    Icon(Icons.Default.Analytics, contentDescription = "Ver Análise Detalhada")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (!uiState.hasPermissions) {
                PermissionRequestUI(permissionLauncher, viewModel.permissions.toTypedArray())
            } else {
                SleepContent(
                    uiState = uiState,
                    onDeleteSessionRequest = { session -> viewModel.deleteSleepSession(session) }, 
                    onEditSession = { session -> viewModel.onEditSession(session) }, 
                    onNavigateToAnalysis = onNavigateToAnalysis,
                    onSleepSessionClick = { session ->
                        Log.d("SleepScreen", "Session clicked: ${session.id}")
                    }
                )
            }

            if (uiState.showManualEntryDialog) {
                ManualSleepEntryDialog(
                    uiState = uiState,
                    onDismiss = { viewModel.onDismissManualEntryDialog() },
                    onSave = { viewModel.saveManualSleepSession() },
                    onDateSelected = { date -> viewModel.onDateSelected(date) },
                    onTimeSet = { hour, minute, isStart -> viewModel.onTimeSet(hour, minute, isStart) },
                    onNotesChanged = { notes -> viewModel.onNotesChanged(notes) }
                )
            }

            uiState.error?.let {
                ErrorDialog(errorMessage = it, onDismiss = { viewModel.clearError() })
            }
        }
    }
}

@Composable
fun PermissionRequestUI(permissionLauncher: ActivityResultLauncher<Array<String>>, permissions: Array<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Precisamos de permissão para acessar seus dados de sono e fornecer uma análise completa.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { permissionLauncher.launch(permissions) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Conceder Permissão")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Você ainda poderá adicionar registros manuais sem essa permissão.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ErrorDialog(errorMessage: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Erro") },
        text = { Text(errorMessage) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
fun SleepContent(
    uiState: SleepUiState,
    onDeleteSessionRequest: (SleepSession) -> Unit,
    onEditSession: (SleepSession) -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onSleepSessionClick: (SleepSession) -> Unit, 
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 72.dp) 
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.lastSession != null) {
                Text("Última Noite de Sono", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                LastSleepCard(session = uiState.lastSession, uiState = uiState)
            } else if (uiState.sleepSessions.isEmpty() && !uiState.isLoading) {
                EmptySessionsMessage(onAddManualClick = { /* viewModel.onShowManualEntryDialog(true) - Ação já está no TopAppBar */ })
            }
        }

        if (uiState.sleepSessions.isNotEmpty()) {
            item {
                AIAdviceSection(
                    advice = uiState.sleepAdvice,
                    isLoading = uiState.aiAdviceLoading
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Histórico de Sono", style = MaterialTheme.typography.titleLarge)
                 OutlinedButton(
                    onClick = onNavigateToAnalysis,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver Análise Detalhada de Tendências")
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(uiState.sleepSessions, key = { it.id }) {
                session ->
                val analysis = uiState.sessionAnalyses[session.id]
                SleepSessionCard(
                    session = session,
                    onEdit = { onEditSession(session) },
                    onDelete = { onDeleteSessionRequest(session) }, 
                    dailyAnalysis = analysis,
                    onClick = { onSleepSessionClick(session) } 
                )
            }
        }
    }
}

@Composable
fun EmptySessionsMessage(onAddManualClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Bedtime,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nenhum registro de sono encontrado.",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Adicione um registro manualmente ou sincronize com o Health Connect para começar.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAddManualClick) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
            Text("Adicionar Sono Manualmente")
        }
    }
}

@Composable
fun LastSleepCard(session: SleepSession?, uiState: SleepUiState, modifier: Modifier = Modifier) {
    if (session == null) {
        OutlinedCard(modifier = modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Nenhum dado da última noite.", style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }

    // Obter a análise pré-calculada do ViewModel em vez de gerar novamente
    val analysis = uiState.sessionAnalyses[session.id] ?: DailyAnalysis(
        date = session.startTimeZoned.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
        analysis = "Sem análise disponível para esta sessão.",
        sleepScore = (session.efficiency).toInt(),
        sleepQuality = "Sem dados"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Em ${session.startTime.atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM"))}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.weight(1.0f))
                if (session.source == "Manual") {
                    Icon(Icons.Default.EditNote, contentDescription = "Entrada Manual", tint = MaterialTheme.colorScheme.secondary)
                } else if (session.source == "HealthConnect") {
                    Icon(Icons.Default.Sensors, contentDescription = "Health Connect", tint = MaterialTheme.colorScheme.primary)
                } else if (session.source == "Simulation") {
                     Icon(Icons.Default.SmartToy, contentDescription = "Simulado", tint = MaterialTheme.colorScheme.tertiary)
                } else {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Fonte desconhecida", tint = MaterialTheme.colorScheme.outline)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format("Você dormiu por %dh %02dmin", session.duration.toHours(), session.duration.toMinutesPart()),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "De ${session.startTime.atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))} até ${session.endTime.atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (analysis.sleepScore > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Qualidade do Sono: ", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "${analysis.sleepQuality} (${analysis.sleepScore}%)",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = QualitätsFarbe(analysis.sleepScore.toDouble()))
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (session.stages.isNotEmpty()) {
                SleepStageInfo(
                    lightSleepPercentage = session.getStagePercentage(SleepStageType.LIGHT),
                    deepSleepPercentage = session.getStagePercentage(SleepStageType.DEEP),
                    remSleepPercentage = session.getStagePercentage(SleepStageType.REM),
                    isEstimated = session.source == "Simulation" || session.stages.any { it.source == "Simulation" }
                )
            } else {
                Text("Estágios do sono não disponíveis.", style = MaterialTheme.typography.bodySmall)
            }

            if (analysis.recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Recomendação: ", style = MaterialTheme.typography.bodyLarge)
                Text(analysis.recommendations.first(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

fun QualitätsFarbe(score: Double): Color {
    return when {
        score >= 80 -> Color(0xFF4CAF50) 
        score >= 60 -> Color(0xFFFFC107) 
        else -> Color(0xFFF44336) 
    }
}

@Composable
fun SleepStageInfo(
    lightSleepPercentage: Double,
    deepSleepPercentage: Double,
    remSleepPercentage: Double,
    modifier: Modifier = Modifier,
    isEstimated: Boolean = false
) {
    Column(modifier = modifier) {
        if(isEstimated){
            Text("(Estágios Estimados)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            SleepStagePercentage("Leve", lightSleepPercentage, Color(0xFF81D4FA), Modifier.weight(1f), isEstimated)
            SleepStagePercentage("Profundo", deepSleepPercentage, Color(0xFF29B6F6), Modifier.weight(1f), isEstimated)
            SleepStagePercentage("REM", remSleepPercentage, Color(0xFF0288D1), Modifier.weight(1f), isEstimated)
        }
    }
}

@Composable
fun SleepStagePercentage(
    label: String,
    percentage: Double,
    color: Color,
    modifier: Modifier = Modifier,
    isEstimated: Boolean = false 
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp) 
                .background(color.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (percentage / 100f).toFloat())
                    .background(color, shape = RoundedCornerShape(4.dp))
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = String.format("%s: %d%%", label, percentage.toInt()),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepSessionCard(
    session: SleepSession,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dailyAnalysis: DailyAnalysis? = null,
    onClick: () -> Unit, 
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick 
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.startTime.atZone(java.time.ZoneId.systemDefault()).format(dateFormatter),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = String.format(
                            "%s - %s (%.1f h)",
                            session.startTime.atZone(java.time.ZoneId.systemDefault()).format(formatter),
                            session.endTime.atZone(java.time.ZoneId.systemDefault()).format(formatter),
                            session.duration.toHours().toFloat()
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (session.source == "Manual") {
                    Icon(Icons.Default.EditNote, contentDescription = "Entrada Manual", tint = MaterialTheme.colorScheme.secondary)
                } else if (session.source == "HealthConnect") {
                    Icon(Icons.Default.Sensors, contentDescription = "Health Connect", tint = MaterialTheme.colorScheme.primary)
                } else if (session.source == "Simulation") {
                     Icon(Icons.Default.SmartToy, contentDescription = "Simulado", tint = MaterialTheme.colorScheme.tertiary)
                } else {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Fonte desconhecida", tint = MaterialTheme.colorScheme.outline)
                }
            }

            if (dailyAnalysis != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Qualidade: ", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${dailyAnalysis.sleepQuality} (${dailyAnalysis.sleepScore}%)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = QualitätsFarbe(dailyAnalysis.sleepScore.toDouble()))
                    )
                }
            }

            if (session.stages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                SleepStageInfo(
                    lightSleepPercentage = session.getStagePercentage(SleepStageType.LIGHT),
                    deepSleepPercentage = session.getStagePercentage(SleepStageType.DEEP),
                    remSleepPercentage = session.getStagePercentage(SleepStageType.REM),
                    isEstimated = session.source == "Simulation" || session.stages.any { stage -> stage.source == "Simulation" }
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    if (session.notes != null && session.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Notas:", style = MaterialTheme.typography.labelMedium)
                        Text(session.notes, style = MaterialTheme.typography.bodySmall)
                    }

                    // Primeira recomendação
                    if (dailyAnalysis?.recommendations?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Recomendação Principal:", style = MaterialTheme.typography.labelMedium)
                        Text(dailyAnalysis.recommendations.first(), style = MaterialTheme.typography.bodySmall)
                        
                        // Segunda recomendação (se existir)
                        if (dailyAnalysis.recommendations.size > 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Outra Dica:", style = MaterialTheme.typography.labelMedium)
                            Text(dailyAnalysis.recommendations[1], style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (session.source == "Manual") {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir")
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Menos Detalhes" else "Mais Detalhes")
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Recolher" else "Expandir"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSleepEntryDialog(
    uiState: SleepUiState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onTimeSet: (hour: Int, minute: Int, isStart: Boolean) -> Unit,
    onNotesChanged: (String) -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (uiState.editingSession == null) "Adicionar Sono Manual" else "Editar Sono Manual") },
        text = {
            Column {
                Button(onClick = {
                    val currentCal = java.util.Calendar.getInstance()
                    uiState.selectedStartTime?.let {
                        currentCal.timeInMillis = it.toInstant().toEpochMilli()
                    }
                    android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            onDateSelected(java.time.LocalDate.of(year, month + 1, dayOfMonth))
                        },
                        currentCal.get(java.util.Calendar.YEAR),
                        currentCal.get(java.util.Calendar.MONTH),
                        currentCal.get(java.util.Calendar.DAY_OF_MONTH)
                    ).show()
                }) {
                    Text("Data: ${uiState.selectedDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "Selecionar"}")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    val currentCal = java.util.Calendar.getInstance()
                     uiState.selectedStartTime?.let {
                        currentCal.timeInMillis = it.toInstant().toEpochMilli()
                    }
                    android.app.TimePickerDialog(
                        context,
                        { _, hour, minute -> onTimeSet(hour, minute, true) },
                        currentCal.get(java.util.Calendar.HOUR_OF_DAY),
                        currentCal.get(java.util.Calendar.MINUTE),
                        true
                    ).show()
                }) {
                    Text("Início: ${uiState.selectedStartTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Selecionar"}")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                     val currentCal = java.util.Calendar.getInstance()
                     uiState.selectedEndTime?.let {
                        currentCal.timeInMillis = it.toInstant().toEpochMilli()
                    }
                    android.app.TimePickerDialog(
                        context,
                        { _, hour, minute -> onTimeSet(hour, minute, false) },
                        currentCal.get(java.util.Calendar.HOUR_OF_DAY),
                        currentCal.get(java.util.Calendar.MINUTE),
                        true
                    ).show()
                }) {
                    Text("Fim: ${uiState.selectedEndTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Selecionar"}")
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.manualEntryNotes,
                    onValueChange = onNotesChanged,
                    label = { Text("Notas (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}