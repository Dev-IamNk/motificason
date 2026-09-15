package com.nk.motificason.ui.lockin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.motificason.data.LockInRepository
import com.nk.motificason.data.SupabaseClientProvider
import com.nk.motificason.data.model.LockIn
import com.nk.motificason.data.model.LockInWithHabits
import com.nk.motificason.data.model.PresetLockIn
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LockInUiState(
    val lockInsWithHabits: List<LockInWithHabits> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val showPickLockInDialog: Boolean = false,
    val customLockInName: String = "",
    val customLockInEmoji: String = "🎯",
    val activeLockInForHabit: LockIn? = null,
    val newHabitTitle: String = ""
)

class LockInViewModel(
    private val repository: LockInRepository = LockInRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockInUiState())
    val uiState: StateFlow<LockInUiState> = _uiState.asStateFlow()

    init {
        loadLockIns()
    }

    private fun getCurrentUserId(): String? {
        val auth = SupabaseClientProvider.client.auth
        return auth.currentUserOrNull()?.id ?: auth.currentSessionOrNull()?.user?.id
    }

    fun loadLockIns() {
        val userId = getCurrentUserId()
        if (userId == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Unable to load Lock-Ins: user not authenticated."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.getLockInsWithHabits(userId)
            result.onSuccess { data ->
                _uiState.value = _uiState.value.copy(
                    lockInsWithHabits = data,
                    isLoading = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to load Lock-Ins."
                )
            }
        }
    }

    fun openPickLockInDialog() {
        _uiState.value = _uiState.value.copy(
            showPickLockInDialog = true,
            customLockInName = "",
            customLockInEmoji = "🎯",
            errorMessage = null
        )
    }

    fun closePickLockInDialog() {
        _uiState.value = _uiState.value.copy(
            showPickLockInDialog = false
        )
    }

    fun onCustomNameChange(name: String) {
        _uiState.value = _uiState.value.copy(customLockInName = name)
    }

    fun onCustomEmojiChange(emoji: String) {
        _uiState.value = _uiState.value.copy(customLockInEmoji = emoji)
    }

    fun pickPreset(preset: PresetLockIn) {
        createLockInInternal(name = preset.name, emoji = preset.emoji)
    }

    fun createCustomLockIn() {
        val name = _uiState.value.customLockInName.trim()
        val emoji = _uiState.value.customLockInEmoji.trim().ifEmpty { "🎯" }

        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a name for the Lock-In.")
            return
        }

        createLockInInternal(name = name, emoji = emoji)
    }

    private fun createLockInInternal(name: String, emoji: String) {
        val userId = getCurrentUserId()
        if (userId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "User not authenticated.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val result = repository.createLockIn(userId = userId, name = name, emoji = emoji)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    showPickLockInDialog = false,
                    customLockInName = "",
                    customLockInEmoji = "🎯"
                )
                loadLockIns()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = error.message ?: "Failed to save Lock-In."
                )
            }
        }
    }

    fun openAddHabitDialog(lockIn: LockIn) {
        _uiState.value = _uiState.value.copy(
            activeLockInForHabit = lockIn,
            newHabitTitle = "",
            errorMessage = null
        )
    }

    fun closeAddHabitDialog() {
        _uiState.value = _uiState.value.copy(
            activeLockInForHabit = null,
            newHabitTitle = ""
        )
    }

    fun onHabitTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(newHabitTitle = title)
    }

    fun createHabit() {
        val lockIn = _uiState.value.activeLockInForHabit ?: return
        val title = _uiState.value.newHabitTitle.trim()

        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a habit title.")
            return
        }

        val userId = getCurrentUserId()
        if (userId == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "User not authenticated.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val result = repository.createHabit(userId = userId, lockInId = lockIn.id, title = title)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    activeLockInForHabit = null,
                    newHabitTitle = ""
                )
                loadLockIns()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = error.message ?: "Failed to add habit."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
