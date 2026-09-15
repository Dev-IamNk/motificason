package com.nk.motificason.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nk.motificason.data.model.CheckIn
import com.nk.motificason.data.model.Habit
import com.nk.motificason.data.model.LockIn

@Entity(
    tableName = "lock_ins",
    indices = [Index(value = ["userId"])]
)
data class LockInEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val emoji: String,
    val createdAt: String? = null
)

fun LockIn.toEntity(): LockInEntity = LockInEntity(
    id = id,
    userId = userId,
    name = name,
    emoji = emoji,
    createdAt = createdAt
)

fun LockInEntity.toDomain(): LockIn = LockIn(
    id = id,
    userId = userId,
    name = name,
    emoji = emoji,
    createdAt = createdAt
)

@Entity(
    tableName = "habits",
    indices = [Index(value = ["userId"]), Index(value = ["lockInId"])]
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val lockInId: String,
    val userId: String,
    val title: String,
    val createdAt: String? = null
)

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    lockInId = lockInId,
    userId = userId,
    title = title,
    createdAt = createdAt
)

fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    lockInId = lockInId,
    userId = userId,
    title = title,
    createdAt = createdAt
)

@Entity(
    tableName = "check_ins",
    indices = [
        Index(value = ["userId", "date"]),
        Index(value = ["habitId", "date"]),
        Index(value = ["synced"])
    ]
)
data class CheckInEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val userId: String,
    val date: String,
    val completed: Boolean,
    val createdAt: String? = null,
    val synced: Boolean = true
)

fun CheckIn.toEntity(synced: Boolean = true): CheckInEntity = CheckInEntity(
    id = id,
    habitId = habitId,
    userId = userId,
    date = date,
    completed = completed,
    createdAt = createdAt,
    synced = synced
)

fun CheckInEntity.toDomain(): CheckIn = CheckIn(
    id = id,
    habitId = habitId,
    userId = userId,
    date = date,
    completed = completed,
    createdAt = createdAt
)

@Entity(
    tableName = "habit_streaks",
    indices = [Index(value = ["userId"])]
)
data class HabitStreakEntity(
    @PrimaryKey val habitId: String,
    val userId: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletedDays: Int,
    val unusedFreezeCount: Int,
    val updatedAt: String
)
