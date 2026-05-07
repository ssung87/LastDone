package com.lastdone.app

import android.app.Application
import androidx.room.Room
import com.lastdone.app.data.local.DatabaseSeed
import com.lastdone.app.data.local.LastDoneDatabase

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
}
