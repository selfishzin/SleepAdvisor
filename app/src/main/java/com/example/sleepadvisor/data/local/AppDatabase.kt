package com.example.sleepadvisor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sleepadvisor.data.local.converter.ZonedDateTimeConverter
import com.example.sleepadvisor.data.local.dao.ManualSleepSessionDao
import com.example.sleepadvisor.data.local.entity.ManualSleepSessionEntity
import java.time.ZonedDateTime

/**
 * Classe de banco de dados Room que gerencia o banco de dados local do SleepAdvisor
 * Atualmente contém apenas a tabela de sessões de sono manuais
 */
@Database(
    entities = [ManualSleepSessionEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(ZonedDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * DAO para acessar as sessões de sono manuais
     */
    abstract fun manualSleepSessionDao(): ManualSleepSessionDao

    companion object {
        private const val DATABASE_NAME = "sleep_advisor_db"

        /**
         * Migração da versão 1 para 2 do banco de dados
         * Adiciona os campos notes e lastModified à tabela manual_sleep_sessions
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Adiciona as novas colunas
                database.execSQL("ALTER TABLE manual_sleep_sessions ADD COLUMN notes TEXT")
                database.execSQL("ALTER TABLE manual_sleep_sessions ADD COLUMN lastModified TEXT NOT NULL DEFAULT '${ZonedDateTime.now()}'")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtém uma instância do banco de dados, criando-a se necessário
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 