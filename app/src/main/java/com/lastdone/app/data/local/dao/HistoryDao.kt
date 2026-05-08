package com.lastdone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lastdone.app.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM histories WHERE itemId = :itemId ORDER BY doneDate DESC")
    fun observeByItem(itemId: Long): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM histories WHERE itemId = :itemId ORDER BY doneDate DESC, id DESC LIMIT 1")
    suspend fun findMostRecentByItem(itemId: Long): HistoryEntity?

    @Insert
    suspend fun insert(history: HistoryEntity): Long

    @Update
    suspend fun update(history: HistoryEntity)

    @Delete
    suspend fun delete(history: HistoryEntity)

    @Query("DELETE FROM histories WHERE itemId = :itemId")
    suspend fun deleteAllForItem(itemId: Long)

    @Query("DELETE FROM histories WHERE id = :id")
    suspend fun deleteById(id: Long)
}
