package com.example.sleepadvisor.data.local.converter

import androidx.room.TypeConverter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Conversor para possibilitar o armazenamento de ZonedDateTime no Room
 * Converte entre ZonedDateTime e String para persistência no banco de dados
 */
class ZonedDateTimeConverter {
    @TypeConverter
    fun fromTimestamp(value: String?): ZonedDateTime? {
        return value?.let {
            // Formato padrão do Instant e depois recupera a timezone do sistema
            val instant = Instant.parse(it.substringBefore("@"))
            val zoneId = ZoneId.of(it.substringAfter("@"))
            ZonedDateTime.ofInstant(instant, zoneId)
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: ZonedDateTime?): String? {
        return date?.let {
            // Armazena o Instant e a timezone para reconstruir o ZonedDateTime depois
            "${it.toInstant()}@${it.zone.id}" 
        }
    }
} 