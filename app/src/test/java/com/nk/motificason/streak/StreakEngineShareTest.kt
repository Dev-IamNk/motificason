package com.nk.motificason.streak

import com.nk.motificason.data.engine.DayStatus
import com.nk.motificason.data.engine.StreakEngine
import com.nk.motificason.data.model.CheckIn
import com.nk.motificason.data.model.StreakFreeze
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakEngineShareTest {

    @Test
    fun testExtractCurrentStreakDays_withCompletedAndFrozenDays() {
        val today = LocalDate.of(2026, 9, 16)
        val habitCreatedAt = LocalDate.of(2026, 9, 1)

        // Streak of 5 days:
        // Sep 16 (today): completed
        // Sep 15: completed
        // Sep 14: frozen
        // Sep 13: completed
        // Sep 12: completed
        val checkIns = listOf(
            CheckIn(id = "1", habitId = "h1", userId = "u1", date = "2026-09-12", completed = true),
            CheckIn(id = "2", habitId = "h1", userId = "u1", date = "2026-09-13", completed = true),
            CheckIn(id = "3", habitId = "h1", userId = "u1", date = "2026-09-15", completed = true),
            CheckIn(id = "4", habitId = "h1", userId = "u1", date = "2026-09-16", completed = true)
        )
        val freezes = listOf(
            StreakFreeze(id = "f1", habitId = "h1", userId = "u1", earnedAt = "2026-09-10", usedOnDate = "2026-09-14")
        )

        val result = StreakEngine.calculate(today, habitCreatedAt, checkIns, freezes)
        assertEquals(4, result.currentStreak)

        val streakDays = StreakEngine.extractCurrentStreakDays(result, maxDays = 30)
        assertEquals(5, streakDays.size)

        // Chronological order from Day 1 to Day 5
        assertEquals(LocalDate.of(2026, 9, 12), streakDays[0].date)
        assertEquals(DayStatus.COMPLETED, streakDays[0].status)

        assertEquals(LocalDate.of(2026, 9, 13), streakDays[1].date)
        assertEquals(DayStatus.COMPLETED, streakDays[1].status)

        assertEquals(LocalDate.of(2026, 9, 14), streakDays[2].date)
        assertEquals(DayStatus.FROZEN, streakDays[2].status)

        assertEquals(LocalDate.of(2026, 9, 15), streakDays[3].date)
        assertEquals(DayStatus.COMPLETED, streakDays[3].status)

        assertEquals(LocalDate.of(2026, 9, 16), streakDays[4].date)
        assertEquals(DayStatus.COMPLETED, streakDays[4].status)
    }

    @Test
    fun testExtractCurrentStreakDays_cappedAtMaxDays() {
        val today = LocalDate.of(2026, 9, 16)
        val habitCreatedAt = LocalDate.of(2026, 7, 1)

        // 35 consecutive completed check-ins
        val checkIns = (0 until 35).map { offset ->
            val dateStr = today.minusDays(offset.toLong()).toString()
            CheckIn(id = "c$offset", habitId = "h1", userId = "u1", date = dateStr, completed = true)
        }

        val result = StreakEngine.calculate(today, habitCreatedAt, checkIns, emptyList())
        assertEquals(35, result.currentStreak)

        val cappedDays = StreakEngine.extractCurrentStreakDays(result, maxDays = 30)
        assertEquals(30, cappedDays.size)
        // Earliest in the capped 30-day window
        assertEquals(today.minusDays(29), cappedDays.first().date)
        // Latest in the capped 30-day window (today)
        assertEquals(today, cappedDays.last().date)
    }
}
