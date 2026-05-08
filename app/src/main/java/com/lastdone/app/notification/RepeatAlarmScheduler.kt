package com.lastdone.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object RepeatAlarmScheduler {

    fun schedule(context: Context, itemId: Long, fireAtMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = pendingIntent(
            context,
            itemId,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                fireAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                fireAtMillis,
                pendingIntent
            )
        }
    }

    fun cancel(context: Context, itemId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(
            context,
            itemId,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pi)
        pi.cancel()
    }

    fun hasActiveAlarm(context: Context, itemId: Long): Boolean {
        return pendingIntent(
            context,
            itemId,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null
    }

    private fun pendingIntent(context: Context, itemId: Long, flags: Int): PendingIntent? {
        val intent = Intent(context, RepeatAlarmReceiver::class.java).apply {
            action = RepeatAlarmReceiver.ACTION_FIRE
            putExtra(RepeatAlarmReceiver.EXTRA_ITEM_ID, itemId)
        }
        return PendingIntent.getBroadcast(
            context,
            itemId.toInt(),
            intent,
            flags
        )
    }
}
