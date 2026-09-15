package com.nk.motificason.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.motificason.data.LockInRepository
import com.nk.motificason.data.SupabaseClientProvider
import com.nk.motificason.data.model.ACHIEVEMENT_DEFINITIONS
import com.nk.motificason.data.model.AchievementUiModel
import com.nk.motificason.data.model.CheckIn
import com.nk.motificason.data.model.Habit
import com.nk.motificason.data.model.LockIn
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HabitWithAchievements(
    val habit: Habit,
    val lockIn: LockIn?,
    val achievements: List<AchievementUiModel>
)

data class AchievementsUiState(
    val accountAchievements: List<AchievementUiModel> = emptyList(),
    val habitAchievements: List<HabitWithAchievements> = emptyList(),
    val totalUnlockedCount: Int = 0,
    val totalAvailableCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AchievementsViewModel(
    private val repository: LockInRepository = LockInRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        loadAchievements()
    }

    private fun getCurrentUserId(): String? {
        val auth = SupabaseClientProvider.client.auth
        return auth.currentUserOrNull()?.id ?: auth.currentSessionOrNull()?.user?.id
    }

    fun loadAchievements() {
        val userId = getCurrentUserId()
        if (userId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "User not authenticated.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                // First evaluate to ensure any newly reached milestone is unlocked
                repository.evaluateAndUnlockAchievements(userId)

                val client = SupabaseClientProvider.client

                val achievements = repository.getUserAchievements(userId).getOrDefault(emptyList())

                val completedCheckIns = client.from("check_ins")
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("completed", true)
                        }
                    }
                    .decodeList<CheckIn>()
                val totalCheckIns = completedCheckIns.size

                val lockIns = client.from("lock_ins")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<LockIn>()
                val lockInMap = lockIns.associateBy { it.id }

                val habits = client.from("habits")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<Habit>()

                val today = LocalDate.now()

                // 1. Account-wide achievements
                val accountDefs = ACHIEVEMENT_DEFINITIONS.filter { !it.isPerHabit }
                val accountList = accountDefs.map { def ->
                    val record = achievements.find { it.type == def.type && it.habitId == null }
                    val isUnlocked = record != null || totalCheckIns >= def.target
                    val remaining = maxOf(0, def.target - totalCheckIns)
                    val progressText = if (isUnlocked) {
                        "Unlocked on ${record?.unlockedAt?.substringBefore("T") ?: "today"} ✅"
                    } else {
                        "$remaining to go (${totalCheckIns}/${def.target} completed)"
                    }
                    val fraction = (totalCheckIns.toFloat() / def.target).coerceIn(0f, 1f)
                    AchievementUiModel(
                        definition = def,
                        isUnlocked = isUnlocked,
                        unlockedAt = record?.unlockedAt?.substringBefore("T"),
                        progressText = progressText,
                        progressFraction = fraction
                    )
                }

                // 2. Per-habit streak achievements
                val streakDefs = ACHIEVEMENT_DEFINITIONS.filter { it.isPerHabit }
                val habitList = habits.map { habit ->
                    val streakResult = repository.getHabitStreak(habit, userId, today).getOrNull()
                    val currentStreak = streakResult?.currentStreak ?: 0

                    val items = streakDefs.map { def ->
                        val record = achievements.find { it.habitId == habit.id && it.type == def.type }
                        val isUnlocked = record != null || currentStreak >= def.target
                        val remaining = maxOf(0, def.target - currentStreak)
                        val progressText = if (isUnlocked) {
                            "Unlocked on ${record?.unlockedAt?.substringBefore("T") ?: "today"} ✅"
                        } else {
                            "$remaining days to go (Streak: $currentStreak)"
                        }
                        val fraction = (currentStreak.toFloat() / def.target).coerceIn(0f, 1f)

                        AchievementUiModel(
                            definition = def,
                            isUnlocked = isUnlocked,
                            unlockedAt = record?.unlockedAt?.substringBefore("T"),
                            progressText = progressText,
                            progressFraction = fraction,
                            habitId = habit.id,
                            habitTitle = habit.title
                        )
                    }

                    HabitWithAchievements(
                        habit = habit,
                        lockIn = lockInMap[habit.lockInId],
                        achievements = items
                    )
                }

                val totalUnlocked = accountList.count { it.isUnlocked } +
                        habitList.sumOf { h -> h.achievements.count { it.isUnlocked } }
                val totalAvailable = accountList.size + (habitList.size * streakDefs.size)

                _uiState.value = _uiState.value.copy(
                    accountAchievements = accountList,
                    habitAchievements = habitList,
                    totalUnlockedCount = totalUnlocked,
                    totalAvailableCount = totalAvailable,
                    isLoading = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to load achievements."
                )
            }
        }
    }
}
