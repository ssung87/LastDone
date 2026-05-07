package com.lastdone.app.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseSeed {
    private val defaultCategories = listOf(
        "집안관리",
        "개인위생",
        "차량",
        "반려동물",
        "건강",
        "기타"
    )

    val callback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            defaultCategories.forEach { name ->
                db.execSQL(
                    "INSERT INTO categories (name, isDefault) VALUES (?, 1)",
                    arrayOf(name)
                )
            }
        }
    }
}
