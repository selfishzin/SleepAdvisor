package com.example.sleepadvisor.di

import android.content.Context
import com.example.sleepadvisor.data.local.AppDatabase
import com.example.sleepadvisor.data.local.dao.ManualSleepSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)
    }
    
    @Provides
    fun provideManualSleepSessionDao(
        database: AppDatabase
    ): ManualSleepSessionDao {
        return database.manualSleepSessionDao()
    }
} 