package com.nk.motificason.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nk.motificason.data.model.NotificationSlot

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_SLOT_ID = "key_slot_id"
        const val KEY_IS_TEST = "key_is_test"
    }

    override suspend fun doWork(): Result {
        val slotId = inputData.getString(KEY_SLOT_ID) ?: NotificationSlot.MORNING.id
        val isTest = inputData.getBoolean(KEY_IS_TEST, false)

        val prefs = NotificationPreferences(applicationContext)
        val slot = NotificationSlot.entries.find { it.id == slotId } ?: NotificationSlot.MORNING

        // If not a test and slot is disabled by user, don't show notification
        if (!isTest && !prefs.isSlotEnabled(slot)) {
            return Result.success()
        }

        // Show notification via NotificationHelper
        NotificationHelper.showNotificationForSlot(
            context = applicationContext,
            slot = slot,
            isTest = isTest
        )

        // Automatically reschedule next occurrence for tomorrow (chained OneTimeWorkRequest)
        if (!isTest) {
            NotificationScheduler.scheduleSlot(
                context = applicationContext,
                slot = slot,
                timeStr = prefs.getSlotTime(slot)
            )
        }

        return Result.success()
    }
}
