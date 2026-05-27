package com.lastdone.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lastdone.app.MainActivity
import com.lastdone.app.R
import com.lastdone.app.data.local.entity.ItemEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object NotificationHelper {
    const val CHANNEL_ID = "due_reminders"
    const val EXTRA_OPEN_ITEM_ID = "open_item_id"
    private const val CHANNEL_NAME = "권장일 알림"
    private const val CHANNEL_DESC = "권장일이 도래한 항목을 알려드려요."

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESC
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    fun sendDueNotification(context: Context, item: ItemEntity, today: LocalDate) {
        if (!hasPostPermission(context)) return

        val daysElapsed = ChronoUnit.DAYS.between(item.lastDoneDate, today).toInt()
        val daysOver = daysElapsed - item.intervalDays
        val title = "${item.name} 할 때가 됐어요"
        val body = if (daysOver > 0) {
            "권장일을 ${daysOver}일 지났어요 (마지막으로 한 지 ${daysElapsed}일)"
        } else {
            "마지막으로 한 지 ${daysElapsed}일 지났어요"
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_ITEM_ID, item.id)
        }
        val openPending = PendingIntent.getActivity(
            context,
            item.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markDoneIntent = Intent(context, MarkDoneReceiver::class.java).apply {
            action = MarkDoneReceiver.ACTION_MARK_DONE
            putExtra(MarkDoneReceiver.EXTRA_ITEM_ID, item.id)
        }
        val markDonePending = PendingIntent.getBroadcast(
            context,
            item.id.toInt(),
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, SnoozeDialogActivity::class.java).apply {
            action = SnoozeDialogActivity.ACTION_SHOW
            putExtra(SnoozeDialogActivity.EXTRA_ITEM_ID, item.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY
        }
        val snoozePending = PendingIntent.getActivity(
            context,
            SNOOZE_REQUEST_OFFSET + item.id.toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(openPending)
            .addAction(0, "오늘 했어요", markDonePending)
            .addAction(0, "나중에 다시", snoozePending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(item.id.toInt(), notification)
    }

    private const val SNOOZE_REQUEST_OFFSET = 1_000_000

    fun hasPostPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
