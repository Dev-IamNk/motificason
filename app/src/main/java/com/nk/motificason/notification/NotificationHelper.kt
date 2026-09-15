package com.nk.motificason.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nk.motificason.MainActivity
import com.nk.motificason.R
import com.nk.motificason.data.NotificationRepository
import com.nk.motificason.data.model.NotificationSlot

object NotificationHelper {

    const val CHANNEL_ID = "motificason_reminders_channel"
    private const val CHANNEL_NAME = "Daily Motivation Reminders"
    private val repository = NotificationRepository()

    suspend fun showNotificationForSlot(
        context: Context,
        slot: NotificationSlot,
        isTest: Boolean = false
    ) {
        val prefs = NotificationPreferences(context)

        if (!isTest && !prefs.isSlotEnabled(slot)) {
            return
        }

        val userTone = prefs.getTone()
        val cachedCategories = prefs.getActiveCategories().toList()

        // Fetch message based on tone and categories
        val motivation = repository.getMotivationMessage(userTone, cachedCategories)

        createNotificationChannelIfNeeded(context)

        val title = if (isTest) {
            "⚡ Test Notification • ${userTone.displayName} Tone"
        } else {
            "${slot.iconEmoji} ${slot.displayName} • ${motivation.category}"
        }

        val notificationId = if (isTest) 9999 else slot.ordinal + 100

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(motivation.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(motivation.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    fun createNotificationChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Motivational reminders and daily habit prompts"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
