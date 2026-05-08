package com.lastdone.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lastdone.app.data.settings.ThemeMode
import com.lastdone.app.notification.NotificationHelper
import com.lastdone.app.notification.NotificationScheduler
import com.lastdone.app.ui.LastDoneNavHost
import com.lastdone.app.ui.theme.LastDoneTheme

class MainActivity : ComponentActivity() {

    private var pendingItemId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeNotificationItemId(intent)
        val app = applicationContext as LastDoneApplication
        setContent {
            val themeMode by app.settingsRepository.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM)

            LaunchedEffect(Unit) {
                NotificationScheduler.schedule(applicationContext)
            }

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LastDoneTheme(darkTheme = darkTheme) {
                LastDoneNavHost(
                    pendingItemId = pendingItemId,
                    onPendingHandled = { pendingItemId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeNotificationItemId(intent)
    }

    private fun consumeNotificationItemId(intent: Intent?) {
        if (intent == null) return
        val id = intent.getLongExtra(NotificationHelper.EXTRA_OPEN_ITEM_ID, -1L)
        if (id > 0) {
            pendingItemId = id
            intent.removeExtra(NotificationHelper.EXTRA_OPEN_ITEM_ID)
        }
    }
}
