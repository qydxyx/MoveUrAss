package com.kegeltrainer.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ProfileEntity::class, PlanDayEntity::class, SessionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun planDao(): PlanDao
    abstract fun sessionDao(): SessionDao
}
