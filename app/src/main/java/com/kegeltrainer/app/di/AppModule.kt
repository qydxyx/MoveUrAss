package com.kegeltrainer.app.di

import android.content.Context
import androidx.room.Room
import com.kegeltrainer.app.data.db.AppDatabase
import com.kegeltrainer.app.data.db.PlanDao
import com.kegeltrainer.app.data.db.ProfileDao
import com.kegeltrainer.app.data.db.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "kegel.db").build()

    @Provides
    fun profileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    fun planDao(db: AppDatabase): PlanDao = db.planDao()

    @Provides
    fun sessionDao(db: AppDatabase): SessionDao = db.sessionDao()
}
