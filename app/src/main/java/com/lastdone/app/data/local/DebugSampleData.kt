package com.lastdone.app.data.local

import com.lastdone.app.data.local.entity.HistoryEntity
import com.lastdone.app.data.local.entity.ItemEntity
import java.time.LocalDate
import java.time.LocalDateTime

object DebugSampleData {

    private data class SampleItem(
        val name: String,
        val categoryId: Long,
        val daysAgo: Int,
        val intervalDays: Int,
        val icon: String?,
        val memo: String?,
        val previousGapsAgo: List<Int>
    )

    private val samples = listOf(
        SampleItem("강아지 목욕", 4, 30, 14, "🐕", null, listOf(45, 60)),
        SampleItem("에어컨 필터 청소", 1, 35, 30, "❄️", null, listOf(70)),
        SampleItem("침구 세탁", 1, 12, 14, "🛏️", "이불, 베개커버 포함", listOf(26, 39)),
        SampleItem("자동차 세차", 3, 20, 30, "🚗", null, emptyList()),
        SampleItem("칫솔 교체", 2, 10, 90, "🪥", null, listOf(105)),
        SampleItem("건강검진", 5, 30, 365, "🏥", null, emptyList())
    )

    suspend fun seedIfEmpty(database: LastDoneDatabase) {
        val itemDao = database.itemDao()
        if (itemDao.count() > 0) return

        val historyDao = database.historyDao()
        val today = LocalDate.now()
        val now = LocalDateTime.now()

        samples.forEach { sample ->
            val lastDone = today.minusDays(sample.daysAgo.toLong())
            val newId = itemDao.insert(
                ItemEntity(
                    name = sample.name,
                    categoryId = sample.categoryId,
                    lastDoneDate = lastDone,
                    intervalDays = sample.intervalDays,
                    icon = sample.icon,
                    memo = sample.memo,
                    notifyEnabled = false,
                    notifyTime = null,
                    createdAt = now
                )
            )
            historyDao.insert(
                HistoryEntity(
                    itemId = newId,
                    doneDate = lastDone,
                    memo = sample.memo
                )
            )
            sample.previousGapsAgo.forEach { gap ->
                historyDao.insert(
                    HistoryEntity(
                        itemId = newId,
                        doneDate = today.minusDays(gap.toLong()),
                        memo = null
                    )
                )
            }
        }
    }
}
