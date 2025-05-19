package com.example.sleepadvisor.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class SleepSessionTest {
    
    private val now = ZonedDateTime.now()
    
    @Test
    fun `deve calcular corretamente a duração total do sono`() {
        // Dado
        val startTime = now.minusHours(8)
        val endTime = now
        val session = createTestSession(
            startTime = startTime,
            endTime = endTime
        )
        
        // Quando
        val duration = session.duration
        
        // Então
        assertEquals(8 * 60, duration.toMinutes())
    }
    
    @Test
    fun `deve calcular corretamente a porcentagem de cada estágio de sono`() {
        // Dado
        val session = createTestSession(
            stages = listOf(
                SleepStage(SleepStageType.LIGHT, Duration.ofHours(4)),  // 50%
                SleepStage(SleepStageType.DEEP, Duration.ofHours(2)),    // 25%
                SleepStage(SleepStageType.REM, Duration.ofHours(2))       // 25%
            )
        )
        
        // Quando/Então
        assertEquals(50.0, session.lightSleepPercentage, 0.01)
        assertEquals(25.0, session.deepSleepPercentage, 0.01)
        assertEquals(25.0, session.remSleepPercentage, 0.01)
    }
    
    @Test
    fun `deve calcular corretamente a eficiência do sono`() {
        // Dado: 8 horas de sono (480 minutos)
        // - 4h leve (50%), 2h profundo (25%), 2h REM (25%)
        // - 1 despertar
        val session = createTestSession(
            duration = Duration.ofHours(8),
            stages = listOf(
                SleepStage(SleepStageType.LIGHT, Duration.ofHours(4)),
                SleepStage(SleepStageType.DEEP, Duration.ofHours(2)),
                SleepStage(SleepStageType.REM, Duration.ofHours(2))
            ),
            wakeCount = 1
        )
        
        // Quando
        val efficiency = session.efficiency
        
        // Então: Pontuação base (100) - penalidades
        // - Sono profundo 25% = ideal (0)
        // - Sono REM 25% = ideal (0)
        // - 1 despertar = -5
        assertEquals(95.0, efficiency, 0.5)
    }
    
    @Test
    fun `deve penalizar eficiência para sono profundo insuficiente`() {
        // Dado: Apenas 10% de sono profundo (abaixo do ideal de 25%)
        val session = createTestSession(
            stages = listOf(
                SleepStage(SleepStageType.LIGHT, Duration.ofHours(7.2)), // 90%
                SleepStage(SleepStageType.DEEP, Duration.ofMinutes(48)),  // 10%
                SleepStage(SleepStageType.REM, Duration.ofMinutes(0.0))    // 0%
            )
        )
        
        // Quando
        val efficiency = session.efficiency
        
        // Então: Deve ter penalidade por sono profundo insuficiente
        assertTrue(efficiency < 90.0)
    }
    
    @Test
    fun `deve penalizar eficiência para múltiplos despertares`() {
        // Dado: 3 despertares
        val session = createTestSession(
            stages = listOf(
                SleepStage(SleepStageType.LIGHT, Duration.ofHours(6)),
                SleepStage(SleepStageType.DEEP, Duration.ofHours(1.5)),
                SleepStage(SleepStageType.REM, Duration.ofHours(0.5))
            ),
            wakeCount = 3
        )
        
        // Quando
        val efficiency = session.efficiency
        
        // Então: Deve ter penalidade por múltiplos despertares
        assertTrue(efficiency < 85.0)
    }
    
    @Test
    fun `deve lidar com duração zero sem erros`() {
        // Dado
        val session = createTestSession(
            startTime = now,
            endTime = now, // Duração zero
            stages = emptyList()
        )
        
        // Quando/Então
        assertEquals(0.0, session.lightSleepPercentage, 0.0)
        assertEquals(0.0, session.deepSleepPercentage, 0.0)
        assertEquals(0.0, session.remSleepPercentage, 0.0)
        assertEquals(0.0, session.efficiency, 0.0)
    }
    
    private fun createTestSession(
        id: String = "test-id",
        startTime: ZonedDateTime = now.minusHours(8),
        endTime: ZonedDateTime = now,
        duration: Duration? = null,
        stages: List<SleepStage> = emptyList(),
        isManualEntry: Boolean = true,
        notes: String? = null,
        wakeCount: Int = 0
    ): SleepSession {
        return SleepSession(
            id = id,
            startTime = startTime,
            endTime = endTime,
            stages = stages,
            isManualEntry = isManualEntry,
            notes = notes,
            wakeDuringNightCount = wakeCount
        ).apply {
            // Sobrescreve a duração se fornecida
            if (duration != null) {
                val durationField = this::class.members.find { it.name == "duration" }!!
                @Suppress("UNCHECKED_CAST")
                (durationField as kotlin.reflect.KMutableProperty1<SleepSession, Duration>).set(this, duration)
            }
        }
    }
}
