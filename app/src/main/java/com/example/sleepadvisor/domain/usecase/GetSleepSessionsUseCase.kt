package com.example.sleepadvisor.domain.usecase

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
// import androidx.health.connect.client.records.ExerciseEventRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SleepStageRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepSource
import com.example.sleepadvisor.domain.model.SleepStage
import com.example.sleepadvisor.domain.model.SleepStageType
import com.example.sleepadvisor.domain.repository.SleepRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

class GetSleepSessionsUseCase @Inject constructor(
    private val repository: SleepRepository
) {
    // Intervalo máximo entre sessões para considerá-las parte da mesma noite (configurável)
    // Aumentado para 15 minutos conforme sugestão do usuário
    private val MAX_SESSION_GAP_MINUTES = Duration.ofMinutes(15)
    
    suspend operator fun invoke(
        startTime: ZonedDateTime,
        endTime: ZonedDateTime
    ): Flow<List<SleepSession>> {
        return repository.getSleepSessions(startTime, endTime)
    }
    
    /**
     * Obtém a última sessão de sono noturno (ignorando sonecas)
     */
    suspend fun getLastNightSession(): Flow<SleepSession?> {
        val endTime = ZonedDateTime.now()
        val startTime = endTime.minusDays(7)
        
        return repository.getSleepSessions(startTime, endTime)
            .map { sessions -> 
                val consolidated = consolidateSleepSessions(sessions)
                    .sortedByDescending { it.startTime }
                
                // Filtra apenas sessões de sono noturno (duração >= 3h e ocorrendo à noite)
                consolidated.firstOrNull { session ->
                    isNightSleep(session)
                }
            }
    }
    
    /**
     * Determina se uma sessão é sono noturno ou uma soneca
     * Critérios: duração >= 3h ou ocorre durante a noite (22h-8h)
     */
    private fun isNightSleep(session: SleepSession): Boolean {
        // Critério 1: Duração mínima de 3 horas
        val minNightSleepDuration = Duration.ofHours(3)
        if (session.duration >= minNightSleepDuration) {
            return true
        }
        
        // Critério 2: Ocorre durante o período noturno (22h-8h)
        val startHour = session.startTime.atZone(ZoneId.systemDefault()).hour
        val endHour = session.endTime.atZone(ZoneId.systemDefault()).hour
        
        // Considera noite se começou após 22h ou terminou antes das 8h
        val startedAtNight = startHour >= 22 || startHour <= 2
        val endedInMorning = endHour >= 5 && endHour <= 11
        
        return startedAtNight && endedInMorning
    }
    
    /**
     * Obtém a última sessão de sono (incluindo sonecas)
     */
    suspend fun getLastSession(): Flow<SleepSession?> {
        return repository.getLastSleepSession()
    }
    
    /**
     * Obtém sessões de sono consolidadas dos últimos 7 dias, agrupando sessões que
     * pertencem à mesma noite (com intervalos menores que o configurado)
     */
    suspend fun getConsolidatedSleepSessions(): Flow<List<SleepSession>> {
        val endTime = ZonedDateTime.now()
        val startTime = endTime.minusDays(7)
        
        return repository.getSleepSessions(startTime, endTime)
            .map { sessions -> 
                consolidateSleepSessions(sessions)
                    .sortedByDescending { it.startTime }
            }
    }
    
    /**
     * Consolida sessões de sono que pertencem à mesma noite
     * Sessões com menos do que o intervalo configurado entre elas são consideradas parte da mesma noite
     * Também preenche os dados de estágios de sono quando disponíveis
     */
    private fun consolidateSleepSessions(sessions: List<SleepSession>): List<SleepSession> {
        if (sessions.isEmpty()) return emptyList()
        
        // Ordena sessões por hora de início
        val sortedSessions = sessions.sortedBy { it.startTime }
        val consolidatedSessions = mutableListOf<SleepSession>()
        
        var currentGroup = mutableListOf(sortedSessions.first())
        
        // Agrupa sessões que ocorrem na mesma noite
        for (i in 1 until sortedSessions.size) {
            val currentSession = sortedSessions[i]
            val previousSession = currentGroup.last()
            
            val timeBetweenSessions = Duration.between(
                previousSession.endTime,
                currentSession.startTime
            )
            
            if (timeBetweenSessions <= MAX_SESSION_GAP_MINUTES) {
                // Adiciona ao grupo atual se o intervalo for pequeno o suficiente
                currentGroup.add(currentSession)
            } else {
                // Consolida o grupo atual e inicia um novo
                consolidatedSessions.add(createConsolidatedSession(currentGroup))
                currentGroup = mutableListOf(currentSession)
            }
        }
        
        // Adiciona o último grupo
        if (currentGroup.isNotEmpty()) {
            consolidatedSessions.add(createConsolidatedSession(currentGroup))
        }
        
        return consolidatedSessions
    }
    
    /**
     * Cria uma sessão de sono consolidada a partir de um grupo de sessões que pertencem à mesma noite
     * Inclui processamento melhorado dos estágios de sono
     */
    private fun createConsolidatedSession(sessions: List<SleepSession>): SleepSession {
        val firstSession = sessions.first()
        val lastSession = sessions.last()
        
        // Usa o horário de início mais cedo e o horário de término mais tarde
        val startTime = sessions.minOf { it.startTime }
        val endTime = sessions.maxOf { it.endTime }
        
        // Calcula a duração total somando todas as durações das sessões
        val totalDuration = Duration.between(startTime, endTime)
        
        // Gera um ID composto para a sessão consolidada
        val consolidatedId = if (sessions.size == 1) {
            firstSession.id
        } else {
            "${firstSession.id}_${lastSession.id}"
        }
        
        // Processa os estágios de sono para garantir que estejam presentes e corretos
        val stages = processAndEnhanceSleepStages(sessions, startTime, endTime, totalDuration)
        
        // Calcula o número total de despertares durante a noite
        val totalWakeCount = sessions.sumOf { it.wakeDuringNightCount }
        
        val startInstant = startTime
        val endInstant = endTime
        
        return SleepSession(
            id = consolidatedId,
            startTime = startInstant,
            endTime = endInstant,
            stages = stages,
            notes = sessions.mapNotNull { it.notes }.joinToString("\n").takeIf { it.isNotEmpty() },
            wakeDuringNightCount = totalWakeCount,
            source = SleepSource.MANUAL // Indicando que esta é uma sessão consolidada
        )
    }
    
    /**
     * Processa e melhora os dados de estágios de sono
     * Se não houver dados de estágios disponíveis, cria estimativas baseadas na duração total
     */
    private fun processAndEnhanceSleepStages(
        sessions: List<SleepSession>,
        startTime: Instant,
        endTime: Instant,
        totalDuration: Duration
    ): List<SleepStage> {
        // Coleta todos os estágios existentes
        val existingStages = sessions.flatMap { it.stages }
        
        // Se existirem estágios, usa-os
        if (existingStages.isNotEmpty()) {
            return existingStages
        }
        
        // Se não houver estágios, cria estimativas baseadas em distribuições típicas
        // Distribuição típica: 55% sono leve, 25% sono profundo, 20% REM
        val totalMinutes = totalDuration.toMinutes()
        
        if (totalMinutes <= 0) return emptyList()
        
        val lightSleepMinutes = (totalMinutes * 0.55).toLong()
        val deepSleepMinutes = (totalMinutes * 0.25).toLong()
        val remSleepMinutes = totalMinutes - lightSleepMinutes - deepSleepMinutes
        
        // Cria estágios estimados
        val lightStage = SleepStage(
            type = SleepStageType.LIGHT,
            startTime = startTime,
            endTime = Instant.ofEpochMilli(startTime.toEpochMilli() + Duration.ofMinutes(lightSleepMinutes).toMillis()),
            source = SleepSource.SIMULATION
        )
        
        val deepStage = SleepStage(
            type = SleepStageType.DEEP,
            startTime = lightStage.endTime,
            endTime = Instant.ofEpochMilli(lightStage.endTime.toEpochMilli() + Duration.ofMinutes(deepSleepMinutes.toLong()).toMillis()),
            source = SleepSource.SIMULATION
        )
        
        val remStage = SleepStage(
            type = SleepStageType.REM,
            startTime = deepStage.endTime,
            endTime = Instant.ofEpochMilli(deepStage.endTime.toEpochMilli() + Duration.ofMinutes(remSleepMinutes.toLong()).toMillis()),
            source = SleepSource.SIMULATION
        )
        
        return listOf(lightStage, deepStage, remStage)
    }
}