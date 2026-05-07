package com.lastdone.app.data.local

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromSecondOfDay(value: Long?): LocalTime? = value?.let(LocalTime::ofSecondOfDay)

    @TypeConverter
    fun toSecondOfDay(time: LocalTime?): Long? = time?.toSecondOfDay()?.toLong()

    @TypeConverter
    fun fromIso(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)

    @TypeConverter
    fun toIso(dateTime: LocalDateTime?): String? = dateTime?.toString()
}
