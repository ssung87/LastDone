package com.lastdone.app.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val DAILY_WORK_NAME = "lastdone_daily_check"

    fun scheduleDaily(context: Context, notifyTime: LocalTime) {
        val initialDelayMillis = computeInitialDelayMillis(notifyTime)
        val request = PeriodicWorkRequestBuilder<DailyCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<DailyCheckWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private fun computeInitialDelayMillis(notifyTime: LocalTime): Long {
        val now = LocalDateTime.now()
        var next = LocalDateTime.of(LocalDate.now(), notifyTime)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis().coerceAtLeast(0L)
    }
}
