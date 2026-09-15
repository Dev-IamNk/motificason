package com.nk.motificason.data.engine

import com.nk.motificason.data.model.CheckIn
import com.nk.motificason.data.model.StreakFreeze
import java.time.LocalDate

enum class DayStatus {
    COMPLETED,
    FROZEN,
    MISSED,
    UPCOMING
}

data class DayHistory(
    val date: LocalDate,
    val status: DayStatus
)

data class StreakCalculationResult(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletedDays: Int,
    val unusedFreezeCount: Int,
    val freezesConsumedInThisRun: List<Pair<StreakFreeze, LocalDate>>,
    val shouldAwardMilestoneFreeze: Boolean,
    val dayHistory: List<DayHistory>
)

object StreakEngine {

    fun calculate(
        today: LocalDate,
        habitCreatedAt: LocalDate?,
        checkIns: List<CheckIn>,
        freezes: List<StreakFreeze>
    ): StreakCalculationResult {
        val completedDates = checkIns.filter { it.completed }
            .mapNotNull {
                runCatching { LocalDate.parse(it.date) }.getOrNull()
            }.toSet()

        // Track frozen dates and available unused freezes
        val frozenDateToFreezeMap = mutableMapOf<LocalDate, StreakFreeze>()
        val unusedFreezes = mutableListOf<StreakFreeze>()

        for (freeze in freezes) {
            if (!freeze.usedOnDate.isNullOrBlank()) {
                runCatching { LocalDate.parse(freeze.usedOnDate) }.getOrNull()?.let { date ->
                    frozenDateToFreezeMap[date] = freeze
                }
            } else {
                unusedFreezes.add(freeze)
            }
        }

        val newlyConsumedFreezes = mutableListOf<Pair<StreakFreeze, LocalDate>>()

        // --- 1. Current Streak: Walking backward from today ---
        var currentStreak = 0
        val isTodayCompleted = completedDates.contains(today)

        var cursor: LocalDate = if (isTodayCompleted) {
            currentStreak++
            today.minusDays(1)
        } else {
            // Today is in progress/upcoming, so walk starts from yesterday
            today.minusDays(1)
        }

        // Boundary date (earliest activity or creation)
        val earliestActivityDate = (completedDates + frozenDateToFreezeMap.keys).minOrNull()
        val boundaryDate = habitCreatedAt ?: earliestActivityDate ?: today

        while (cursor.isEqual(boundaryDate) || cursor.isAfter(boundaryDate.minusDays(1))) {
            if (completedDates.contains(cursor)) {
                currentStreak++
                cursor = cursor.minusDays(1)
            } else if (frozenDateToFreezeMap.containsKey(cursor)) {
                // Previously frozen day keeps streak alive
                cursor = cursor.minusDays(1)
            } else if (unusedFreezes.isNotEmpty()) {
                // Consume an unused freeze
                val freezeToConsume = unusedFreezes.removeAt(0)
                frozenDateToFreezeMap[cursor] = freezeToConsume
                newlyConsumedFreezes.add(Pair(freezeToConsume, cursor))
                cursor = cursor.minusDays(1)
            } else {
                // Missed day with no freeze available -> streak breaks
                break
            }
        }

        // --- 2. Milestone Freeze Awarding (Multiples of 7) ---
        // A habit earns a freeze at 7, 14, 21...
        val hasEarnedToday = freezes.any { it.earnedAt == today.toString() }
        val shouldAwardMilestoneFreeze = currentStreak >= 7 && (currentStreak % 7 == 0) && !hasEarnedToday

        // --- 3. Longest Streak: Recompute-safe historical walk forward ---
        val startDate = listOfNotNull(habitCreatedAt, earliestActivityDate).minOrNull() ?: today
        var maxStreak = 0
        var runningStreak = 0
        var walkDate = startDate

        while (!walkDate.isAfter(today)) {
            val isDateToday = walkDate.isEqual(today)
            if (completedDates.contains(walkDate)) {
                runningStreak++
                if (runningStreak > maxStreak) {
                    maxStreak = runningStreak
                }
            } else if (frozenDateToFreezeMap.containsKey(walkDate)) {
                // Freeze preserves streak without breaking it
            } else if (isDateToday && !isTodayCompleted) {
                // In-progress today does not break historical streak
            } else {
                runningStreak = 0
            }
            walkDate = walkDate.plusDays(1)
        }
        val longestStreak = maxOf(maxStreak, currentStreak)

        // --- 4. Total Completed Days ---
        val totalCompletedDays = completedDates.size

        // --- 5. DayHistory Classification for calendar heatmap (last 30 days) ---
        val historyDays = mutableListOf<DayHistory>()
        var dayPointer = today.minusDays(29)
        while (!dayPointer.isAfter(today)) {
            val status = when {
                dayPointer.isEqual(today) && !isTodayCompleted -> DayStatus.UPCOMING
                dayPointer.isAfter(today) -> DayStatus.UPCOMING
                completedDates.contains(dayPointer) -> DayStatus.COMPLETED
                frozenDateToFreezeMap.containsKey(dayPointer) -> DayStatus.FROZEN
                dayPointer.isBefore(boundaryDate) -> DayStatus.UPCOMING
                else -> DayStatus.MISSED
            }
            historyDays.add(DayHistory(date = dayPointer, status = status))
            dayPointer = dayPointer.plusDays(1)
        }

        return StreakCalculationResult(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalCompletedDays = totalCompletedDays,
            unusedFreezeCount = unusedFreezes.size,
            freezesConsumedInThisRun = newlyConsumedFreezes,
            shouldAwardMilestoneFreeze = shouldAwardMilestoneFreeze,
            dayHistory = historyDays
        )
    }
}
