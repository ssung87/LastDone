package com.lastdone.app

import android.app.Application
import androidx.room.Room
import com.google.android.gms.ads.MobileAds
import com.lastdone.app.data.billing.PlusRepository
import com.lastdone.app.data.local.DatabaseSeed
import com.lastdone.app.data.local.LastDoneDatabase
import com.lastdone.app.data.local.MIGRATION_1_2
import com.lastdone.app.data.local.MIGRATION_2_3
import com.lastdone.app.data.settings.SettingsRepository
import com.lastdone.app.notification.NotificationHelper

class LastDoneApplication : Application() {
    val database: LastDoneDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            LastDoneDatabase::class.java,
            "lastdone.db"
        )
            .addCallback(DatabaseSeed.callback)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(applicationContext)
    }

    val plusRepository: PlusRepository by lazy {
        PlusRepository()
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        MobileAds.initialize(this) {}
    }
}
