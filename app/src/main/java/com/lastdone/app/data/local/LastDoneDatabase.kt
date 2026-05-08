package com.lastdone.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lastdone.app.data.local.dao.CategoryDao
import com.lastdone.app.data.local.dao.HistoryDao
import com.lastdone.app.data.local.dao.ItemDao
import com.lastdone.app.data.local.dao.TemplateDao
import com.lastdone.app.data.local.entity.CategoryEntity
import com.lastdone.app.data.local.entity.HistoryEntity
import com.lastdone.app.data.local.entity.ItemEntity
import com.lastdone.app.data.local.entity.TemplateEntity

@Database(
    entities = [
        ItemEntity::class,
        HistoryEntity::class,
        CategoryEntity::class,
        TemplateEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LastDoneDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun historyDao(): HistoryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun templateDao(): TemplateDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `templates` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `name` TEXT NOT NULL,
                `intervalDays` INTEGER NOT NULL,
                `icon` TEXT,
                `categoryId` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_templates_categoryId` ON `templates` (`categoryId`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `items` ADD COLUMN `notifyPreset` TEXT NOT NULL DEFAULT 'MORNING_OF'"
        )
        db.execSQL(
            "ALTER TABLE `items` ADD COLUMN `repeatIntervalMinutes` INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE `items` ADD COLUMN `lastNotifiedAt` TEXT"
        )
    }
}
