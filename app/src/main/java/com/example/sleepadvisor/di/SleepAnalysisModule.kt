package com.example.sleepadvisor.di

import com.example.sleepadvisor.domain.repository.SleepRepository
import com.example.sleepadvisor.domain.usecase.AnalyzeSleepQualityUseCase
import com.example.sleepadvisor.domain.usecase.AnalyzeSleepTrendsUseCase
import com.example.sleepadvisor.domain.usecase.DetectNapsUseCase
import com.example.sleepadvisor.domain.usecase.GenerateSleepRecommendationsUseCase
import com.example.sleepadvisor.domain.usecase.GetSleepSessionDetailsUseCase
import com.example.sleepadvisor.domain.usecase.GetSleepSessionsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt que fornece casos de uso relacionados à análise avançada de sono.
 * Segue o padrão de arquitetura Clean Architecture, onde os casos de uso são injetados nos ViewModels.
 */
@Module
@InstallIn(SingletonComponent::class)
object SleepAnalysisModule {
    
    /**
     * Fornece o caso de uso para obter sessões de sono.
     * Este caso de uso é responsável por recuperar, filtrar e consolidar sessões de sono.
     */
    @Provides
    @Singleton
    fun provideGetSleepSessionsUseCase(
        repository: SleepRepository
    ): GetSleepSessionsUseCase {
        return GetSleepSessionsUseCase(repository)
    }
    
    /**
     * Fornece o caso de uso para analisar a qualidade do sono.
     * Este caso de uso implementa algoritmos avançados para calcular a pontuação de qualidade
     * do sono com base em múltiplos fatores, como eficiência, estágios, duração e despertares.
     */
    @Provides
    @Singleton
    fun provideAnalyzeSleepQualityUseCase(): AnalyzeSleepQualityUseCase {
        return AnalyzeSleepQualityUseCase()
    }
    
    /**
     * Fornece o caso de uso para detectar e analisar sonecas.
     * Este caso de uso identifica sonecas a partir das sessões de sono e fornece
     * análises detalhadas sobre sua qualidade e impacto no sono noturno.
     */
    @Provides
    @Singleton
    fun provideDetectNapsUseCase(): DetectNapsUseCase {
        return DetectNapsUseCase()
    }
    
    /**
     * Fornece o caso de uso para analisar tendências de sono.
     * Este caso de uso analisa padrões semanais, consistência, e diferenças
     * entre dias de semana e fins de semana.
     */
    @Provides
    @Singleton
    fun provideAnalyzeSleepTrendsUseCase(): AnalyzeSleepTrendsUseCase {
        return AnalyzeSleepTrendsUseCase()
    }
    
    /**
     * Fornece o caso de uso para gerar recomendações personalizadas de sono.
     * Este caso de uso combina os resultados de múltiplas análises para criar
     * recomendações abrangentes e personalizadas.
     */
    @Provides
    @Singleton
    fun provideGenerateSleepRecommendationsUseCase(): GenerateSleepRecommendationsUseCase {
        return GenerateSleepRecommendationsUseCase()
    }
    
    /**
     * Fornece o caso de uso para obter os detalhes de uma sessão de sono específica.
     */
    @Provides
    @Singleton
    fun provideGetSleepSessionDetailsUseCase(
        repository: SleepRepository
    ): GetSleepSessionDetailsUseCase {
        return GetSleepSessionDetailsUseCase(repository)
    }
}
