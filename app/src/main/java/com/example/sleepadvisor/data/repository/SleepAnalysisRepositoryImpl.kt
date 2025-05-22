package com.example.sleepadvisor.data.repository

import com.example.sleepadvisor.domain.model.SleepAdvice
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepSource
import com.example.sleepadvisor.domain.repository.SleepAnalysisRepository
import com.example.sleepadvisor.domain.service.AIAnalysisService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepAnalysisRepositoryImpl @Inject constructor(
    private val aiAnalysisService: AIAnalysisService
) : SleepAnalysisRepository {
    
    private val TAG = "SleepAnalysisRepo"

    override suspend fun analyzeSleepSession(sleepSession: SleepSession): SleepAdvice {
        android.util.Log.d(TAG, "Iniciando análise da sessão de sono: ${sleepSession.id}")
        val advice = aiAnalysisService.analyzeSleepData(sleepSession)
        android.util.Log.d(TAG, "Análise concluída, retornando recomendações")
        return advice
    }
    
    override suspend fun analyzeWeeklySleepData(sessions: List<SleepSession>): SleepAdvice {
        // Para anu00e1lise semanal, podemos usar a mu00e9dia dos dados ou implementar lu00f3gica especu00edfica
        if (sessions.isEmpty()) {
            return SleepAdvice(
                mainAdvice = "Nu00e3o hu00e1 dados suficientes para anu00e1lise semanal.",
                customRecommendations = listOf("Registre seu sono regularmente para obter recomendau00e7u00f5es personalizadas.")
            )
        }
        
        // Calcular mu00e9dias
        val avgDeepSleep = sessions.map { it.deepSleepPercentage }.average()
        val avgRemSleep = sessions.map { it.remSleepPercentage }.average()
        val avgLightSleep = sessions.map { it.lightSleepPercentage }.average()
        val avgEfficiency = sessions.map { it.efficiency }.average()
        val avgWakeCount = sessions.map { it.wakeDuringNightCount }.average().toInt()
        val avgDuration = sessions.map { it.duration.toMinutes() }.average().toLong()
        
        // Criar sessu00e3o "mu00e9dia" para anu00e1lise
        val averageSession = SleepSession(
            id = "weekly_analysis_${System.currentTimeMillis()}",
            startTime = sessions.first().startTime,
            endTime = sessions.first().endTime,
            title = "Anu00e1lise Semanal",
            notes = "Anu00e1lise mu00e9dia de ${sessions.size} sessu00f5es de sono",
            stages = emptyList(), // Nu00e3o relevante para anu00e1lise mu00e9dia
            wakeDuringNightCount = avgWakeCount,
            heartRateSamples = emptyList(), // Nu00e3o disponu00edvel na anu00e1lise mu00e9dia
            deepSleepPercentage = avgDeepSleep,
            remSleepPercentage = avgRemSleep,
            lightSleepPercentage = avgLightSleep,
            efficiency = avgEfficiency,
            source = SleepSource.SIMULATION
        )
        
        return aiAnalysisService.analyzeSleepData(averageSession)
    }
}
