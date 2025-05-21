package com.example.sleepadvisor.domain.repository

import com.example.sleepadvisor.domain.model.SleepSession
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.time.LocalDate

/**
 * Interface do repositório que define operações para acessar dados de sono
 * Fornece uma abstração entre a camada de dados e a camada de domínio
 */
interface SleepRepository {
    /**
     * Obtém sessões de sono dentro de um intervalo de tempo
     */
    suspend fun getSleepSessions(
        startTime: ZonedDateTime,
        endTime: ZonedDateTime
    ): Flow<List<SleepSession>>
    
    /**
     * Obtém a última sessão de sono registrada
     */
    suspend fun getLastSleepSession(): Flow<SleepSession?>
    
    /**
     * Obtém uma sessão de sono específica pelo ID
     */
    suspend fun getSleepSessionById(id: String): SleepSession?
    
    /**
     * Adiciona uma nova sessão de sono manual
     */
    suspend fun addManualSleepSession(
        startTime: ZonedDateTime,
        endTime: ZonedDateTime,
        sleepType: String? = null,
        notes: String? = null,
        wakeCount: Int
    ): SleepSession
    
    /**
     * Remove uma sessão de sono manual
     */
    suspend fun deleteManualSleepSession(session: SleepSession)
    
    /**
     * Atualiza as anotações de uma sessão de sono manual
     */
    suspend fun updateManualSleepSessionNotes(sessionId: String, notes: String?)
    
    /**
     * Verifica se o aplicativo tem as permissões necessárias
     */
    suspend fun hasPermissions(): Boolean
    
    /**
     * Solicita as permissões necessárias
     */
    suspend fun requestPermissions()

    /**
     * Obtém sessões de sono manuais para uma data específica.
     */
    fun getManualSleepSessionsForDate(date: LocalDate): Flow<List<SleepSession>>

    /**
     * Atualiza uma sessão de sono manual existente
     */
    suspend fun updateManualSleepSession(session: SleepSession)
} 