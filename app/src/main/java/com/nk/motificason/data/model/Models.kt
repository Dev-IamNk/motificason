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

@Serializable
data class MotivationMessage(
    val id: String = "",
    val category: String,
    val tone: String,
    val message: String,
    val active: Boolean = true
)

@Serializable
data class MotivationMessageInsert(
    val category: String,
    val tone: String,
    val message: String,
    val active: Boolean = true
)

enum class MotivationTone(
    val id: String,
    val displayName: String,
    val emoji: String,
    val description: String,
    val example: String
) {
    FRIENDLY(
        id = "Friendly",
        displayName = "Friendly",
        emoji = "😊",
        description = "Encouraging, warm, and supportive reminders",
        example = "\"Hey! Ready to write some awesome code today? Small steps lead to big apps! 💻✨\""
    ),
    COACH(
        id = "Coach",
        displayName = "Coach",
        emoji = "🏋️",
        description = "Disciplined, structured, and focused on execution",
        example = "\"Time to lock in. Ship that code, clear the backlog, and execute! 💻🔥\""
    ),
    SAVAGE(
        id = "Savage",
        displayName = "Savage",
        emoji = "💀",
        description = "Tough-love, brutally honest, and no-excuses wake-up calls",
        example = "\"Those bugs won't fix themselves while you scroll memes. Open the IDE! 💻💀\""
    ),
    BRAINROT(
        id = "Brainrot",
        displayName = "Brainrot",
        emoji = "🗿",
        description = "Peak Gen-Z slang, sigma grindset, and max aura",
        example = "\"Bro is NOT coding. Stop being NPC and lock in on that sigma grindset fr fr! 💻🗿\""
    );

    companion object {
        fun fromId(id: String): MotivationTone {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: COACH
        }
    }
}

enum class NotificationSlot(
    val id: String,
    val displayName: String,
    val iconEmoji: String,
    val defaultTime: String
) {
    MORNING("morning", "Morning Kickoff", "🌅", "08:00"),
    AFTERNOON("afternoon", "Afternoon Lock-In", "☀️", "13:00"),
    EVENING("evening", "Evening Reflection", "🌙", "20:00");

    companion object {
        fun fromId(id: String): NotificationSlot {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MORNING
        }
    }
}

val DEFAULT_MOTIVATION_MESSAGES = listOf(
    // Coding Grind
    MotivationMessageInsert("Coding Grind", "Friendly", "Hey! Ready to write some awesome code today? Small steps lead to big apps! 💻✨"),
    MotivationMessageInsert("Coding Grind", "Coach", "Time to lock in. Ship that code, clear the backlog, and execute! 💻🔥"),
    MotivationMessageInsert("Coding Grind", "Savage", "Those bugs won't fix themselves while you scroll memes. Open the IDE or stay mediocre! 💻💀"),
    MotivationMessageInsert("Coding Grind", "Brainrot", "Bro is NOT coding. Stop being NPC and lock in on that sigma grindset fr fr no cap! 💻🗿"),

    // Gym Mode
    MotivationMessageInsert("Gym Mode", "Friendly", "You're doing great! Even a light workout today keeps your healthy momentum alive! 🏋️‍♂️🌱"),
    MotivationMessageInsert("Gym Mode", "Coach", "Discipline over motivation. Hit the iron and push past your comfort zone! 🏋️‍♂️💪"),
    MotivationMessageInsert("Gym Mode", "Savage", "The weights don't care about your excuses. Get off the couch or stay weak! 🏋️‍♂️💥"),
    MotivationMessageInsert("Gym Mode", "Brainrot", "Time to mew and hit PRs. If you don't lift today you're literally getting mogged in Ohio! 🏋️‍♂️🗣️"),

    // Music Training
    MotivationMessageInsert("Music Training", "Friendly", "Your instruments miss you! A few minutes of practice makes all the difference! 🎸🎶"),
    MotivationMessageInsert("Music Training", "Coach", "Repetition is the mother of skill. Scales, rhythm, focus. Let's play! 🎸🎯"),
    MotivationMessageInsert("Music Training", "Savage", "Your guitar is collecting dust and your neighbors are judging. Go practice! 🎸🔥"),
    MotivationMessageInsert("Music Training", "Brainrot", "You got zero guitar rizz right now chat. Hop on the fretboard and start shredding! 🎸🔥"),

    // Study Mode
    MotivationMessageInsert("Study Mode", "Friendly", "Time to feed your brain! You've got this, let's learn something new today! 📚💡"),
    MotivationMessageInsert("Study Mode", "Coach", "Focus up. Eliminate distractions and conquer the material today! 📚⚡"),
    MotivationMessageInsert("Study Mode", "Savage", "You promised yourself you'd be smart. Right now you're just procrastinating. Study! 📚🥱"),
    MotivationMessageInsert("Study Mode", "Brainrot", "Bro think he pass without studying 💀. Unc status revoked if you don't lock in right now! 📚🧢"),

    // Dopamine Detox
    MotivationMessageInsert("Dopamine Detox", "Friendly", "Take a deep breath and unplug for a bit. Peace and focus are waiting! 📵🌿"),
    MotivationMessageInsert("Dopamine Detox", "Coach", "Eyes off the feed, eyes on your goals. Stay locked in and disciplined! 📵🛡️"),
    MotivationMessageInsert("Dopamine Detox", "Savage", "Hooked on cheap dopamine like a puppet? Put the phone down and regain control! 📵🗑️"),
    MotivationMessageInsert("Dopamine Detox", "Brainrot", "Bro is scrolling TikTok like a skibidi toilet. Put the phone down and touch grass! 📵🌿"),

    // Sleep Reset
    MotivationMessageInsert("Sleep Reset", "Friendly", "Wind down and get some rest tonight. You worked hard and deserve it! 😴🌙"),
    MotivationMessageInsert("Sleep Reset", "Coach", "Champions recover properly. Hit the sack and prepare for tomorrow's victory! 😴🏆"),
    MotivationMessageInsert("Sleep Reset", "Savage", "Stop doom-scrolling at 2 AM. Go to sleep before your circadian rhythm files for divorce! 😴🪦"),
    MotivationMessageInsert("Sleep Reset", "Brainrot", "Sleeping early is peak sigma male aura +10000. Go eep right now lil bro! 😴🛌"),

    // General (fallback for custom lock-ins or empty lock-ins)
    MotivationMessageInsert("General", "Friendly", "Remember your goals today! You're making progress every single day! ✨"),
    MotivationMessageInsert("General", "Coach", "Standard is standard. Lock in on your commitments today and deliver! 🎯"),
    MotivationMessageInsert("General", "Savage", "Excuses don't build results. Get moving or stay right where you are! 😤"),
    MotivationMessageInsert("General", "Brainrot", "Aura check failed if you skip today. Lock in and stay gigachad! 🗿")
)

@Serializable
data class DailyActivity(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    val title: String,
    val emoji: String,
    val date: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class DailyActivityInsert(
    @SerialName("user_id") val userId: String,
    val title: String,
    val emoji: String,
    val date: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String
)

