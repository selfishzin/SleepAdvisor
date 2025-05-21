package com.example.sleepadvisor.di

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.room.Room
import com.example.sleepadvisor.data.local.AppDatabase
import com.example.sleepadvisor.data.local.dao.ManualSleepSessionDao
import com.example.sleepadvisor.data.repository.SleepRepositoryImpl
import com.example.sleepadvisor.domain.repository.SleepRepository
import com.example.sleepadvisor.domain.service.SleepAIService
import com.example.sleepadvisor.domain.usecase.CalculateCustomSleepStagesUseCase
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // Removido método provideHealthConnectClient para evitar duplicação com HealthConnectModule
    
    // Removido método provideAppDatabase para evitar duplicação com DatabaseModule
    
    // Removido método provideManualSleepSessionDao para evitar duplicação com DatabaseModule
    
    // Removido método provideSleepRepository para evitar duplicação com HealthConnectModule
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
    
    @Provides
    @Singleton
    fun provideSleepAIService(
        okHttpClient: OkHttpClient, 
        gson: Gson,
        calculateCustomSleepStagesUseCase: CalculateCustomSleepStagesUseCase
    ): SleepAIService {
        return SleepAIService(okHttpClient, gson, calculateCustomSleepStagesUseCase)
    }
} 