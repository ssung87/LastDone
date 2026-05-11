package com.lastdone.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.lastdone.app.LastDoneApplication
import com.lastdone.app.MainActivity
import com.lastdone.app.R
import com.lastdone.app.data.local.entity.ItemEntity
import com.lastdone.app.domain.ItemStatus
import com.lastdone.app.domain.calculateItemStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class LastDoneWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val app = context.applicationContext as LastDoneApplication
                val items = app.database.itemDao().observeAll().first()
                val threshold = app.settingsRepository.impendingThreshold.first()
                val today = LocalDate.now()

                val ranked = items
                    .map { it to calculateItemStatus(it.lastDoneDate, it.intervalDays, today, threshold) }
                    .sortedWith(widgetComparator)
                    .take(MAX_ROWS)

                appWidgetIds.forEach { id ->
                    val views = buildViews(context, ranked)
                    appWidgetManager.updateAppWidget(id, views)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun buildViews(
        context: Context,
        ranked: List<Pair<ItemEntity, ItemStatus>>
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_lastdone)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openPending)

        if (ranked.isEmpty()) {
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            ROW_IDS.forEach { row -> views.setViewVisibility(row.container, View.GONE) }
            return views
        }

        views.setViewVisibility(R.id.widget_empty, View.GONE)
        ROW_IDS.forEachIndexed { index, row ->
            val pair = ranked.getOrNull(index)
            if (pair == null) {
                views.setViewVisibility(row.container, View.GONE)
                return@forEachIndexed
            }
            val (item, status) = pair
            views.setViewVisibility(row.container, View.VISIBLE)
            views.setTextViewText(row.icon, item.icon ?: "")
            views.setTextViewText(row.name, item.name)
            views.setTextViewText(row.status, statusLabel(status))
            views.setTextColor(row.status, statusColor(status.kind))
        }
        return views
    }

    private fun statusLabel(status: ItemStatus): String = when (status.kind) {
        ItemStatus.Kind.OVERDUE -> "${-status.daysRemaining}일 지남"
        ItemStatus.Kind.DUE_TODAY -> "오늘"
        ItemStatus.Kind.IMPENDING -> "D-${status.daysRemaining}"
        ItemStatus.Kind.RELAXED -> "D-${status.daysRemaining}"
    }

    private fun statusColor(kind: ItemStatus.Kind): Int = when (kind) {
        ItemStatus.Kind.OVERDUE -> 0xFFD32F2F.toInt()
        ItemStatus.Kind.DUE_TODAY -> 0xFFE65100.toInt()
        ItemStatus.Kind.IMPENDING -> 0xFFF9A825.toInt()
        ItemStatus.Kind.RELAXED -> 0xFF666666.toInt()
    }

    private data class RowIds(val container: Int, val icon: Int, val name: Int, val status: Int)

    companion object {
        private const val MAX_ROWS = 4

        private val ROW_IDS = listOf(
            RowIds(R.id.widget_row_0, R.id.widget_row_0_icon, R.id.widget_row_0_name, R.id.widget_row_0_status),
            RowIds(R.id.widget_row_1, R.id.widget_row_1_icon, R.id.widget_row_1_name, R.id.widget_row_1_status),
            RowIds(R.id.widget_row_2, R.id.widget_row_2_icon, R.id.widget_row_2_name, R.id.widget_row_2_status),
            RowIds(R.id.widget_row_3, R.id.widget_row_3_icon, R.id.widget_row_3_name, R.id.widget_row_3_status)
        )

        private val widgetComparator = Comparator<Pair<ItemEntity, ItemStatus>> { a, b ->
            val byKind = a.second.kind.ordinal.compareTo(b.second.kind.ordinal)
            if (byKind != 0) return@Comparator byKind
            a.second.daysRemaining.compareTo(b.second.daysRemaining)
        }

        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, LastDoneWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            val intent = Intent(context, LastDoneWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
