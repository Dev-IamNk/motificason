package com.nk.motificason.ui.notification

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.motificason.data.NotificationRepository
import com.nk.motificason.data.SupabaseClientProvider
import com.nk.motificason.data.model.LockIn
import com.nk.motificason.data.model.MotivationTone
import com.nk.motificason.data.model.NotificationSlot
import com.nk.motificason.notification.NotificationPreferences
import com.nk.motificason.notification.NotificationScheduler
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SlotUiState(
    val slot: NotificationSlot,
    val isEnabled: Boolean,
    val time: String
)

data class NotificationSettingsUiState(
    val selectedTone: MotivationTone = MotivationTone.COACH,
    val slots: List<SlotUiState> = emptyList(),
    val canScheduleExactAlarms: Boolean = true,
    val isSeeding: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

class NotificationSettingsViewModel(
    private val repository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        seedMessagesInBackground()
    }

    private fun seedMessagesInBackground() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSeeding = true)
            repository.seedMessagesIfEmpty()
            _uiState.value = _uiState.value.copy(isSeeding = false)
        }
    }

    fun loadSettings(context: Context) {
        val prefs = NotificationPreferences(context)
        val currentTone = prefs.getTone()
        val slotStates = NotificationSlot.entries.map { slot ->
            SlotUiState(
                slot = slot,
                isEnabled = prefs.isSlotEnabled(slot),
                time = prefs.getSlotTime(slot)
            )
        }
        val exactAllowed = NotificationScheduler.canScheduleExactAlarms(context)

        _uiState.value = _uiState.value.copy(
            selectedTone = currentTone,
            slots = slotStates,
            canScheduleExactAlarms = exactAllowed
        )

        // Cache active categories in background
        viewModelScope.launch {
            runCatching {
                val auth = SupabaseClientProvider.client.auth
                val userId = auth.currentUserOrNull()?.id ?: auth.currentSessionOrNull()?.user?.id
                if (userId != null) {
                    val lockIns = SupabaseClientProvider.client.from("lock_ins")
                        .select {
                            filter {
                                eq("user_id", userId)
                            }
                        }
                        .decodeList<LockIn>()
                    val names = lockIns.map { it.name }.toSet()
                    prefs.setActiveCategories(names)
                }
            }
        }
    }

    fun refreshExactAlarmPermission(context: Context) {
        val exactAllowed = NotificationScheduler.canScheduleExactAlarms(context)
        _uiState.value = _uiState.value.copy(canScheduleExactAlarms = exactAllowed)
    }

    fun selectTone(context: Context, tone: MotivationTone) {
        val prefs = NotificationPreferences(context)
        prefs.setTone(tone)
        _uiState.value = _uiState.value.copy(selectedTone = tone)
    }

    fun toggleSlot(context: Context, slot: NotificationSlot, enabled: Boolean) {
        val prefs = NotificationPreferences(context)
        prefs.setSlotEnabled(slot, enabled)

        val timeStr = prefs.getSlotTime(slot)
        if (enabled) {
            NotificationScheduler.scheduleSlot(context, slot, timeStr)
        } else {
            NotificationScheduler.cancelSlot(context, slot)
        }

        val updatedSlots = _uiState.value.slots.map {
            if (it.slot == slot) it.copy(isEnabled = enabled) else it
        }
        _uiState.value = _uiState.value.copy(slots = updatedSlots)
    }

    fun updateSlotTime(context: Context, slot: NotificationSlot, newTime: String) {
        val prefs = NotificationPreferences(context)
        prefs.setSlotTime(slot, newTime)

        if (prefs.isSlotEnabled(slot)) {
            NotificationScheduler.scheduleSlot(context, slot, newTime)
        }

        val updatedSlots = _uiState.value.slots.map {
            if (it.slot == slot) it.copy(time = newTime) else it
        }
        _uiState.value = _uiState.value.copy(slots = updatedSlots)
    }

    fun sendTestNotification(context: Context) {
        NotificationScheduler.triggerImmediateTestNotification(context)
        _uiState.value = _uiState.value.copy(
            infoMessage = "Test notification triggered! Check your notification shade."
        )
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }
}
