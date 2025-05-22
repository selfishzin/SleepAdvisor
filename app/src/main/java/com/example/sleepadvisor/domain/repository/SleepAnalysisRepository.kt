package com.example.sleepadvisor.domain.repository

import com.example.sleepadvisor.domain.model.SleepAdvice
import com.example.sleepadvisor.domain.model.SleepSession

/**
 * Repositu00f3rio responsu00e1vel pela anu00e1lise de dados de sono
 */
interface SleepAnalysisRepository {
    
    /**
     * Analisa uma sessu00e3o de sono individual
     * @param sleepSession A sessu00e3o de sono a ser analisada
     * @return Conselhos e recomendau00e7u00f5es personalizadas
     */
    suspend fun analyzeSleepSession(sleepSession: SleepSession): SleepAdvice
    
    /**
     * Analisa dados de sono de uma semana
     * @param sessions Lista de sessu00f5es de sono a serem analisadas
     * @return Conselhos e recomendau00e7u00f5es personalizadas baseadas nos dados semanais
     */
    suspend fun analyzeWeeklySleepData(sessions: List<SleepSession>): SleepAdvice
}
