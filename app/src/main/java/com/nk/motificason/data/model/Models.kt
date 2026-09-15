package com.nk.motificason.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LockIn(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    val name: String,
    val emoji: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class LockInInsert(
    @SerialName("user_id") val userId: String,
    val name: String,
    val emoji: String
)

@Serializable
data class Habit(
    val id: String = "",
    @SerialName("lock_in_id") val lockInId: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class HabitInsert(
    @SerialName("lock_in_id") val lockInId: String,
    @SerialName("user_id") val userId: String,
    val title: String
)

data class LockInWithHabits(
    val lockIn: LockIn,
    val habits: List<Habit> = emptyList()
)

@Serializable
data class CheckIn(
    val id: String = "",
    @SerialName("habit_id") val habitId: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    val completed: Boolean,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CheckInInsert(
    @SerialName("habit_id") val habitId: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    val completed: Boolean
)

@Serializable
data class CheckInUpdate(
    val completed: Boolean
)

data class HabitWithCheckIn(
    val habit: Habit,
    val isCompleted: Boolean = false,
    val checkInId: String? = null
)

data class LockInWithHabitCheckIns(
    val lockIn: LockIn,
    val habits: List<HabitWithCheckIn> = emptyList()
) {
    val completedCount: Int get() = habits.count { it.isCompleted }
    val totalCount: Int get() = habits.size
}

data class PresetLockIn(
    val name: String,
    val emoji: String
)

val PRESET_LOCK_INS = listOf(
    PresetLockIn(name = "Coding Grind", emoji = "💻"),
    PresetLockIn(name = "Gym Mode", emoji = "🏋️"),
    PresetLockIn(name = "Music Training", emoji = "🎸"),
    PresetLockIn(name = "Study Mode", emoji = "📚"),
    PresetLockIn(name = "Dopamine Detox", emoji = "📵"),
    PresetLockIn(name = "Sleep Reset", emoji = "😴")
)
