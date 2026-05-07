package com.lastdone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.lastdone.app.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM histories WHERE itemId = :itemId ORDER BY doneDate DESC")
    fun observeByItem(itemId: Long): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(history: HistoryEntity): Long

    @Delete
    suspend fun delete(history: HistoryEntity)

    @Query("DELETE FROM histories WHERE itemId = :itemId")
    suspend fun deleteAllForItem(itemId: Long)
}
