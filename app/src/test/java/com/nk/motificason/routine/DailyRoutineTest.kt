package com.nk.motificason.routine

import com.nk.motificason.data.model.DailyActivity
import com.nk.motificason.ui.routine.RoutineClockHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyRoutineTest {

    @Test
    fun testParseMinutes_validTimes() {
        assertEquals(0, RoutineClockHelper.parseMinutes("00:00"))
        assertEquals(390, RoutineClockHelper.parseMinutes("06:30"))
        assertEquals(720, RoutineClockHelper.parseMinutes("12:00"))
        assertEquals(1080, RoutineClockHelper.parseMinutes("18:00"))
        assertEquals(1439, RoutineClockHelper.parseMinutes("23:59"))
    }

    @Test
    fun testCalculateDurationMinutes_sameDayAndOvernight() {
        // Standard daytime duration
        val durationDay = RoutineClockHelper.calculateDurationMinutes("09:00", "10:30")
        assertEquals(90, durationDay)

        // Same hour duration
        val durationShort = RoutineClockHelper.calculateDurationMinutes("14:15", "14:45")
        assertEquals(30, durationShort)

        // Overnight crossing midnight (e.g. 23:00 to 01:30)
        val durationOvernight = RoutineClockHelper.calculateDurationMinutes("23:00", "01:30")
        assertEquals(150, durationOvernight)
    }

    @Test
    fun testFormatDuration() {
        assertEquals("1h 30m", RoutineClockHelper.formatDuration(90))
        assertEquals("2h", RoutineClockHelper.formatDuration(120))
        assertEquals("45m", RoutineClockHelper.formatDuration(45))
        assertEquals("0m", RoutineClockHelper.formatDuration(0))
    }

    @Test
    fun testCalculateClockArcs_anglesAndDurations() {
        val activities = listOf(
            DailyActivity(
                id = "1",
                userId = "u1",
                title = "Morning Workout",
                emoji = "🏋️",
                date = "2026-09-16",
                startTime = "06:00",
                endTime = "07:30"
            ),
            DailyActivity(
                id = "2",
                userId = "u1",
                title = "Deep Work",
                emoji = "💻",
                date = "2026-09-16",
                startTime = "09:00",
                endTime = "12:00"
            ),
            DailyActivity(
                id = "3",
                userId = "u1",
                title = "Afternoon Study",
                emoji = "📚",
                date = "2026-09-16",
                startTime = "14:00",
                endTime = "16:00"
            )
        )

        val arcs = RoutineClockHelper.calculateClockArcs(activities)
        assertEquals(3, arcs.size)

        // 06:00: 360 mins * 0.25 deg/min = 90 deg from midnight.
        // Start angle = -90 + 90 = 0 degrees (pointing East / 3 o'clock)
        val arc1 = arcs[0]
        assertEquals(0f, arc1.startAngle, 0.01f)
        // 90 minutes * 0.25 deg/min = 22.5 deg sweep
        assertEquals(22.5f, arc1.sweepAngle, 0.01f)
        assertEquals(90, arc1.durationMinutes)
        assertEquals("1h 30m", arc1.formattedDuration)
        assertEquals("06:00 - 07:30", arc1.formattedTimeSpan)

        // 09:00: 540 mins * 0.25 = 135 deg from midnight.
        // Start angle = -90 + 135 = 45 degrees
        val arc2 = arcs[1]
        assertEquals(45f, arc2.startAngle, 0.01f)
        // 180 minutes * 0.25 = 45 deg sweep
        assertEquals(45f, arc2.sweepAngle, 0.01f)
        assertEquals(180, arc2.durationMinutes)
        assertEquals("3h", arc2.formattedDuration)

        // 14:00: 840 mins * 0.25 = 210 deg from midnight.
        // Start angle = -90 + 210 = 120 degrees
        val arc3 = arcs[2]
        assertEquals(120f, arc3.startAngle, 0.01f)
        // 120 minutes * 0.25 = 30 deg sweep
        assertEquals(30f, arc3.sweepAngle, 0.01f)
        assertEquals(120, arc3.durationMinutes)
        assertEquals("2h", arc3.formattedDuration)
    }
}
