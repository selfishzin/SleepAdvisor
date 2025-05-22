package com.example.sleepadvisor.presentation.screens.sleep

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent // Mantido caso seja usado em outro lugar, senão pode ser removido
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
// import androidx.compose.ui.platform.LocalLifecycleOwner // Não utilizado diretamente aqui
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient // Não utilizado diretamente aqui
import androidx.health.connect.client.PermissionController // Não utilizado diretamente aqui
import androidx.health.connect.client.permission.HealthPermission // Não utilizado diretamente aqui
import androidx.health.connect.client.records.SleepSessionRecord // Não utilizado diretamente aqui
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sleepadvisor.domain.model.DailyAnalysis
import com.example.sleepadvisor.domain.model.SleepSession // Assumindo que SleepSession.source é atualizado para SleepSource
import com.example.sleepadvisor.domain.model.SleepStageType
import com.example.sleepadvisor.domain.model.getStagePercentage
// import com.example.sleepadvisor.domain.service.SleepAdvice // Não utilizado diretamente aqui
// import com.example.sleepadvisor.presentation.screens.sleep.SleepAnalysisSummary // Não utilizado diretamente aqui
// import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis // Não utilizado diretamente aqui
// import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis // Não utilizado diretamente aqui
// import com.patrykandpatrick.vico.compose.chart.Chart // Não utilizado diretamente aqui
// import com.patrykandpatrick.vico.compose.chart.line.lineChart // Não utilizado diretamente aqui
// import com.patrykandpatrick.vico.core.axis.AxisPosition // Não utilizado diretamente aqui
// import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter // Não utilizado diretamente aqui
// import com.patrykandpatrick.vico.core.component.shape.LineComponent // Não utilizado diretamente aqui
// import com.patrykandpatrick.vico.core.component.text.TextComponent // Não utilizado diretamente aqui
// import com.patrykandpatrick.vico.core.entry.entryModelOf // Não utilizado diretamente aqui
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.OffsetDateTime // Assumindo que uiState.selectedStartTime/EndTime são deste tipo ou similar com isBefore/isEqual
import java.time.format.DateTimeFormatter
import java.util.Calendar
// import kotlin.time.Duration.Companion.minutes // Não utilizado diretamente aqui
import kotlinx.coroutines.launch // Não utilizado diretamente aqui, mas rememberCoroutineScope sim
import com.example.sleepadvisor.domain.model.SleepSource
// data class SleepStage(..., val source: SleepSource, ...)

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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        viewModel.onPermissionsResult(grantResults.filter { it.value }.keys)
    }

    LaunchedEffect(Unit) {
        // Só pede permissões se não as tiver e não houver um erro que impeça (ex: Health Connect não disponível)
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
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar dados")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.sleepSessions.isNotEmpty()) {
                FloatingActionButton(onClick = onNavigateToAnalysis) {
                    Icon(Icons.Default.Analytics, contentDescription = "Ver Análise Detalhada de Tendências")
                }
            }
        }
    ) { padding ->
        Column(modifier = modifier
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
                    onEditSession = { session ->
                        viewModel.onEditSession(session) // ViewModel prepara o estado para edição
                        // onNavigateToManualEntry(session.id) // Navegação pode ser feita pelo ViewModel ou removida se o diálogo for modal
                    },
                    onNavigateToAnalysis = onNavigateToAnalysis,
                    onSleepSessionClick = { session ->
                        Log.d("SleepScreen", "Session clicked: ${session.id}")
                        // Ação de clique pode ser expandir ou navegar para detalhes, se houver outra tela.
                    },
                    onShowManualEntryDialog = { viewModel.onShowManualEntryDialog(true) }
                )
            }

            if (uiState.showManualEntryDialog) {
                ManualSleepEntryDialog(
                    uiState = uiState,
                    onDismiss = { viewModel.onDismissManualEntryDialog() },
                    onSave = { viewModel.saveManualSleepSession() }, // ViewModel lida com criação ou atualização
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
    uiState: SleepViewModel.SleepUiState,
    onDeleteSessionRequest: (SleepSession) -> Unit,
    onEditSession: (SleepSession) -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onSleepSessionClick: (SleepSession) -> Unit,
    onShowManualEntryDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 72.dp) // Espaço para o FAB
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.lastSession != null) {
                Text("Última Noite de Sono", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                LastSleepCard(session = uiState.lastSession, uiState = uiState)
            } else if (uiState.sleepSessions.isEmpty() && !uiState.isLoading) {
                EmptySessionsMessage(onAddManualClick = onShowManualEntryDialog)
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

            items(uiState.sleepSessions, key = { it.id }) { session ->
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
fun SourceIcon(source: SleepSource, modifier: Modifier = Modifier) {
    val icon = when (source) {
        SleepSource.MANUAL -> Icons.Default.EditNote
        SleepSource.HEALTH_CONNECT -> Icons.Default.Sensors
        SleepSource.SIMULATION -> Icons.Default.SmartToy
        SleepSource.UNKNOWN -> Icons.Default.HelpOutline
        SleepSource.GOOGLE_FIT -> Icons.Default.FitnessCenter
    }
    val tint = when (source) {
        SleepSource.MANUAL -> MaterialTheme.colorScheme.secondary
        SleepSource.HEALTH_CONNECT -> MaterialTheme.colorScheme.primary
        SleepSource.SIMULATION -> MaterialTheme.colorScheme.tertiary
        SleepSource.UNKNOWN -> MaterialTheme.colorScheme.outline
        SleepSource.GOOGLE_FIT -> MaterialTheme.colorScheme.primary
    }
    Icon(imageVector = icon, contentDescription = "Fonte: ${source.displayName}", tint = tint, modifier = modifier)
}

@Composable
fun LastSleepCard(session: SleepSession?, uiState: SleepViewModel.SleepUiState, modifier: Modifier = Modifier) {
    if (session == null) {
        OutlinedCard(modifier = modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Nenhum dado da última noite.", style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }

    val sourceEnum = session.source

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.startTimeZoned.format(DateTimeFormatter.ofPattern("dd/MM")),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = String.format(
                            "%s - %s (%.1f h)",
                            session.startTimeZoned.format(DateTimeFormatter.ofPattern("HH:mm")),
                            session.endTimeZoned.format(DateTimeFormatter.ofPattern("HH:mm")),
                            session.duration.toHours().toFloat()
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                SourceIcon(source = sourceEnum)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "De ${session.startTimeZoned.format(DateTimeFormatter.ofPattern("HH:mm"))} até ${session.endTimeZoned.format(DateTimeFormatter.ofPattern("HH:mm"))}",
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
                    isEstimated = sourceEnum == SleepSource.SIMULATION || session.stages.any { it.source == SleepSource.SIMULATION }
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
        if (isEstimated) {
            Text("(Estágios Estimados)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            SleepStagePercentage("Leve", lightSleepPercentage, Color(0xFF81D4FA), Modifier.weight(1f))
            SleepStagePercentage("Profundo", deepSleepPercentage, Color(0xFF29B6F6), Modifier.weight(1f))
            SleepStagePercentage("REM", remSleepPercentage, Color(0xFF0288D1), Modifier.weight(1f))
        }
    }
}

@Composable
fun SleepStagePercentage(
    label: String,
    percentage: Double,
    color: Color,
    modifier: Modifier = Modifier
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
    var expanded by rememberSaveable(key = session.id) { mutableStateOf(false) }

    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    val sourceEnum = session.source

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.startTimeZoned.format(dateFormatter),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = String.format(
                            "%s - %s (%.1f h)",
                            session.startTimeZoned.format(formatter),
                            session.endTimeZoned.format(formatter),
                            session.duration.toHours().toFloat()
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                SourceIcon(source = sourceEnum)
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
                    isEstimated = sourceEnum == SleepSource.SIMULATION || session.stages.any { it.source == SleepSource.SIMULATION }
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    if (session.source == SleepSource.HEALTH_CONNECT || session.source == SleepSource.GOOGLE_FIT) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Notas: ${session.notes ?: "Nenhuma nota."}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (dailyAnalysis?.recommendations?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Recomendação Principal:", style = MaterialTheme.typography.labelMedium)
                        Text(dailyAnalysis.recommendations.first(), style = MaterialTheme.typography.bodySmall)

                        if (dailyAnalysis.recommendations.size > 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Outra Dica:", style = MaterialTheme.typography.labelMedium)
                            Text(dailyAnalysis.recommendations[1], style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (sourceEnum == SleepSource.MANUAL) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar Sessão")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir Sessão")
                    }
                }
                Spacer(modifier = Modifier.weight(1f)) // Empurra o botão de detalhes para a direita
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Menos Detalhes" else "Mais Detalhes")
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Recolher detalhes" else "Expandir detalhes"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSleepEntryDialog(
    uiState: SleepViewModel.SleepUiState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSet: (hour: Int, minute: Int, isStart: Boolean) -> Unit,
    onNotesChanged: (String) -> Unit
) {
    val context = LocalContext.current
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (uiState.editingSession == null) "Adicionar Sono Manual" else "Editar Sono Manual") },
        text = {
            Column {
                Button(onClick = {
                    val currentCal = Calendar.getInstance()
                    uiState.selectedStartTime?.let { // Usa selectedStartTime se já definido, senão data atual
                        currentCal.timeInMillis = it.toInstant().toEpochMilli()
                    }
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
                            validationError = null // Limpa erro ao mudar data
                        },
                        currentCal.get(Calendar.YEAR),
                        currentCal.get(Calendar.MONTH),
                        currentCal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }) {
                    Text("Data: ${uiState.selectedDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "Selecionar Data"}")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    val currentCal = Calendar.getInstance()
                    uiState.selectedStartTime?.let {
                        currentCal.timeInMillis = it.toInstant().toEpochMilli()
                    }
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            onTimeSet(hour, minute, true)
                            validationError = null // Limpa erro
                        },
                        currentCal.get(Calendar.HOUR_OF_DAY),
                        currentCal.get(Calendar.MINUTE),
                        true // is24HourView
                    ).show()
                }) {
                    Text("Início: ${uiState.selectedStartTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Selecionar Início"}")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    val currentCal = Calendar.getInstance()
                    uiState.selectedEndTime?.let {
                        currentCal.timeInMillis = it.toInstant().toEpochMilli()
                    }
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            onTimeSet(hour, minute, false)
                            validationError = null // Limpa erro
                        },
                        currentCal.get(Calendar.HOUR_OF_DAY),
                        currentCal.get(Calendar.MINUTE),
                        true // is24HourView
                    ).show()
                }) {
                    Text("Fim: ${uiState.selectedEndTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Selecionar Fim"}")
                }

                validationError?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
            Button(
                onClick = {
                    if (uiState.selectedDate == null) {
                        validationError = "Por favor, selecione uma data."
                        return@Button
                    }
                    if (uiState.selectedStartTime == null) {
                        validationError = "Por favor, selecione a hora de início."
                        return@Button
                    }
                    if (uiState.selectedEndTime == null) {
                        validationError = "Por favor, selecione a hora de fim."
                        return@Button
                    }

                    // Validação de tempo (assumindo que selectedStartTime e selectedEndTime são OffsetDateTime ou similar)
                    // selectedStartTime e selectedEndTime já incluem a data através do viewModel
                    if (uiState.selectedEndTime.isBefore(uiState.selectedStartTime)) {
                        validationError = "A hora de fim não pode ser anterior à hora de início."
                        return@Button
                    }
                    if (uiState.selectedEndTime.isEqual(uiState.selectedStartTime)) {
                        validationError = "A hora de início e fim não podem ser iguais."
                        return@Button
                    }
                    validationError = null
                    onSave()
                },
                enabled = uiState.selectedDate != null && uiState.selectedStartTime != null && uiState.selectedEndTime != null
            ) {
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

// Placeholder para AIAdviceSection se não existir
@Composable
fun AIAdviceSection(advice: String?, isLoading: Boolean, modifier: Modifier = Modifier) {
    if (isLoading) {
        Box(modifier = modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (!advice.isNullOrBlank()) {
        Card(modifier = modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Conselho da IA", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(advice, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// Assume que SleepViewModel e os modelos de dados (SleepSession, DailyAnalysis, SleepUiState)
// foram atualizados para usar SleepSource onde aplicável (especialmente SleepSession.source
// e SleepStage.source se este último também tiver uma fonte individual).