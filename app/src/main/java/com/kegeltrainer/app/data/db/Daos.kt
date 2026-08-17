package com.kegeltrainer.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun get(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProfileEntity)
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM plan_days ORDER BY dayIndex ASC")
    fun observeAll(): Flow<List<PlanDayEntity>>

    @Query("SELECT * FROM plan_days ORDER BY dayIndex ASC")
    suspend fun getAll(): List<PlanDayEntity>

    @Query("SELECT * FROM plan_days WHERE dayIndex = :index")
    suspend fun get(index: Int): PlanDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(days: List<PlanDayEntity>)

    @Query("DELETE FROM plan_days")
    suspend fun clear()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY endedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY endedAt DESC")
    suspend fun getAll(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE epochDay = :epochDay ORDER BY endedAt DESC")
    fun observeOn(epochDay: Long): Flow<List<SessionEntity>>

    @Insert
    suspend fun insert(entity: SessionEntity): Long
}
