package com.example.sleepadvisor.performance

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sleepadvisor.data.local.AppDatabase
import com.example.sleepadvisor.data.local.dao.ManualSleepSessionDao
import com.example.sleepadvisor.data.local.entity.ManualSleepSessionEntity
import com.example.sleepadvisor.data.repository.SleepRepositoryImpl
import com.example.sleepadvisor.domain.repository.SleepRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class PerformanceTest {

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
    fun testBulkInsertPerformance() = runBlocking {
        // DADO: Preparamos 60 sessões diárias para inserção
        val today = ZonedDateTime.now()
        val sessions = (1..60).map { day ->
            ManualSleepSessionEntity(
                id = UUID.randomUUID().toString(),
                startTime = today.minusDays(day.toLong()).minusHours(8),
                endTime = today.minusDays(day.toLong()),
                lightSleepPercentage = 50.0,
                deepSleepPercentage = 30.0,
                remSleepPercentage = 20.0,
                notes = "Dia $day",
                wakeDuringNightCount = (0..3).random()
            )
        }
        
        // QUANDO: Inserimos todas as sessões e medimos o tempo
        val insertTime = measureTimeMillis {
            dao.insertSleepSessions(sessions)
        }
        
        // ENTÃO: A operação deve ser concluída em tempo razoável
        println("Tempo para inserir 60 sessões: $insertTime ms")
        
        // Em um dispositivo moderno, isso geralmente deve ser inferior a 500ms
        assertTrue("Inserção em massa muito lenta: $insertTime ms", insertTime < 3000)
        
        // Verifica se todas as sessões foram salvas
        val savedSessions = dao.getAllSleepSessions().first()
        assertEquals(60, savedSessions.size)
    }
    
    @Test
    fun testQueryPerformance() = runBlocking {
        // DADO: Um banco de dados com 60 sessões
        val today = ZonedDateTime.now()
        val sessions = (1..60).map { day ->
            ManualSleepSessionEntity(
                id = UUID.randomUUID().toString(),
                startTime = today.minusDays(day.toLong()).minusHours(8),
                endTime = today.minusDays(day.toLong()),
                lightSleepPercentage = 50.0,
                deepSleepPercentage = 30.0,
                remSleepPercentage = 20.0,
                notes = "Dia $day",
                wakeDuringNightCount = (0..3).random()
            )
        }
        
        dao.insertSleepSessions(sessions)
        
        // QUANDO: Consultamos todas as sessões da última semana e medimos o tempo
        val queryTime = measureTimeMillis {
            val lastWeekSessions = repository.getSleepSessions(
                today.minusDays(7),
                today
            ).first()
            
            // Verifica se há 7 sessões na última semana
            assertEquals(7, lastWeekSessions.size)
        }
        
        // ENTÃO: A consulta deve ser rápida
        println("Tempo para consultar sessões da última semana: $queryTime ms")
        assertTrue("Consulta muito lenta: $queryTime ms", queryTime < 1000)
    }
    
    @Test
    fun testConsolidatedDataPerformance() = runBlocking {
        // DADO: Um banco de dados com 30 sessões, incluindo múltiplas sessões por dia
        val today = ZonedDateTime.now()
        val sessions = mutableListOf<ManualSleepSessionEntity>()
        
        // Cria sessões normais para 20 dias
        (1..20).forEach { day ->
            sessions.add(
                ManualSleepSessionEntity(
                    id = UUID.randomUUID().toString(),
                    startTime = today.minusDays(day.toLong()).minusHours(8),
                    endTime = today.minusDays(day.toLong()),
                    lightSleepPercentage = 50.0,
                    deepSleepPercentage = 30.0,
                    remSleepPercentage = 20.0,
                    notes = "Dia $day - Sessão única",
                    wakeDuringNightCount = (0..3).random()
                )
            )
        }
        
        // Cria sessões fragmentadas para 5 dias (cada dia tem 2 sessões)
        (21..25).forEach { day ->
            // Primeira parte da noite
            sessions.add(
                ManualSleepSessionEntity(
                    id = UUID.randomUUID().toString(),
                    startTime = today.minusDays(day.toLong()).minusHours(8),
                    endTime = today.minusDays(day.toLong()).minusHours(4),
                    lightSleepPercentage = 60.0,
                    deepSleepPercentage = 20.0,
                    remSleepPercentage = 20.0,
                    notes = "Dia $day - Parte 1",
                    wakeDuringNightCount = 1
                )
            )
            
            // Segunda parte da noite
            sessions.add(
                ManualSleepSessionEntity(
                    id = UUID.randomUUID().toString(),
                    startTime = today.minusDays(day.toLong()).minusHours(3),
                    endTime = today.minusDays(day.toLong()),
                    lightSleepPercentage = 40.0,
                    deepSleepPercentage = 40.0,
                    remSleepPercentage = 20.0,
                    notes = "Dia $day - Parte 2",
                    wakeDuringNightCount = 1
                )
            )
        }
        
        dao.insertSleepSessions(sessions)
        
        // QUANDO: Obtemos os dados consolidados e medimos o tempo
        val consolidationTime = measureTimeMillis {
            val consolidatedData = repository.getConsolidatedSleepSessions(
                today.minusDays(30),
                today
            ).first()
            
            // Deve haver 25 dias de dados (20 dias com sessão única + 5 dias com sessões consolidadas)
            assertEquals(25, consolidatedData.size)
        }
        
        // ENTÃO: A consolidação deve ser rápida
        println("Tempo para consolidar 30 sessões: $consolidationTime ms")
        assertTrue("Consolidação de dados muito lenta: $consolidationTime ms", consolidationTime < 2000)
    }
    
    @Test
    fun testStressWithInvalidData() = runBlocking {
        // DADO: Tentamos inserir sessões com dados extremos ou inválidos
        val today = ZonedDateTime.now()
        
        // 1. Sessão com horários invertidos (fim antes do início)
        val invertedSession = ManualSleepSessionEntity(
            id = UUID.randomUUID().toString(),
            startTime = today,
            endTime = today.minusHours(8), // Término antes do início
            lightSleepPercentage = 50.0,
            deepSleepPercentage = 30.0,
            remSleepPercentage = 20.0,
            notes = "Sessão com horários invertidos",
            wakeDuringNightCount = 1
        )
        
        // 2. Sessão com porcentagens que somam mais de 100%
        val invalidPercentageSession = ManualSleepSessionEntity(
            id = UUID.randomUUID().toString(),
            startTime = today.minusHours(8),
            endTime = today,
            lightSleepPercentage = 50.0,
            deepSleepPercentage = 30.0,
            remSleepPercentage = 30.0, // Soma: 110%
            notes = "Sessão com porcentagens inválidas",
            wakeDuringNightCount = 1
        )
        
        // 3. Sessão com valor extremamente alto de despertares
        val extremeWakeCountSession = ManualSleepSessionEntity(
            id = UUID.randomUUID().toString(),
            startTime = today.minusHours(8),
            endTime = today,
            lightSleepPercentage = 50.0,
            deepSleepPercentage = 30.0,
            remSleepPercentage = 20.0,
            notes = "Sessão com muitos despertares",
            wakeDuringNightCount = 999
        )
        
        // QUANDO: Tentamos inserir essas sessões
        
        // A sessão com horários invertidos deve falhar na validação do repositório
        try {
            repository.saveManualSleepSession(
                id = invertedSession.id,
                startTime = invertedSession.startTime,
                endTime = invertedSession.endTime,
                lightSleepPercentage = invertedSession.lightSleepPercentage,
                deepSleepPercentage = invertedSession.deepSleepPercentage,
                remSleepPercentage = invertedSession.remSleepPercentage,
                notes = invertedSession.notes,
                wakeDuringNightCount = invertedSession.wakeDuringNightCount
            )
            fail("Deveria falhar ao inserir sessão com horários invertidos")
        } catch (e: Exception) {
            // Esperado
        }
        
        // As outras sessões, mesmo com dados extremos, devem ser salvas
        // (o app deve ser resiliente, mesmo que os dados sejam estranhos)
        
        dao.insertSleepSession(invalidPercentageSession)
        dao.insertSleepSession(extremeWakeCountSession)
        
        // ENTÃO: As sessões válidas devem ser recuperáveis
        val sessions = dao.getAllSleepSessions().first()
        
        // Devem haver 2 sessões (as duas últimas)
        assertEquals(2, sessions.size)
    }
    
    @Test
    fun testRapidSuccessiveOperations() = runBlocking {
        // DADO: Uma sessão de sono existente
        val sessionId = UUID.randomUUID().toString()
        val today = ZonedDateTime.now()
        
        dao.insertSleepSession(
            ManualSleepSessionEntity(
                id = sessionId,
                startTime = today.minusHours(8),
                endTime = today,
                lightSleepPercentage = 50.0,
                deepSleepPercentage = 30.0,
                remSleepPercentage = 20.0,
                notes = "Sessão original",
                wakeDuringNightCount = 1
            )
        )
        
        // QUANDO: Realizamos 50 atualizações rápidas e sucessivas
        val updateTime = measureTimeMillis {
            repeat(50) { i ->
                dao.updateSleepSession(
                    ManualSleepSessionEntity(
                        id = sessionId,
                        startTime = today.minusHours(8),
                        endTime = today,
                        lightSleepPercentage = 50.0,
                        deepSleepPercentage = 30.0,
                        remSleepPercentage = 20.0,
                        notes = "Atualização $i",
                        wakeDuringNightCount = 1 + (i % 5)
                    )
                )
            }
        }
        
        // ENTÃO: O banco de dados deve ser capaz de lidar com as operações rapidamente
        println("Tempo para 50 atualizações sucessivas: $updateTime ms")
        assertTrue("Atualizações sucessivas muito lentas: $updateTime ms", updateTime < 5000)
        
        // Verifica se a última atualização foi salva corretamente
        val updatedSession = dao.getSleepSessionById(sessionId)
        assertEquals("Atualização 49", updatedSession?.notes)
    }
}
