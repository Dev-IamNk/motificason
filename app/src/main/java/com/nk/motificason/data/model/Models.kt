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
