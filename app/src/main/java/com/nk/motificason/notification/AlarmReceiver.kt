package com.nk.motificason.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nk.motificason.data.model.NotificationSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_SLOT_ID = "extra_slot_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val slotId = intent.getStringExtra(EXTRA_SLOT_ID) ?: return
        val slot = NotificationSlot.entries.find { it.id == slotId } ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Show the notification
                NotificationHelper.showNotificationForSlot(context, slot, isTest = false)

                // Reschedule exact alarm for tomorrow
                val prefs = NotificationPreferences(context)
                if (prefs.isSlotEnabled(slot)) {
                    NotificationScheduler.scheduleExactAlarm(context, slot, prefs.getSlotTime(slot))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
