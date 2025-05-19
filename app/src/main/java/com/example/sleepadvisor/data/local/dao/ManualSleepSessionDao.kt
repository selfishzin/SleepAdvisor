package com.example.sleepadvisor.data.local.dao

import androidx.room.*
import com.example.sleepadvisor.data.local.entity.ManualSleepSessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime

/**
 * DAO (Data Access Object) para gerenciar as operações de banco de dados
 * relacionadas às sessões de sono adicionadas manualmente
 */
@Dao
interface ManualSleepSessionDao {
    /**
     * Obtém todas as sessões de sono dentro de um intervalo de tempo específico
     * 
     * @param startTime Data e hora de início do intervalo
     * @param endTime Data e hora de fim do intervalo
     * @return Flow de lista de sessões encontradas, ordenadas por data de início decrescente
     */
    @Query("SELECT * FROM manual_sleep_sessions WHERE startTime >= :startTime AND endTime <= :endTime ORDER BY startTime DESC")
    fun getSleepSessionsBetween(startTime: ZonedDateTime, endTime: ZonedDateTime): Flow<List<ManualSleepSessionEntity>>
    
    /**
     * Obtém todas as sessões de sono manuais, ordenadas por data de início
     * 
     * @return Flow de lista de todas as sessões manuais
     */
    @Query("SELECT * FROM manual_sleep_sessions ORDER BY startTime DESC")
    fun getAllSleepSessions(): Flow<List<ManualSleepSessionEntity>>
    
    /**
     * Insere ou atualiza uma sessão de sono no banco de dados
     * 
     * @param session A sessão de sono a ser inserida ou atualizada
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepSession(session: ManualSleepSessionEntity)
    
    /**
     * Insere ou atualiza múltiplas sessões de sono no banco de dados
     * 
     * @param sessions Lista de sessões a serem inseridas ou atualizadas
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepSessions(sessions: List<ManualSleepSessionEntity>)
    
    /**
     * Remove uma sessão de sono do banco de dados
     * 
     * @param session A sessão de sono a ser removida
     */
    @Delete
    suspend fun deleteSleepSession(session: ManualSleepSessionEntity)
    
    /**
     * Remove uma sessão de sono pelo seu ID
     * 
     * @param id O ID da sessão a ser removida
     */
    @Query("DELETE FROM manual_sleep_sessions WHERE id = :id")
    suspend fun deleteSleepSessionById(id: String)
    
    /**
     * Obtém uma sessão de sono específica pelo seu ID
     * 
     * @param id O ID da sessão a ser buscada
     * @return A sessão de sono ou null se não encontrada
     */
    @Query("SELECT * FROM manual_sleep_sessions WHERE id = :id")
    suspend fun getSleepSessionById(id: String): ManualSleepSessionEntity?
    
    /**
     * Obtém todas as sessões de sono que começam em uma data específica
     * 
     * @param startDate Início do dia
     * @param endDate Fim do dia
     * @return Flow de lista de sessões para o dia
     */
    @Query("SELECT * FROM manual_sleep_sessions WHERE startTime >= :startDate AND startTime < :endDate")
    fun getSleepSessionsForDate(startDate: ZonedDateTime, endDate: ZonedDateTime): Flow<List<ManualSleepSessionEntity>>
    
    /**
     * Obtém a última sessão de sono registrada com base no horário de término
     * 
     * @return A última sessão de sono ou null se não houver registros
     */
    @Query("SELECT * FROM manual_sleep_sessions ORDER BY endTime DESC LIMIT 1")
    suspend fun getLastSleepSession(): ManualSleepSessionEntity?
    
    /**
     * Atualiza as anotações de uma sessão de sono específica
     * 
     * @param id O ID da sessão
     * @param notes As novas anotações
     * @param lastModified A data de última modificação
     */
    @Query("UPDATE manual_sleep_sessions SET notes = :notes, lastModified = :lastModified WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String?, lastModified: ZonedDateTime)
    
    /**
     * Atualiza uma sessão de sono existente
     * 
     * @param session A sessão de sono a ser atualizada
     */
    @Update
    suspend fun updateSleepSession(session: ManualSleepSessionEntity)
} 