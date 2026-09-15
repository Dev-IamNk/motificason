package com.nk.motificason.ui.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.motificason.data.DailyActivityRepository
import com.nk.motificason.data.SupabaseClientProvider
import com.nk.motificason.data.model.DailyActivity
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DailyRoutineUiState(
    val activities: List<DailyActivity> = emptyList(),
    val todayDate: String = "",
    val displayDate: String = "",
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val errorMessage: String? = null,
    val titleInput: String = "",
    val emojiInput: String = "💻",
    val startTimeInput: String = "09:00",
    val endTimeInput: String = "10:30",
    val formError: String? = null
) {
    val totalScheduledMinutes: Int get() = activities.sumOf {
        RoutineClockHelper.calculateDurationMinutes(it.startTime, it.endTime)
    }
    val formattedTotalTime: String get() = RoutineClockHelper.formatDuration(totalScheduledMinutes)
}

class DailyRoutineViewModel(
    private val repository: DailyActivityRepository = DailyActivityRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyRoutineUiState())
    val uiState: StateFlow<DailyRoutineUiState> = _uiState.asStateFlow()

    init {
        loadTodayActivities()
    }

    private fun getCurrentUserId(): String? {
        val auth = SupabaseClientProvider.client.auth
        return auth.currentUserOrNull()?.id ?: auth.currentSessionOrNull()?.user?.id
    }

    fun loadTodayActivities() {
        val userId = getCurrentUserId()
        val now = LocalDate.now()
        val dateIso = now.toString()
        val dateDisplay = now.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

        _uiState.value = _uiState.value.copy(
            todayDate = dateIso,
            displayDate = dateDisplay
        )

        if (userId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "User not authenticated.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.getTodayActivities(userId, dateIso)
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(
                        activities = list,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load routine."
                    )
                }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(titleInput = title, formError = null)
    }

    fun onEmojiChange(emoji: String) {
        _uiState.value = _uiState.value.copy(emojiInput = emoji)
    }

    fun onStartTimeChange(startTime: String) {
        _uiState.value = _uiState.value.copy(startTimeInput = startTime, formError = null)
    }

    fun onEndTimeChange(endTime: String) {
        _uiState.value = _uiState.value.copy(endTimeInput = endTime, formError = null)
    }

    fun addActivity() {
        val currentState = _uiState.value
        val title = currentState.titleInput.trim()
        val emoji = currentState.emojiInput.trim().ifBlank { "🎯" }
        val startTime = currentState.startTimeInput.trim()
        val endTime = currentState.endTimeInput.trim()
        val userId = getCurrentUserId()

        if (userId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "User not authenticated.")
            return
        }

        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(formError = "Please enter an activity title.")
            return
        }

        val timeRegex = Regex("^([01]?\\d|2[0-3]):[0-5]\\d$")
        if (!timeRegex.matches(startTime)) {
            _uiState.value = _uiState.value.copy(formError = "Start time must be in HH:mm format (e.g. 09:00).")
            return
        }
        if (!timeRegex.matches(endTime)) {
            _uiState.value = _uiState.value.copy(formError = "End time must be in HH:mm format (e.g. 10:30).")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAdding = true, formError = null)
            repository.addActivity(
                userId = userId,
                title = title,
                emoji = emoji,
                date = currentState.todayDate,
                startTime = startTime,
                endTime = endTime
            ).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isAdding = false,
                    titleInput = "",
                    formError = null
                )
                loadTodayActivities()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isAdding = false,
                    formError = error.message ?: "Failed to add activity."
                )
            }
        }
    }

    fun deleteActivity(activityId: String) {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            repository.deleteActivity(activityId, userId)
                .onSuccess {
                    loadTodayActivities()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Failed to delete activity."
                    )
                }
        }
    }
}
