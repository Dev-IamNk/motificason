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

@Serializable
data class StreakFreeze(
    val id: String = "",
    @SerialName("habit_id") val habitId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("earned_at") val earnedAt: String,
    @SerialName("used_on_date") val usedOnDate: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class StreakFreezeInsert(
    @SerialName("habit_id") val habitId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("earned_at") val earnedAt: String,
    @SerialName("used_on_date") val usedOnDate: String? = null
)

@Serializable
data class StreakFreezeUpdate(
    @SerialName("used_on_date") val usedOnDate: String
)

@Serializable
data class Achievement(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("habit_id") val habitId: String? = null,
    val type: String,
    @SerialName("unlocked_at") val unlockedAt: String = ""
)

@Serializable
data class AchievementInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("habit_id") val habitId: String? = null,
    val type: String,
    @SerialName("unlocked_at") val unlockedAt: String
)

data class AchievementDefinition(
    val type: String,
    val title: String,
    val description: String,
    val icon: String,
    val isPerHabit: Boolean,
    val target: Int
)

val ACHIEVEMENT_DEFINITIONS = listOf(
    AchievementDefinition(
        type = "first_checkin",
        title = "First Step",
        description = "Complete your very first habit check-in",
        icon = "🚀",
        isPerHabit = false,
        target = 1
    ),
    AchievementDefinition(
        type = "total_checkins_100",
        title = "Centurion",
        description = "Complete 100 habit check-ins in total",
        icon = "💯",
        isPerHabit = false,
        target = 100
    ),
    AchievementDefinition(
        type = "streak_7",
        title = "Week Warrior",
        description = "Reach a 7-day streak on a habit",
        icon = "🔥",
        isPerHabit = true,
        target = 7
    ),
    AchievementDefinition(
        type = "streak_30",
        title = "Monthly Master",
        description = "Reach a 30-day streak on a habit",
        icon = "🌟",
        isPerHabit = true,
        target = 30
    ),
    AchievementDefinition(
        type = "streak_100",
        title = "Century Club",
        description = "Reach a 100-day streak on a habit",
        icon = "⚡",
        isPerHabit = true,
        target = 100
    ),
    AchievementDefinition(
        type = "streak_365",
        title = "Legend of the Year",
        description = "Reach a 365-day streak on a habit",
        icon = "👑",
        isPerHabit = true,
        target = 365
    )
)

data class AchievementUiModel(
    val definition: AchievementDefinition,
    val isUnlocked: Boolean,
    val unlockedAt: String? = null,
    val progressText: String,
    val progressFraction: Float,
    val habitId: String? = null,
    val habitTitle: String? = null
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
