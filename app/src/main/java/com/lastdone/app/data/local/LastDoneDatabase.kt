package com.lastdone.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lastdone.app.data.local.dao.CategoryDao
import com.lastdone.app.data.local.dao.HistoryDao
import com.lastdone.app.data.local.dao.ItemDao
import com.lastdone.app.data.local.entity.CategoryEntity
import com.lastdone.app.data.local.entity.HistoryEntity
import com.lastdone.app.data.local.entity.ItemEntity

@Database(
    entities = [ItemEntity::class, HistoryEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LastDoneDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun historyDao(): HistoryDao
    abstract fun categoryDao(): CategoryDao
}
