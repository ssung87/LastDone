package com.lastdone.app

import android.app.Application
import androidx.room.Room
import com.google.android.gms.ads.MobileAds
import com.lastdone.app.data.billing.PlusRepository
import com.lastdone.app.data.local.DatabaseSeed
import com.lastdone.app.data.local.DebugSampleData
import com.lastdone.app.data.local.LastDoneDatabase
import com.lastdone.app.data.settings.SettingsRepository
import com.lastdone.app.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LastDoneApplication : Application() {
    val database: LastDoneDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            LastDoneDatabase::class.java,
            "lastdone.db"
        )
            .addCallback(DatabaseSeed.callback)
            .build()
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(applicationContext)
    }

    val plusRepository: PlusRepository by lazy {
        PlusRepository()
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        MobileAds.initialize(this) {}
        if (BuildConfig.DEBUG) {
            applicationScope.launch {
                DebugSampleData.seedIfEmpty(database)
            }
        }
    }
}
