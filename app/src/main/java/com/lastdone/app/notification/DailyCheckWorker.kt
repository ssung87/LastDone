package com.lastdone.app.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lastdone.app.LastDoneApplication
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DailyCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as LastDoneApplication
        val items = app.database.itemDao().observeAll().first()
        val globalNotifyTime = app.settingsRepository.notifyTime.first()
        val today = LocalDate.now()
        val now = LocalDateTime.now()

        items.forEach { item ->
            if (!item.notifyEnabled) {
                RepeatAlarmScheduler.cancel(applicationContext, item.id)
                return@forEach
            }

            val preset = NotifyPreset.parse(item.notifyPreset)
            val dueDate = item.lastDoneDate.plusDays(item.intervalDays.toLong())
            val triggerDate = dueDate.minusDays(preset.leadDays().toLong())
            val triggerTime = preset.timeOfDay() ?: globalNotifyTime
            val triggerInstant = LocalDateTime.of(triggerDate, triggerTime)

            if (now.isBefore(triggerInstant)) {
                // 사이클이 아직 트리거 전 → 잔여 알람 정리
                RepeatAlarmScheduler.cancel(applicationContext, item.id)
                return@forEach
            }

            val firstFired = item.lastNotifiedAt?.let { !it.isBefore(triggerInstant) } == true

            if (!firstFired) {
                NotificationHelper.sendDueNotification(applicationContext, item, today)
                app.database.itemDao().update(item.copy(lastNotifiedAt = now))
                if (item.repeatIntervalMinutes > 0) {
                    val nextFireMillis = now
                        .plusMinutes(item.repeatIntervalMinutes.toLong())
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    RepeatAlarmScheduler.schedule(applicationContext, item.id, nextFireMillis)
                }
            } else if (item.repeatIntervalMinutes > 0 &&
                !RepeatAlarmScheduler.hasActiveAlarm(applicationContext, item.id)
            ) {
                // 이미 첫 발화는 했지만 알람이 사라진 상태 (재부팅/앱 재설치 등) → 다음 반복 재예약
                val lastAt = item.lastNotifiedAt ?: now
                val nextFire = lastAt.plusMinutes(item.repeatIntervalMinutes.toLong())
                val target = if (nextFire.isBefore(now)) {
                    now.plusMinutes(item.repeatIntervalMinutes.toLong())
                } else nextFire
                val nextFireMillis = target
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                RepeatAlarmScheduler.schedule(applicationContext, item.id, nextFireMillis)
            }
        }
        return Result.success()
    }
}
