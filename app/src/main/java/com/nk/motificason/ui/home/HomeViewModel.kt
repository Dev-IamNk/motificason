package com.nk.motificason.ui.home

import com.nk.motificason.data.local.SyncManager
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
        viewModelScope.launch {
            SyncManager.syncCompletedEvents.collect {
                loadTodayDashboard()
            }
        }
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
            // 1. Instant load from Room cache (no spinner if data exists)
            val cached = repository.getCachedTodayDashboard(userId, dateIso)
            if (cached.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    lockInsWithCheckIns = cached,
                    isLoading = false,
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            }

            // 2. Background refresh from Supabase
            val result = repository.getTodayDashboard(userId, dateIso)
            result.onSuccess { freshData ->
                _uiState.value = _uiState.value.copy(
                    lockInsWithCheckIns = freshData,
                    isLoading = false,
                    errorMessage = null
                )
            }.onFailure { error ->
                if (_uiState.value.lockInsWithCheckIns.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load today's dashboard."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
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

        viewModelScope.launch {
            // 1. Save immediately to Room with synced = false
            val offlineCheckIn = repository.recordCheckInOffline(
                habitId = habitId,
                userId = userId,
                date = date,
                existingCheckInId = targetHabit.checkInId,
                newCompleted = newCompleted
            )

            // 2. Instant UI update
            val optimisticList = _uiState.value.lockInsWithCheckIns.map { lockInGroup ->
                val updatedHabits = lockInGroup.habits.map { h ->
                    if (h.habit.id == habitId) {
                        h.copy(
                            isCompleted = newCompleted,
                            checkInId = offlineCheckIn.id
                        )
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

            // 3. Attempt background Supabase sync
            val result = repository.toggleCheckIn(
                habitId = habitId,
                userId = userId,
                date = date,
                existingCheckInId = targetHabit.checkInId,
                newCompleted = newCompleted
            )

            result.onSuccess { updatedCheckIn ->
                repository.markCheckInSynced(updatedCheckIn.id)

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

                // Check achievements when check-in is completed
                if (updatedCheckIn.completed) {
                    viewModelScope.launch {
                        repository.evaluateAndUnlockAchievements(userId, habitId)
                    }
                }
            }.onFailure {
                // Offline: check-in is safely saved in Room with synced = false.
                // SyncManager will sync it as soon as connectivity returns.
                // Do NOT roll back UI state.
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
