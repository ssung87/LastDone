package com.lastdone.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lastdone.app.LastDoneApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RepeatAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        if (itemId < 0) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val app = context.applicationContext as LastDoneApplication
                val item = app.database.itemDao().getById(itemId) ?: return@launch
                if (!item.notifyEnabled || item.repeatIntervalMinutes <= 0) return@launch

                // 권장일 사이클이 종료(사용자가 "오늘 했어요")됐는지 확인:
                // 사이클 갱신 후엔 lastDoneDate가 바뀌어 첫 알림 조건부터 다시 평가되어야 한다.
                val dueDate = item.lastDoneDate.plusDays(item.intervalDays.toLong())
                val today = LocalDate.now()
                if (today.isBefore(dueDate.minusDays(NotifyPreset.parse(item.notifyPreset).leadDays().toLong()))) {
                    // 트리거 시점이 아직 도래 안함 (사이클 리셋된 케이스) — 알람만 정리하고 종료
                    return@launch
                }

                val now = LocalDateTime.now()
                val settings = app.settingsRepository.settings.first()
                val inQuietHours = settings.quietHoursEnabled &&
                    QuietHours.isWithin(now.toLocalTime(), settings.quietHoursStart, settings.quietHoursEnd)

                if (!inQuietHours) {
                    NotificationHelper.sendDueNotification(context, item, today)
                    app.database.itemDao().update(item.copy(lastNotifiedAt = now))
                }

                val nextFireMillis = now
                    .plusMinutes(item.repeatIntervalMinutes.toLong())
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                RepeatAlarmScheduler.schedule(context, itemId, nextFireMillis)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.lastdone.app.action.REPEAT_ALARM_FIRE"
        const val EXTRA_ITEM_ID = "itemId"
    }
}
