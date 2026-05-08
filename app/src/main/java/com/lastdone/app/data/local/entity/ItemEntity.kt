package com.lastdone.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(
    tableName = "items",
    indices = [Index("categoryId")]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long,
    val lastDoneDate: LocalDate,
    val intervalDays: Int,
    val icon: String? = null,
    val memo: String? = null,
    val notifyEnabled: Boolean = false,
    val notifyTime: LocalTime? = null,
    val notifyPreset: String = "MORNING_OF",
    val repeatIntervalMinutes: Int = 0,
    val lastNotifiedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime
)
