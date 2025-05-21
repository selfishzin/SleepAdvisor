package com.example.sleepadvisor.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sleepadvisor.data.local.AppDatabase
import com.example.sleepadvisor.data.local.dao.ManualSleepSessionDao
import com.example.sleepadvisor.data.local.entity.ManualSleepSessionEntity
import com.example.sleepadvisor.data.repository.SleepRepositoryImpl
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.repository.SleepRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SleepRepositoryIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ManualSleepSessionDao
    private lateinit var repository: SleepRepository
    private val mockHealthConnectClient = mock(androidx.health.connect.client.HealthConnectClient::class.java)

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        dao = database.manualSleepSessionDao()
        repository = SleepRepositoryImpl(mockHealthConnectClient, dao)
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun testManualSleepSessionIsStoredAndRetrieved() = runBlocking {
        // DADO: Horários de início e fim para a sessão de sono
        val startTime = ZonedDateTime.now().minusHours(8)
        val endTime = ZonedDateTime.now()
        val notes = "Notas de teste"
        val wakeDuringNightCount = 2

        // QUANDO: Salvamos uma sessão de sono manual
        repository.saveManualSleepSession(
            startTime = startTime,
            endTime = endTime,
            lightSleepPercentage = 50.0,
            deepSleepPercentage = 30.0,
            remSleepPercentage = 20.0,
            notes = notes,
            wakeDuringNightCount = wakeDuringNightCount
        )

        // ENTÃO: A sessão deve ser recuperada corretamente
        val sessions = repository.getSleepSessions(
            startTime.minusDays(1),
            endTime.plusDays(1)
        ).first()

        assertEquals(1, sessions.size)
        with(sessions[0]) {
            assertEquals(startTime.toInstant(), this.startTime.toInstant())
            assertEquals(endTime.toInstant(), this.endTime.toInstant())
            assertEquals(notes, this.notes)
            assertEquals(wakeDuringNightCount, this.wakeDuringNightCount)
            assertEquals(50.0, this.lightSleepPercentage, 0.1)
            assertEquals(30.0, this.deepSleepPercentage, 0.1)
            assertEquals(20.0, this.remSleepPercentage, 0.1)
            assertTrue(this.isManualEntry)
        }
    }

    @Test
    fun testUpdateManualSleepSession() = runBlocking {
        // DADO: Uma sessão de sono já salva
        val id = UUID.randomUUID().toString()
        val startTime = ZonedDateTime.now().minusHours(8)
        val endTime = ZonedDateTime.now()
        
        dao.insertSleepSession(
            ManualSleepSessionEntity(
                id = id,
                startTime = startTime,
                endTime = endTime,
                lightSleepPercentage = 50.0,
                deepSleepPercentage = 30.0,
                remSleepPercentage = 20.0,
                notes = "Notas originais",
                wakeDuringNightCount = 1
            )
        )

        // QUANDO: Atualizamos a sessão
        repository.updateManualSleepSession(
            id = id,
            startTime = startTime.plusMinutes(30),
            endTime = endTime.minusMinutes(30),
            lightSleepPercentage = 40.0,
            deepSleepPercentage = 40.0,
            remSleepPercentage = 20.0,
            notes = "Notas atualizadas",
            wakeDuringNightCount = 3
        )

        // ENTÃO: As alterações devem ser salvas
        val updatedSessions = repository.getSleepSessions(
            startTime.minusDays(1),
            endTime.plusDays(1)
        ).first()

        assertEquals(1, updatedSessions.size)
        with(updatedSessions[0]) {
            assertEquals(startTime.plusMinutes(30).toInstant(), this.startTime.toInstant())
            assertEquals(endTime.minusMinutes(30).toInstant(), this.endTime.toInstant())
            assertEquals("Notas atualizadas", this.notes)
            assertEquals(3, this.wakeDuringNightCount)
            assertEquals(40.0, this.lightSleepPercentage, 0.1)
            assertEquals(40.0, this.deepSleepPercentage, 0.1)
            assertEquals(20.0, this.remSleepPercentage, 0.1)
        }
    }

    @Test
    fun testDuplicateEntriesHandling() = runBlocking {
        // DADO: Uma sessão existente para um determinado dia
        val today = ZonedDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0)
        val todayStart = today.withHour(0).withMinute(0).withSecond(0).withNano(0)
        val todayEnd = today.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999)
        
        val firstSession = ManualSleepSessionEntity(
            id = UUID.randomUUID().toString(),
            startTime = today.minusHours(8),
            endTime = today,
            lightSleepPercentage = 50.0,
            deepSleepPercentage = 30.0,
            remSleepPercentage = 20.0,
            notes = "Sessão original",
            wakeDuringNightCount = 1
        )
        
        dao.insertSleepSession(firstSession)

        // QUANDO: Verificamos sessões para o mesmo dia
        val hasDuplicateEntry = repository.hasSleepSessionForDay(today)

        // ENTÃO: Deve indicar que existe uma sessão
        assertTrue(hasDuplicateEntry)

        // QUANDO: Tentamos salvar uma nova sessão para o mesmo dia
        val secondSessionId = UUID.randomUUID().toString()
        val secondSession = ManualSleepSessionEntity(
            id = secondSessionId,
            startTime = today.minusHours(7),
            endTime = today.plusHours(1),
            lightSleepPercentage = 40.0,
            deepSleepPercentage = 40.0,
            remSleepPercentage = 20.0,
            notes = "Segunda sessão",
            wakeDuringNightCount = 2
        )
        
        dao.insertSleepSession(secondSession)

        // ENTÃO: Ambas as sessões devem estar presentes, sem conflito
        val sessionsForDay = repository.getSleepSessions(todayStart, todayEnd).first()
        assertEquals(2, sessionsForDay.size)
    }

    @Test
    fun testDeleteManualSleepSession() = runBlocking {
        // DADO: Uma sessão de sono já salva
        val id = UUID.randomUUID().toString()
        val startTime = ZonedDateTime.now().minusHours(8)
        val endTime = ZonedDateTime.now()
        
        dao.insertSleepSession(
            ManualSleepSessionEntity(
                id = id,
                startTime = startTime,
                endTime = endTime,
                lightSleepPercentage = 50.0,
                deepSleepPercentage = 30.0,
                remSleepPercentage = 20.0,
                notes = "Notas originais",
                wakeDuringNightCount = 1
            )
        )

        // QUANDO: Excluímos a sessão
        repository.deleteSleepSession(id)

        // ENTÃO: A sessão não deve mais existir
        val sessions = repository.getSleepSessions(
            startTime.minusDays(1),
            endTime.plusDays(1)
        ).first()

        assertEquals(0, sessions.size)
    }

    @Test
    fun testGetSessionById() = runBlocking {
        // DADO: Uma sessão de sono já salva
        val id = UUID.randomUUID().toString()
        val startTime = ZonedDateTime.now().minusHours(8)
        val endTime = ZonedDateTime.now()
        
        dao.insertSleepSession(
            ManualSleepSessionEntity(
                id = id,
                startTime = startTime,
                endTime = endTime,
                lightSleepPercentage = 50.0,
                deepSleepPercentage = 30.0,
                remSleepPercentage = 20.0,
                notes = "Notas específicas",
                wakeDuringNightCount = 1
            )
        )

        // QUANDO: Buscamos a sessão pelo ID
        val session = repository.getSleepSessionById(id)

        // ENTÃO: A sessão correta deve ser retornada
        assertNotNull(session)
        assertEquals(id, session?.id)
        assertEquals("Notas específicas", session?.notes)
    }

    @Test
    fun testGetConsolidatedSleepData() = runBlocking {
        // DADO: Múltiplas sessões de sono para vários dias
        val today = ZonedDateTime.now()
        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)
        
        // Dia 1 - sessão única
        val session1 = ManualSleepSessionEntity(
            id = UUID.randomUUID().toString(),
            startTime = twoDaysAgo.minusHours(8),
            endTime = twoDaysAgo,
            lightSleepPercentage = 50.0,
            deepSleepPercentage = 30.0,
            remSleepPercentage = 20.0,
            notes = "Dia 1",
            wakeDuringNightCount = 1
        )
        
        // Dia 2 - múltiplas sessões (fragmentadas)
        val session2a = ManualSleepSessionEntity(
            id = UUID.randomUUID().toString(),
            startTime = yesterday.minusHours(8),
            endTime = yesterday.minusHours(4),
            lightSleepPercentage = 60.0,
            deepSleepPercentage = 20.0,
            remSleepPercentage = 20.0,
            notes = "Dia 2 - parte 1",
            wakeDuringNightCount = 2
        )
        
        val session2b = ManualSleepSessionEntity(
            id = UUID.randomUUID().toString(),
            startTime = yesterday.minusHours(3),
            endTime = yesterday,
            lightSleepPercentage = 40.0,
            deepSleepPercentage = 40.0,
            remSleepPercentage = 20.0,
            notes = "Dia 2 - parte 2",
            wakeDuringNightCount = 1
        )
        
        // Inserir todas as sessões
        dao.insertSleepSessions(listOf(session1, session2a, session2b))

        // QUANDO: Buscamos dados consolidados
        val consolidatedData = repository.getConsolidatedSleepSessions(
            twoDaysAgo.minusDays(1),
            today.plusDays(1)
        ).first()

        // ENTÃO: Deve haver 2 entradas, uma para cada dia
        assertEquals(2, consolidatedData.size)
        
        // A entrada para o dia 2 deve combinar as duas sessões fragmentadas
        val day2Session = consolidatedData.find { it.startTime.toLocalDate() == yesterday.toLocalDate() }
        assertNotNull(day2Session)
        assertEquals(7, day2Session?.duration?.toHours())
        assertEquals(3, day2Session?.wakeDuringNightCount) // Soma dos despertares
    }
}
