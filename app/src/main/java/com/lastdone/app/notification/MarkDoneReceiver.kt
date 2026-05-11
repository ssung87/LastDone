package com.lastdone.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.lastdone.app.LastDoneApplication
import com.lastdone.app.data.local.entity.HistoryEntity
import com.lastdone.app.widget.LastDoneWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

class MarkDoneReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        if (itemId < 0) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val app = context.applicationContext as LastDoneApplication
                val item = app.database.itemDao().getById(itemId) ?: return@launch
                val today = LocalDate.now()
                app.database.itemDao().update(
                    item.copy(lastDoneDate = today, lastNotifiedAt = null)
                )
                app.database.historyDao().insert(
                    HistoryEntity(itemId = itemId, doneDate = today, memo = null)
                )
                NotificationManagerCompat.from(context).cancel(itemId.toInt())
                RepeatAlarmScheduler.cancel(context, itemId)
                LastDoneWidgetProvider.requestUpdate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.lastdone.app.action.MARK_DONE"
        const val EXTRA_ITEM_ID = "itemId"
    }
}
