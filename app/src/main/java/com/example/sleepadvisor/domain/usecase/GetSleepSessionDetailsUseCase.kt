package com.example.sleepadvisor.domain.usecase

import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.repository.SleepRepository
import javax.inject.Inject

/**
 * Caso de uso para obter os detalhes completos de uma sessão de sono específica.
 */
class GetSleepSessionDetailsUseCase @Inject constructor(
    private val repository: SleepRepository
) {
    /**
     * Obtém uma sessão de sono pelo seu ID.
     *
     * @param sessionId O ID da sessão de sono a ser recuperada.
     * @return A [SleepSession] correspondente, ou null se não encontrada.
     */
    suspend operator fun invoke(sessionId: String): SleepSession? {
        return repository.getSleepSessionById(sessionId)
    }
}
