package com.example.sleepadvisor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Entidade que representa uma sessão de sono adicionada manualmente pelo usuário
 * 
 * @property id Identificador único da sessão de sono
 * @property startTime Hora de início da sessão (inclui data e timezone)
 * @property endTime Hora de fim da sessão (inclui data e timezone)
 * @property createdAt Data e hora de criação do registro (para auditoria)
 * @property lightSleepPercentage Porcentagem de sono leve
 * @property deepSleepPercentage Porcentagem de sono profundo
 * @property remSleepPercentage Porcentagem de sono REM
 * @property notes Anotações opcionais do usuário sobre a sessão de sono
 * @property lastModified Data e hora da última modificação
 * @property wakeDuringNightCount Número de vezes que o usuário acordou durante a noite
 */
@Entity(tableName = "manual_sleep_sessions")
data class ManualSleepSessionEntity(
    @PrimaryKey
    val id: String,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
    val lightSleepPercentage: Double = 0.0,
    val deepSleepPercentage: Double = 0.0,
    val remSleepPercentage: Double = 0.0,
    val notes: String? = null,
    val lastModified: ZonedDateTime = ZonedDateTime.now(),
    val wakeDuringNightCount: Int = 0
) {
    /**
     * Calcula a duração da sessão de sono com base nos horários de início e fim
     */
    val duration: Duration
        get() = Duration.between(startTime, endTime)
} 