package com.kegeltrainer.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProfileEntity::class, PlanDayEntity::class, SessionEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun planDao(): PlanDao
    abstract fun sessionDao(): SessionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_epochDay` ON `sessions` (`epochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_endedAt` ON `sessions` (`endedAt`)")
            }
        }
    }
}
