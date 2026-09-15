package com.nk.motificason.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nk.motificason.MainActivity
import com.nk.motificason.R
import com.nk.motificason.data.NotificationRepository
import com.nk.motificason.data.SupabaseClientProvider
import com.nk.motificason.data.model.LockIn
import com.nk.motificason.data.model.NotificationSlot
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_SLOT_ID = "key_slot_id"
        const val KEY_IS_TEST = "key_is_test"
        private const val CHANNEL_ID = "motificason_reminders_channel"
        private const val CHANNEL_NAME = "Daily Motivation Reminders"
    }

    private val repository = NotificationRepository()

    override suspend fun doWork(): Result {
        val slotId = inputData.getString(KEY_SLOT_ID) ?: NotificationSlot.MORNING.id
        val isTest = inputData.getBoolean(KEY_IS_TEST, false)

        val prefs = NotificationPreferences(applicationContext)

        val slot = NotificationSlot.entries.find { it.id == slotId } ?: NotificationSlot.MORNING

        // If not a test and slot is disabled by user, don't show notification
        if (!isTest && !prefs.isSlotEnabled(slot)) {
            return Result.success()
        }

        val userTone = prefs.getTone()

        // Fetch user's active lock-in categories if user is logged in
        val activeCategories = runCatching {
            val auth = SupabaseClientProvider.client.auth
            val userId = auth.currentUserOrNull()?.id ?: auth.currentSessionOrNull()?.user?.id
            if (userId != null) {
                SupabaseClientProvider.client.from("lock_ins")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<LockIn>()
                    .map { it.name }
            } else {
                emptyList()
            }
        }.getOrDefault(emptyList())

        // Fetch message based on tone and categories
        val motivation = repository.getMotivationMessage(userTone, activeCategories)

        // Show Notification
        createNotificationChannelIfNeeded()

        val title = if (isTest) {
            "⚡ Test Notification • ${userTone.displayName} Tone"
        } else {
            "${slot.iconEmoji} ${slot.displayName} • ${motivation.category}"
        }

        showNotification(
            notificationId = if (isTest) 9999 else slot.ordinal + 100,
            title = title,
            message = motivation.message
        )

        // Automatically reschedule next occurrence for tomorrow if this is a recurring slot
        if (!isTest) {
            NotificationScheduler.scheduleSlot(applicationContext, slot, prefs.getSlotTime(slot))
        }

        return Result.success()
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Motivational reminders and daily habit prompts"
                enableVibration(true)
            }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(notificationId: Int, title: String, message: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
        }
    }
}
