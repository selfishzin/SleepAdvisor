package com.example.sleepadvisor.data.repository

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.sleepadvisor.data.HealthConnectRepository
import com.example.sleepadvisor.data.local.dao.ManualSleepSessionDao
import com.example.sleepadvisor.data.local.entity.ManualSleepSessionEntity
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepSource
import com.example.sleepadvisor.domain.model.SleepStage
import com.example.sleepadvisor.domain.model.SleepStageType
import com.example.sleepadvisor.domain.model.calculateAndUpdateStagePercentages
import com.example.sleepadvisor.domain.repository.SleepRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepRepositoryImpl @Inject constructor(
    private val healthConnectClient: HealthConnectClient,
    private val manualSleepSessionDao: ManualSleepSessionDao,
    private val healthConnectRepository: HealthConnectRepository
) : SleepRepository {
    
    companion object {
        private const val TAG = "SleepRepositoryImpl"
    }
    
    override fun getSleepSessions(
        startTime: ZonedDateTime,
        endTime: ZonedDateTime
    ): Flow<List<SleepSession>> = combine(
        getHealthConnectSessions(startTime, endTime),
        getManualSessions(startTime, endTime)
    ) { hcSessions, manualSessions ->
        (hcSessions + manualSessions)
            .sortedByDescending { it.startTime }
    }
    
    private fun getHealthConnectSessions(
        startTimeZoned: ZonedDateTime,
        endTimeZoned: ZonedDateTime
    ): Flow<List<SleepSession>> = flow {
        Log.d(TAG, "getHealthConnectSessions CHAMADO. De: ${startTimeZoned.toInstant().toEpochMilli()}, Para: ${endTimeZoned.toInstant().toEpochMilli()}")

        try {
            // Verifica se o cliente do Health Connect está disponível
            val client = healthConnectClient ?: run {
                Log.w(TAG, "Cliente do Health Connect não está disponível")
                emit(emptyList())
                return@flow
            }

            // Verifica se as permissões foram concedidas
            val granted = try {
                val grantedPermissions = client.permissionController.getGrantedPermissions()
                val requiredPermissions = healthConnectRepository.permissions
                val hasAllPermissions = grantedPermissions.containsAll(requiredPermissions)
                
                Log.d(TAG, "Permissões concedidas: $grantedPermissions")
                Log.d(TAG, "Permissões necessárias: $requiredPermissions")
                Log.d(TAG, "Todas as permissões concedidas? $hasAllPermissions")
                
                hasAllPermissions
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao verificar permissões: ${e.message}", e)
                false
            }

            if (!granted) {
                Log.w(TAG, "Permissões não concedidas em getHealthConnectSessions. Retornando lista vazia.")
                emit(emptyList())
                return@flow
            }

            Log.d(TAG, "Buscando sessões de sono no Health Connect...")
            
            val timeRange = TimeRangeFilter.between(
                startTimeZoned.toInstant(),
                endTimeZoned.toInstant()
            )
            
            val sessionsRequest = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = timeRange
            )
            
            Log.d(TAG, "Enviando requisição de leitura de sessões...")
            val sessionsResponse = client.readRecords(sessionsRequest)
            val domainSleepSessions = mutableListOf<SleepSession>()
            
            Log.d(TAG, "Total de sessões encontradas: ${sessionsResponse.records.size}")
            
            for (sessionRecord in sessionsResponse.records) {
                try {
                    Log.d(TAG, "Processando sessão: ${sessionRecord.metadata.id} (${sessionRecord.startTime} - ${sessionRecord.endTime})")
                    
                    // Busca estágios do sono usando a API correta
                    val stagesResponse = try {
                        client.readRecord(
                            recordType = SleepSessionRecord::class,
                            recordId = sessionRecord.metadata.id
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao buscar estágios do sono: ${e.message}")
                        null
                    }
                    
                    val stages = stagesResponse?.record?.stages ?: emptyList()
                    Log.d(TAG, "Total de estágios encontrados: ${stages.size}")
                    
                    // Busca batimentos cardíacos
                    val heartRateRequest = ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(
                            sessionRecord.startTime,
                            sessionRecord.endTime
                        )
                    )
                    
                    Log.d(TAG, "Buscando batimentos cardíacos para a sessão ${sessionRecord.metadata.id}")
                    val heartRateResponse = client.readRecords(heartRateRequest)
                    val heartRateSamples = heartRateResponse.records.flatMap { it.samples }
                    Log.d(TAG, "Total de amostras de batimento cardíaco: ${heartRateSamples.size}")
                    
                    // Mapeia os estágios de sono para o domínio
                    val mappedStages = stages.map { stage ->
                        SleepStage(
                            startTime = stage.startTime,
                            endTime = stage.endTime,
                            type = mapHcStageToDomainStage(stage.stage),
                            source = SleepSource.HEALTH_CONNECT
                        )
                    }

                    val wakeCountFromStages = mappedStages.count { it.type == SleepStageType.AWAKE }
                    
                    Log.d(TAG, "Criando objeto SleepSession para a sessão ${sessionRecord.metadata.id}")
                    val sessionWithPercentages = SleepSession(
                        id = sessionRecord.metadata.id,
                        startTime = sessionRecord.startTime,
                        endTime = sessionRecord.endTime,
                        title = sessionRecord.title,
                        notes = sessionRecord.notes,
                        stages = mappedStages,
                        wakeDuringNightCount = wakeCountFromStages,
                        heartRateSamples = heartRateSamples,
                        source = SleepSource.HEALTH_CONNECT
                    ).calculateAndUpdateStagePercentages()
                    
                    domainSleepSessions.add(sessionWithPercentages)
                    Log.d(TAG, "Sessão ${sessionRecord.metadata.id} processada com sucesso")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao processar sessão ${sessionRecord.metadata.id}: ${e.message}", e)
                    // Continua para a próxima sessão mesmo se uma falhar
                }
            }
            
            Log.d(TAG, "Total de sessões processadas com sucesso: ${domainSleepSessions.size}")
            emit(domainSleepSessions)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler sessões do Health Connect: ${e.message}", e)
            emit(emptyList())
        }
    }
    
    private fun mapHcStageToDomainStage(hcStage: Int): SleepStageType {
        return when (hcStage) {
            SleepSessionRecord.STAGE_TYPE_AWAKE -> SleepStageType.AWAKE
            SleepSessionRecord.STAGE_TYPE_LIGHT -> SleepStageType.LIGHT
            SleepSessionRecord.STAGE_TYPE_DEEP -> SleepStageType.DEEP
            SleepSessionRecord.STAGE_TYPE_REM -> SleepStageType.REM
            SleepSessionRecord.STAGE_TYPE_SLEEPING -> SleepStageType.SLEEPING
            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> SleepStageType.OUT_OF_BED
            else -> {
                Log.w(TAG, "Tipo de estágio de sono desconhecido: $hcStage")
                SleepStageType.UNKNOWN
            }
        }
    }
    
    private fun getManualSessions(
        startTime: ZonedDateTime,
        endTime: ZonedDateTime
    ): Flow<List<SleepSession>> = flow {
        try {
            manualSleepSessionDao.getSleepSessionsBetween(startTime, endTime)
                .catch { e -> 
                    Log.e(TAG, "Erro ao ler sessões manuais: ${e.message}", e)
                    emit(emptyList()) 
                }
                .collect { entities ->
                    val sessions = entities.map { entity ->
                        mapEntityToSleepSession(entity)
                    }
                    emit(sessions)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao iniciar fluxo de sessões manuais: ${e.message}", e)
            emit(emptyList())
        }
    }
    
    private fun mapEntityToSleepSession(entity: ManualSleepSessionEntity): SleepSession {
        return SleepSession(
            id = entity.id,
            startTime = entity.startTime.toInstant(),
            endTime = entity.endTime.toInstant(),
            source = SleepSource.MANUAL,
            title = null,
            notes = entity.notes,
            wakeDuringNightCount = entity.wakeDuringNightCount,
            stages = emptyList(),
            efficiency = 0.0,
            deepSleepPercentage = entity.deepSleepPercentage,
            remSleepPercentage = entity.remSleepPercentage,
            lightSleepPercentage = entity.lightSleepPercentage,
            heartRateSamples = emptyList()
        )
    }
    
    override suspend fun getLastSleepSession(): Flow<SleepSession?> = flow {
        try {
            val lastEntity = manualSleepSessionDao.getLastSleepSession()
            emit(lastEntity?.let { mapEntityToSleepSession(it) })
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter última sessão: ${e.message}", e)
            emit(null)
        }
    }
    
    override suspend fun getSleepSessionById(id: String): SleepSession? {
        return try {
            val entity = manualSleepSessionDao.getSleepSessionById(id)
            entity?.let { mapEntityToSleepSession(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar sessão por ID: ${e.message}", e)
            null
        }
    }
    
    override suspend fun addManualSleepSession(
        startTime: ZonedDateTime,
        endTime: ZonedDateTime,
        sleepType: String?,
        notes: String?,
        wakeCount: Int
    ): SleepSession {
        val newEntity = ManualSleepSessionEntity(
            id = UUID.randomUUID().toString(),
            startTime = startTime,
            endTime = endTime,
            notes = notes,
            wakeDuringNightCount = wakeCount,
            createdAt = ZonedDateTime.now(),
            lastModified = ZonedDateTime.now(),
            lightSleepPercentage = 0.0,
            deepSleepPercentage = 0.0,
            remSleepPercentage = 0.0
        )
        manualSleepSessionDao.insertSleepSession(newEntity)
        return mapEntityToSleepSession(newEntity)
    }
    
    override suspend fun deleteManualSleepSession(session: SleepSession) {
        try {
            manualSleepSessionDao.deleteSleepSessionById(session.id)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao deletar sessão manual: ${e.message}", e)
        }
    }
    
    override suspend fun updateManualSleepSessionNotes(sessionId: String, notes: String?) {
        try {
            manualSleepSessionDao.updateNotes(sessionId, notes, ZonedDateTime.now())
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar notas da sessão: ${e.message}", e)
        }
    }
    
    override suspend fun hasPermissions(): Boolean {
        return try {
            healthConnectRepository.hasAllPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar permissões: ${e.message}", e)
            false
        }
    }
    
    override suspend fun requestPermissions() {
        Log.w(TAG, "requestPermissions() não implementado em SleepRepositoryImpl")
    }

    override fun getManualSleepSessionsForDate(date: LocalDate): Flow<List<SleepSession>> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault())
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1)
        return flow {
            manualSleepSessionDao.getSleepSessionsForDate(startOfDay, endOfDay)
                .catch { e -> 
                    Log.e(TAG, "Erro ao buscar sessões manuais por data: ${e.message}", e)
                    emit(emptyList())
                }
                .collect { entities ->
                    emit(entities.map { mapEntityToSleepSession(it) })
                }
        }
    }

    override suspend fun updateManualSleepSession(session: SleepSession) {
        try {
            val existingEntity = manualSleepSessionDao.getSleepSessionById(session.id)
            val entityToUpdate = ManualSleepSessionEntity(
                id = session.id,
                startTime = session.startTime.atZone(ZoneId.systemDefault()),
                endTime = session.endTime.atZone(ZoneId.systemDefault()),
                notes = session.notes,
                wakeDuringNightCount = session.wakeDuringNightCount,
                createdAt = existingEntity?.createdAt ?: ZonedDateTime.now(),
                lastModified = ZonedDateTime.now(),
                lightSleepPercentage = session.lightSleepPercentage ?: 0.0,
                deepSleepPercentage = session.deepSleepPercentage ?: 0.0,
                remSleepPercentage = session.remSleepPercentage ?: 0.0
            )
            manualSleepSessionDao.insertSleepSession(entityToUpdate)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar sessão manual: ${e.message}", e)
        }
    }
}
