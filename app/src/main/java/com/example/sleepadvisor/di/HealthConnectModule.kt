package com.example.sleepadvisor.di

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import com.example.sleepadvisor.data.HealthConnectRepository
import com.example.sleepadvisor.data.local.dao.ManualSleepSessionDao
import com.example.sleepadvisor.data.repository.SleepRepositoryImpl
import com.example.sleepadvisor.domain.repository.SleepRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HealthConnectModule {
    
    @Provides
    @Singleton
    fun provideHealthConnectClient(
        @ApplicationContext context: Context
    ): HealthConnectClient {
        return HealthConnectClient.getOrCreate(context)
    }
    
    @Provides
    @Singleton
    fun provideSleepRepository(
        healthConnectClient: HealthConnectClient,
        healthConnectRepository: HealthConnectRepository,
        manualSleepSessionDao: ManualSleepSessionDao
    ): SleepRepository {
        return SleepRepositoryImpl(healthConnectClient, healthConnectRepository, manualSleepSessionDao)
    }
    
    @Provides
    @Singleton
    fun provideHealthConnectRepository(
        @ApplicationContext context: Context
    ): HealthConnectRepository {
        return HealthConnectRepository(context)
    }
} 