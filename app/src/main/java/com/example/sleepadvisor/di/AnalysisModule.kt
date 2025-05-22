package com.example.sleepadvisor.di

import com.example.sleepadvisor.data.repository.SleepAnalysisRepositoryImpl
import com.example.sleepadvisor.domain.repository.SleepAnalysisRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalysisModule {
    
    @Binds
    @Singleton
    abstract fun bindSleepAnalysisRepository(
        repository: SleepAnalysisRepositoryImpl
    ): SleepAnalysisRepository
}
