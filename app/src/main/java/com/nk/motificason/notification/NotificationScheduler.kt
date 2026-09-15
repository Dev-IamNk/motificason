package com.nk.motificason.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nk.motificason.data.model.NotificationSlot
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun calculateNextOccurrence(timeStr: String, fallbackTime: String = "08:00"): Pair<Long, Long> {
        val now = LocalDateTime.now()
        val localTime = runCatching { LocalTime.parse(timeStr) }.getOrElse {
            runCatching { LocalTime.parse(fallbackTime) }.getOrDefault(LocalTime.of(8, 0))
        }

        var target = now.toLocalDate().atTime(localTime)
        if (!target.isAfter(now)) {
            // Target time has already passed today; schedule for tomorrow
            target = target.plusDays(1)
        }

        val initialDelayMs = Duration.between(now, target).toMillis().coerceAtLeast(1000L)
        val epochMillis = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return Pair(initialDelayMs, epochMillis)
    }

    fun scheduleSlot(context: Context, slot: NotificationSlot, timeStr: String) {
        val (delayMs, _) = calculateNextOccurrence(timeStr, slot.defaultTime)

        // 1. Schedule via WorkManager (chained OneTimeWorkRequest with initial delay)
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(NotificationWorker.KEY_SLOT_ID to slot.id))
            .addTag("notification_slot_${slot.id}")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            getUniqueWorkName(slot),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        // 2. Schedule via AlarmManager for exact timing if allowed
        scheduleExactAlarm(context, slot, timeStr)
    }

    fun scheduleExactAlarm(context: Context, slot: NotificationSlot, timeStr: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (!canScheduleExactAlarms(context)) {
            return
        }

        val (_, targetEpochMillis) = calculateNextOccurrence(timeStr, slot.defaultTime)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_SLOT_ID, slot.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            slot.ordinal + 200,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        runCatching {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                targetEpochMillis,
                pendingIntent
            )
        }
    }

    fun cancelSlot(context: Context, slot: NotificationSlot) {
        // Cancel WorkManager
        WorkManager.getInstance(context).cancelUniqueWork(getUniqueWorkName(slot))

        // Cancel AlarmManager
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager != null) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_SLOT_ID, slot.id)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                slot.ordinal + 200,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
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
        prefs.setInitialized(true)
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
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
