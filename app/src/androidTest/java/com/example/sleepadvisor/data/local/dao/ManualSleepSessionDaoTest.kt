package com.example.sleepadvisor.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sleepadvisor.data.local.AppDatabase
import com.example.sleepadvisor.data.local.entity.ManualSleepSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

/**
 * Testes de integração para o ManualSleepSessionDao
 * Verifica se as operações de persistência estão funcionando corretamente
 */
@RunWith(AndroidJUnit4::class)
class ManualSleepSessionDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: ManualSleepSessionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Cria um banco de dados em memória para os testes
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // Permite consultas na thread principal para testes
            .build()
        dao = db.manualSleepSessionDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    /**
     * Verifica se conseguimos inserir e recuperar uma sessão de sono
     */
    @Test
    fun insertAndGetSleepSession() = runBlocking {
        // Cria uma sessão de teste
        val now = ZonedDateTime.now()
        val testSession = ManualSleepSessionEntity(
            id = UUID.randomUUID().toString(),
            startTime = now.minusHours(8),
            endTime = now,
            createdAt = now,
            sleepType = "DEEP",
            notes = "Teste de inserção",
            lastModified = now
        )
        
        // Insere no banco
        dao.insertSleepSession(testSession)
        
        // Busca todas as sessões
        val sessions = dao.getAllSleepSessions().first()
        
        // Verifica se a sessão foi inserida corretamente
        assertEquals(1, sessions.size)
        assertEquals(testSession.id, sessions[0].id)
        assertEquals(testSession.notes, sessions[0].notes)
        assertEquals(testSession.startTime.toInstant(), sessions[0].startTime.toInstant())
    }
    
    /**
     * Verifica se conseguimos atualizar as anotações de uma sessão
     */
    @Test
    fun updateSleepSessionNotes() = runBlocking {
        // Cria e insere uma sessão
        val id = UUID.randomUUID().toString()
        val now = ZonedDateTime.now()
        val testSession = ManualSleepSessionEntity(
            id = id,
            startTime = now.minusHours(8),
            endTime = now,
            notes = "Anotação original"
        )
        dao.insertSleepSession(testSession)
        
        // Atualiza as anotações
        val updateTime = now.plusMinutes(10)
        val newNotes = "Anotação atualizada"
        dao.updateNotes(id, newNotes, updateTime)
        
        // Verifica se a atualização foi feita
        val updatedSession = dao.getSleepSessionById(id)
        assertEquals(newNotes, updatedSession?.notes)
        assertEquals(updateTime.toInstant(), updatedSession?.lastModified?.toInstant())
    }
    
    /**
     * Verifica se conseguimos excluir uma sessão
     */
    @Test
    fun deleteSleepSession() = runBlocking {
        // Cria e insere uma sessão
        val testSession = ManualSleepSessionEntity(
            id = UUID.randomUUID().toString(),
            startTime = ZonedDateTime.now().minusHours(8),
            endTime = ZonedDateTime.now()
        )
        dao.insertSleepSession(testSession)
        
        // Exclui a sessão
        dao.deleteSleepSession(testSession)
        
        // Verifica se a sessão foi excluída
        val sessions = dao.getAllSleepSessions().first()
        assertTrue(sessions.isEmpty())
        
        // Tenta buscar por ID
        val deletedSession = dao.getSleepSessionById(testSession.id)
        assertNull(deletedSession)
    }
    
    /**
     * Verifica se conseguimos filtrar por intervalo de datas
     */
    @Test
    fun getSleepSessionsBetweenDates() = runBlocking {
        val zone = ZoneId.systemDefault()
        val today = ZonedDateTime.now(zone).withHour(12).withMinute(0).withSecond(0).withNano(0)
        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)
        val threeDaysAgo = today.minusDays(3)
        
        // Sessões em diferentes dias
        val session1 = ManualSleepSessionEntity(
            id = "1",
            startTime = yesterday.withHour(22),
            endTime = today.withHour(6)
        )
        val session2 = ManualSleepSessionEntity(
            id = "2",
            startTime = twoDaysAgo.withHour(23),
            endTime = yesterday.withHour(7)
        )
        val session3 = ManualSleepSessionEntity(
            id = "3",
            startTime = threeDaysAgo.withHour(21),
            endTime = twoDaysAgo.withHour(5)
        )
        
        // Insere todas as sessões
        dao.insertSleepSessions(listOf(session1, session2, session3))
        
        // Busca apenas as sessões dos últimos 2 dias
        val sessions = dao.getSleepSessionsBetween(
            twoDaysAgo.withHour(0),
            today.withHour(23)
        ).first()
        
        // Deve retornar 2 sessões (session1 e session2)
        assertEquals(2, sessions.size)
        assertTrue(sessions.any { it.id == "1" })
        assertTrue(sessions.any { it.id == "2" })
    }
} 