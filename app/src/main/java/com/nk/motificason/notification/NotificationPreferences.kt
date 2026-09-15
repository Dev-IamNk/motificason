package com.nk.motificason.notification

import android.content.Context
import android.content.SharedPreferences
import com.nk.motificason.data.model.MotivationTone
import com.nk.motificason.data.model.NotificationSlot

class NotificationPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "motificason_notification_prefs"
        private const val KEY_TONE = "user_motivation_tone"
        private const val KEY_SLOT_ENABLED_PREFIX = "slot_enabled_"
        private const val KEY_SLOT_TIME_PREFIX = "slot_time_"
    }

    fun getTone(): MotivationTone {
        val toneId = prefs.getString(KEY_TONE, MotivationTone.COACH.id) ?: MotivationTone.COACH.id
        return MotivationTone.fromId(toneId)
    }

    fun setTone(tone: MotivationTone) {
        prefs.edit().putString(KEY_TONE, tone.id).apply()
    }

    fun isSlotEnabled(slot: NotificationSlot): Boolean {
        // By default, all 3 slots are enabled
        return prefs.getBoolean(KEY_SLOT_ENABLED_PREFIX + slot.id, true)
    }

    fun setSlotEnabled(slot: NotificationSlot, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SLOT_ENABLED_PREFIX + slot.id, enabled).apply()
    }

    fun getSlotTime(slot: NotificationSlot): String {
        return prefs.getString(KEY_SLOT_TIME_PREFIX + slot.id, slot.defaultTime) ?: slot.defaultTime
    }

    fun setSlotTime(slot: NotificationSlot, time: String) {
        prefs.edit().putString(KEY_SLOT_TIME_PREFIX + slot.id, time).apply()
    }
}
