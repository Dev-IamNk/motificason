package com.nk.motificason.ui.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.motificason.data.LockInRepository
import com.nk.motificason.data.SupabaseClientProvider
import com.nk.motificason.data.engine.DayHistory
import com.nk.motificason.data.model.Habit
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class StreakUiState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompletedDays: Int = 0,
    val unusedFreezeCount: Int = 0,
    val dayHistory: List<DayHistory> = emptyList(),
    val currentStreakDays: List<DayHistory> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class StreakViewModel(
    private val repository: LockInRepository = LockInRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StreakUiState())
    val uiState: StateFlow<StreakUiState> = _uiState.asStateFlow()

    private fun getCurrentUserId(): String? {
        val auth = SupabaseClientProvider.client.auth
        return auth.currentUserOrNull()?.id ?: auth.currentSessionOrNull()?.user?.id
    }

    fun loadStreak(habit: Habit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "User not authenticated.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.getHabitStreak(habit, userId, LocalDate.now())
            result.onSuccess { data ->
                _uiState.value = _uiState.value.copy(
                    currentStreak = data.currentStreak,
                    longestStreak = data.longestStreak,
                    totalCompletedDays = data.totalCompletedDays,
                    unusedFreezeCount = data.unusedFreezeCount,
                    dayHistory = data.dayHistory,
                    currentStreakDays = data.currentStreakDays,
                    isLoading = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to calculate streak."
                )
            }
        }
    }
}
