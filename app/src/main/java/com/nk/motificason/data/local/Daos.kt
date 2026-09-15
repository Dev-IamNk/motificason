package com.nk.motificason.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface LockInDao {
    @Query("SELECT * FROM lock_ins WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getLockInsForUser(userId: String): List<LockInEntity>

    @Upsert
    suspend fun upsertLockIns(lockIns: List<LockInEntity>)

    @Upsert
    suspend fun upsertLockIn(lockIn: LockInEntity)

    @Query("DELETE FROM lock_ins WHERE userId = :userId")
    suspend fun deleteLockInsForUser(userId: String): Int
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getHabitsForUser(userId: String): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE lockInId = :lockInId ORDER BY createdAt ASC")
    suspend fun getHabitsForLockIn(lockInId: String): List<HabitEntity>

    @Upsert
    suspend fun upsertHabits(habits: List<HabitEntity>)

    @Upsert
    suspend fun upsertHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE userId = :userId")
    suspend fun deleteHabitsForUser(userId: String): Int
}

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins WHERE userId = :userId AND date = :date")
    suspend fun getCheckInsForDate(userId: String, date: String): List<CheckInEntity>

    @Query("SELECT * FROM check_ins WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getCheckIn(habitId: String, date: String): CheckInEntity?

    @Query("SELECT * FROM check_ins WHERE habitId = :habitId ORDER BY date ASC")
    suspend fun getCheckInsForHabit(habitId: String): List<CheckInEntity>

    @Upsert
    suspend fun upsertCheckIns(checkIns: List<CheckInEntity>)

    @Upsert
    suspend fun upsertCheckIn(checkIn: CheckInEntity)

    @Query("SELECT * FROM check_ins WHERE synced = 0")
    suspend fun getAllUnsyncedCheckIns(): List<CheckInEntity>

    @Query("SELECT * FROM check_ins WHERE userId = :userId AND synced = 0")
    suspend fun getUnsyncedCheckInsForUser(userId: String): List<CheckInEntity>

    @Query("UPDATE check_ins SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String): Int

    @Query("UPDATE check_ins SET id = :newId, synced = 1 WHERE id = :oldId")
    suspend fun updateIdAndMarkSynced(oldId: String, newId: String): Int

    @Query("DELETE FROM check_ins WHERE id = :id")
    suspend fun deleteCheckIn(id: String): Int
}

@Dao
interface HabitStreakDao {
    @Query("SELECT * FROM habit_streaks WHERE habitId = :habitId LIMIT 1")
    suspend fun getStreak(habitId: String): HabitStreakEntity?

    @Upsert
    suspend fun upsertStreak(streak: HabitStreakEntity)
}
