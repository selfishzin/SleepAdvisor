package com.example.sleepadvisor.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SleepStageRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class HealthConnectRepository(private val context: Context) {
    companion object {
        private const val TAG = "HealthConnectRepo"
        const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
    }
    
    private var healthConnectClient: HealthConnectClient? = null

    // Permissões necessárias
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(SleepStageRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    // Verifica se o Health Connect está instalado
    fun isProviderInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(HEALTH_CONNECT_PACKAGE, PackageManager.GET_META_DATA)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    // Obtém a intent para instalar o Health Connect
    fun getHealthConnectIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setPackage("com.android.vending")
            data = android.net.Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    // Verifica disponibilidade do Health Connect
    fun checkAvailability(): Int {
        return HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE)
    }

    // Inicializa o cliente se disponível
    fun initializeClient(): HealthConnectClient? {
        return try {
            val availability = checkAvailability()
            Log.d(TAG, "Status de disponibilidade do Health Connect: $availability")
            
            if (availability == HealthConnectClient.SDK_AVAILABLE) {
                healthConnectClient = HealthConnectClient.getOrCreate(context)
                Log.d(TAG, "Cliente do Health Connect inicializado com sucesso")
                healthConnectClient
            } else {
                Log.w(TAG, "Health Connect não está disponível. Status: $availability")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar o cliente do Health Connect: ${e.message}", e)
            null
        }
    }

    /**
     * Verifica se todas as permissões necessárias foram concedidas.
     * 
     * @return `true` se todas as permissões necessárias foram concedidas, `false` caso contrário.
     */
    suspend fun hasAllPermissions(): Boolean {
        return try {
            val client = healthConnectClient ?: run {
                Log.w(TAG, "Cliente do Health Connect não está disponível")
                return false
            }
            
            val grantedPermissions = client.permissionController.getGrantedPermissions()
            val hasAll = grantedPermissions.containsAll(permissions)
            
            Log.d(TAG, "Permissões concedidas: $grantedPermissions")
            Log.d(TAG, "Permissões necessárias: $permissions")
            Log.d(TAG, "Todas as permissões concedidas? $hasAll")
            
            hasAll
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar permissões: ${e.message}", e)
            false
        }
    }

    /**
     * Lê dados de frequência cardíaca para um determinado intervalo de tempo.
     *
     * @param startTimeZoned Início do intervalo.
     * @param endTimeZoned Fim do intervalo.
     * @return Flow de lista de samples de frequência cardíaca.
     */
    suspend fun readHeartRateData(
        startTimeZoned: ZonedDateTime,
        endTimeZoned: ZonedDateTime
    ): Flow<List<HeartRateRecord.Sample>> = flow {
        try {
            val client = healthConnectClient ?: throw IllegalStateException("Health Connect client não inicializado")

            val heartRatePermissions = setOf(HealthPermission.getReadPermission(HeartRateRecord::class))
            if (!client.permissionController.getGrantedPermissions().containsAll(heartRatePermissions)) {
                throw SecurityException("Permissão de leitura de frequência cardíaca não concedida")
            }

            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTimeZoned.toInstant(), endTimeZoned.toInstant())
            )
            val response = client.readRecords(request)
            val allSamples = response.records.flatMap { it.samples }
            emit(allSamples)
        } catch (e: Exception) {
            throw e
        }
    }


} 