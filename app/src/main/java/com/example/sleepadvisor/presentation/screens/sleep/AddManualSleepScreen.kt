package com.example.sleepadvisor.presentation.screens.sleep

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.clickable
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.SelectableDates
import java.time.Instant
import java.util.Calendar
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualSleepScreen(
    viewModel: SleepViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    @Suppress("UNUSED_VARIABLE")
    val scope = rememberCoroutineScope()
    // Scope mantido para possível uso futuro

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var startTimeState by remember { mutableStateOf(LocalTime.of(22, 0)) }
    var endTimeState by remember { mutableStateOf(LocalTime.of(7, 0)) }
    var wakeCountString by remember { mutableStateOf("0") }
    var notesState by remember { mutableStateOf("") }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    
    val startTimePickerState = rememberTimePickerState(
        initialHour = startTimeState.hour,
        initialMinute = startTimeState.minute
    )
    
    val endTimePickerState = rememberTimePickerState(
        initialHour = endTimeState.hour,
        initialMinute = endTimeState.minute
    )
    
    LaunchedEffect(uiState.addSessionSuccess) {
        if (uiState.addSessionSuccess) {
            viewModel.resetAddSessionSuccess()
            onNavigateBack()
        }
    }
    
    if (uiState.showDuplicateEntryDialog) {
        DuplicateEntryDialog(
            onConfirm = {
                val zone = ZoneId.systemDefault()
                val startDateTime = ZonedDateTime.of(selectedDate, startTimeState, zone)
                val endDateTime = ZonedDateTime.of(
                    if (endTimeState.isBefore(startTimeState)) selectedDate.plusDays(1) else selectedDate,
                    endTimeState,
                    zone
                )
                val wakeCount = wakeCountString.toIntOrNull() ?: 0
                viewModel.confirmOverwriteSleepSession(startDateTime, endDateTime, notesState.ifEmpty { null }, wakeCount)
            },
            onDismiss = {
                viewModel.cancelOverwriteSleepSession()
            }
        )
    }
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = it
                        selectedDate = LocalDate.of(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH) + 1,
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showStartTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            onConfirm = {
                startTimeState = LocalTime.of(
                    startTimePickerState.hour,
                    startTimePickerState.minute
                )
                showStartTimePicker = false
            }
        ) {
            TimePicker(state = startTimePickerState)
        }
    }
    
    if (showEndTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            onConfirm = {
                endTimeState = LocalTime.of(
                    endTimePickerState.hour,
                    endTimePickerState.minute
                )
                showEndTimePicker = false
            }
        ) {
            TimePicker(state = endTimePickerState)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adicionar Sono Manualmente") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Selection
            OutlinedCard(
                modifier = Modifier.clickable { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Data",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = selectedDate.format(dateFormatter),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Icon(Icons.Default.DateRange, contentDescription = null)
                }
            }
            
            // Time Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showStartTimePicker = true }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Início",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = startTimeState.format(timeFormatter),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showEndTimePicker = true }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Fim",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = endTimeState.format(timeFormatter),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            
            // Wake Count Input
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Quantas vezes você acordou à noite?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = wakeCountString,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 2) {
                                wakeCountString = newValue
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        placeholder = { Text("0") },
                        leadingIcon = { Icon(Icons.Filled.Loop, contentDescription = "Número de despertares") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true
                    )
                }
            }
            
            // Campo de anotações
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Anotações (opcional)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    OutlinedTextField(
                        value = notesState,
                        onValueChange = { notesState = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        placeholder = { Text("Insira suas anotações aqui...") },
                        maxLines = 4
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    val zone = ZoneId.systemDefault()
                    val startDateTime = ZonedDateTime.of(selectedDate, startTimeState, zone)
                    val endDateTime = ZonedDateTime.of(
                        if (endTimeState.isBefore(startTimeState)) selectedDate.plusDays(1) else selectedDate,
                        endTimeState,
                        zone
                    )
                    val wakeCount = wakeCountString.toIntOrNull() ?: 0
                    
                    viewModel.addManualSleepSession(
                        startDateTime,
                        endDateTime,
                        notesState.ifEmpty { null },
                        wakeCount
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isAddingManualSession
            ) {
                if (uiState.isAddingManualSession) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Salvar Sono")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar")
            }
        },
        text = { content() }
    )
}

// Diálogo para confirmar a substituição de entrada existente
@Composable
private fun DuplicateEntryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Entrada Duplicada") },
        text = { Text("Você já tem dados de sono para este dia. Deseja editar a entrada existente?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Editar entrada existente")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
} 