package com.nk.motificason.ui.routine

import androidx.compose.ui.graphics.Color
import com.nk.motificason.data.model.DailyActivity

val ROUTINE_PALETTE = listOf(
    Color(0xFF00E676), // Emerald
    Color(0xFF00E5FF), // Cyan
    Color(0xFFFFD54F), // Amber
    Color(0xFFFF5252), // Coral
    Color(0xFFE040FB), // Purple
    Color(0xFF448AFF), // Electric Blue
    Color(0xFFFF9100), // Orange
    Color(0xFF69F0AE), // Mint
    Color(0xFFB388FF), // Lavender
    Color(0xFFFF4081)  // Hot Pink
)

data class ClockBlockArc(
    val activity: DailyActivity,
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Color,
    val durationMinutes: Int,
    val formattedDuration: String,
    val formattedTimeSpan: String
)

object RoutineClockHelper {

    fun parseMinutes(timeStr: String): Int {
        val parts = timeStr.trim().split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val mins = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return (hours * 60 + mins).coerceIn(0, 1440)
    }

    fun calculateDurationMinutes(startStr: String, endStr: String): Int {
        val startMins = parseMinutes(startStr)
        val endMins = parseMinutes(endStr)
        return if (endMins >= startMins) {
            endMins - startMins
        } else {
            (1440 - startMins) + endMins
        }
    }

    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    fun formatDisplayTime(timeStr: String): String {
        val parts = timeStr.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return "%02d:%02d".format(h, m)
    }

    fun formatTimeSpan(startStr: String, endStr: String): String {
        return "${formatDisplayTime(startStr)} - ${formatDisplayTime(endStr)}"
    }

    fun calculateClockArcs(activities: List<DailyActivity>): List<ClockBlockArc> {
        return activities.mapIndexed { index, activity ->
            val startMin = parseMinutes(activity.startTime)
            val duration = calculateDurationMinutes(activity.startTime, activity.endTime)

            // -90 degrees is the top (12 AM / 00:00).
            // 360 degrees / 1440 minutes = 0.25 degrees per minute.
            val startAngle = -90f + (startMin * 0.25f)
            val sweepAngle = (duration * 0.25f).coerceAtLeast(3f)
            val color = ROUTINE_PALETTE[index % ROUTINE_PALETTE.size]

            ClockBlockArc(
                activity = activity,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                color = color,
                durationMinutes = duration,
                formattedDuration = formatDuration(duration),
                formattedTimeSpan = formatTimeSpan(activity.startTime, activity.endTime)
            )
        }
    }
}
