package com.nk.motificason.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.motificason.data.LockInRepository
import com.nk.motificason.data.SupabaseClientProvider
import com.nk.motificason.data.model.HabitWithCheckIn
import com.nk.motificason.data.model.LockInWithHabitCheckIns
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val lockInsWithCheckIns: List<LockInWithHabitCheckIns> = emptyList(),
    val todayDate: String = "",
    val displayDate: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val totalHabits: Int get() = lockInsWithCheckIns.sumOf { it.totalCount }
    val completedHabits: Int get() = lockInsWithCheckIns.sumOf { it.completedCount }
    val progressFraction: Float get() = if (totalHabits > 0) completedHabits.toFloat() / totalHabits else 0f
}

class HomeViewModel(
    private val repository: LockInRepository = LockInRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadTodayDashboard()
    }

    private fun getCurrentUserId(): String? {
        val auth = SupabaseClientProvider.client.auth
        return auth.currentUserOrNull()?.id ?: auth.currentSessionOrNull()?.user?.id
    }

    fun loadTodayDashboard() {
        val userId = getCurrentUserId()
        val now = LocalDate.now()
        val dateIso = now.toString()
        val dateDisplay = now.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

        _uiState.value = _uiState.value.copy(
            todayDate = dateIso,
            displayDate = dateDisplay
        )

        if (userId == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "User not authenticated."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.getTodayDashboard(userId, dateIso)
            result.onSuccess { data ->
                _uiState.value = _uiState.value.copy(
                    lockInsWithCheckIns = data,
                    isLoading = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to load today's dashboard."
                )
            }
        }
    }

    fun toggleHabit(habitId: String) {
        val currentList = _uiState.value.lockInsWithCheckIns
        var targetHabit: HabitWithCheckIn? = null

        for (lockInWithHabits in currentList) {
            for (habitWithCheckIn in lockInWithHabits.habits) {
                if (habitWithCheckIn.habit.id == habitId) {
                    targetHabit = habitWithCheckIn
                    break
                }
            }
            if (targetHabit != null) break
        }

        if (targetHabit == null) return

        val newCompleted = !targetHabit.isCompleted
        val userId = getCurrentUserId() ?: return
        val date = _uiState.value.todayDate

        // Optimistic UI update
        val optimisticList = currentList.map { lockInGroup ->
            val updatedHabits = lockInGroup.habits.map { h ->
                if (h.habit.id == habitId) {
                    h.copy(isCompleted = newCompleted)
                } else {
                    h
                }
            }
            lockInGroup.copy(habits = updatedHabits)
        }

        _uiState.value = _uiState.value.copy(
            lockInsWithCheckIns = optimisticList,
            errorMessage = null
        )

        // Asynchronous Supabase update
        viewModelScope.launch {
            val result = repository.toggleCheckIn(
                habitId = habitId,
                userId = userId,
                date = date,
                existingCheckInId = targetHabit.checkInId,
                newCompleted = newCompleted
            )

            result.onSuccess { updatedCheckIn ->
                // Update checkInId in case it was a new record
                val finalList = _uiState.value.lockInsWithCheckIns.map { lockInGroup ->
                    val finalHabits = lockInGroup.habits.map { h ->
                        if (h.habit.id == habitId) {
                            h.copy(
                                isCompleted = updatedCheckIn.completed,
                                checkInId = updatedCheckIn.id
                            )
                        } else {
                            h
                        }
                    }
                    lockInGroup.copy(habits = finalHabits)
                }
                _uiState.value = _uiState.value.copy(lockInsWithCheckIns = finalList)

                // Check achievements whenever a check-in is completed
                if (updatedCheckIn.completed) {
                    viewModelScope.launch {
                        repository.evaluateAndUnlockAchievements(userId, habitId)
                    }
                }
            }.onFailure { error ->
                // Rollback optimistic update on failure
                val revertedList = _uiState.value.lockInsWithCheckIns.map { lockInGroup ->
                    val revertedHabits = lockInGroup.habits.map { h ->
                        if (h.habit.id == habitId) {
                            h.copy(isCompleted = targetHabit.isCompleted)
                        } else {
                            h
                        }
                    }
                    lockInGroup.copy(habits = revertedHabits)
                }
                _uiState.value = _uiState.value.copy(
                    lockInsWithCheckIns = revertedList,
                    errorMessage = "Failed to update check-in: ${error.message ?: "Network error"}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
