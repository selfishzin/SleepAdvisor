package com.example.sleepadvisor.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class HealthConnectRepository(private val context: Context) {
    private var healthConnectClient: HealthConnectClient? = null

    // Permissões necessárias
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
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
        if (checkAvailability() == HealthConnectClient.SDK_AVAILABLE) {
            healthConnectClient = HealthConnectClient.getOrCreate(context)
        }
        return healthConnectClient
    }

    // Verifica se todas as permissões foram concedidas
    suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    // Lê dados de passos das últimas 24 horas
    suspend fun readStepsData(): Flow<List<StepsRecord>> = flow {
        try {
            val client = healthConnectClient ?: throw IllegalStateException("Health Connect client não inicializado")
            
            val stepsPermissions = setOf(
                HealthPermission.getReadPermission(StepsRecord::class)
            )
            if (!client.permissionController.getGrantedPermissions().containsAll(stepsPermissions)) {
                throw SecurityException("Permissões de leitura de passos não concedidas")
            }

            val endTime = Instant.now()
            val startTime = endTime.minus(24, ChronoUnit.HOURS)

            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            emit(response.records)
        } catch (e: Exception) {
            throw e
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

    companion object {
        const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
    }
} 