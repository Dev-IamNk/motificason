package com.nk.motificason.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nk.motificason.data.model.NotificationSlot
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleSlot(context: Context, slot: NotificationSlot, timeStr: String) {
        val now = LocalDateTime.now()
        val localTime = runCatching { LocalTime.parse(timeStr) }.getOrElse {
            runCatching { LocalTime.parse(slot.defaultTime) }.getOrDefault(LocalTime.of(8, 0))
        }

        var target = now.toLocalDate().atTime(localTime)
        if (!target.isAfter(now)) {
            // Target time has already passed today; schedule for tomorrow
            target = target.plusDays(1)
        }

        val initialDelayMs = Duration.between(now, target).toMillis().coerceAtLeast(1000L)

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(NotificationWorker.KEY_SLOT_ID to slot.id))
            .addTag("notification_slot_${slot.id}")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            getUniqueWorkName(slot),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelSlot(context: Context, slot: NotificationSlot) {
        WorkManager.getInstance(context).cancelUniqueWork(getUniqueWorkName(slot))
    }

    fun rescheduleAll(context: Context) {
        val prefs = NotificationPreferences(context)
        for (slot in NotificationSlot.entries) {
            if (prefs.isSlotEnabled(slot)) {
                val timeStr = prefs.getSlotTime(slot)
                scheduleSlot(context, slot, timeStr)
            } else {
                cancelSlot(context, slot)
            }
        }
    }

    fun triggerImmediateTestNotification(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(
                workDataOf(
                    NotificationWorker.KEY_SLOT_ID to "test_slot",
                    NotificationWorker.KEY_IS_TEST to true
                )
            )
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    private fun getUniqueWorkName(slot: NotificationSlot): String = "motificason_slot_${slot.id}"
}
