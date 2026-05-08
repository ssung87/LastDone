package com.lastdone.app.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lastdone.app.LastDoneApplication
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DailyCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as LastDoneApplication
        val items = app.database.itemDao().observeAll().first()
        val today = LocalDate.now()

        items.forEach { item ->
            if (!item.notifyEnabled) return@forEach
            val elapsed = ChronoUnit.DAYS.between(item.lastDoneDate, today).toInt()
            val remaining = item.intervalDays - elapsed
            if (remaining <= 0) {
                NotificationHelper.sendDueNotification(applicationContext, item, today)
            }
        }
        return Result.success()
    }
}
