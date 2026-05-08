package com.lastdone.app.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val PERIODIC_WORK_NAME = "lastdone_periodic_check"

    fun schedule(context: Context) {
        // 항목별 프리셋(당일 09/12/18 등) 및 반복 알림(10/30/60분)을 지원하기 위해
        // PeriodicWork 최소 주기인 15분 간격으로 실행한다.
        // 10분 반복 옵션은 WorkManager 한계로 실제로는 ~15분 마다 발화될 수 있다.
        val request = PeriodicWorkRequestBuilder<DailyCheckWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<DailyCheckWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
