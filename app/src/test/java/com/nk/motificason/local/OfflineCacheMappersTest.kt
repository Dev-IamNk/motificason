package com.nk.motificason.local

import com.nk.motificason.data.local.CheckInEntity
import com.nk.motificason.data.local.HabitEntity
import com.nk.motificason.data.local.HabitStreakEntity
import com.nk.motificason.data.local.LockInEntity
import com.nk.motificason.data.local.toDomain
import com.nk.motificason.data.local.toEntity
import com.nk.motificason.data.model.CheckIn
import com.nk.motificason.data.model.Habit
import com.nk.motificason.data.model.LockIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineCacheMappersTest {

    @Test
    fun testLockInEntity_mapping() {
        val lockIn = LockIn(
            id = "lock_1",
            userId = "user_1",
            name = "Coding Grind",
            emoji = "💻",
            createdAt = "2026-09-01T10:00:00Z"
        )
        val entity = lockIn.toEntity()
        assertEquals(lockIn.id, entity.id)
        assertEquals(lockIn.userId, entity.userId)
        assertEquals(lockIn.name, entity.name)
        assertEquals(lockIn.emoji, entity.emoji)
        assertEquals(lockIn.createdAt, entity.createdAt)

        val domain = entity.toDomain()
        assertEquals(lockIn, domain)
    }

    @Test
    fun testHabitEntity_mapping() {
        val habit = Habit(
            id = "h_1",
            lockInId = "lock_1",
            userId = "user_1",
            title = "LeetCode 2 Problems",
            createdAt = "2026-09-01T10:05:00Z"
        )
        val entity = habit.toEntity()
        assertEquals(habit.id, entity.id)
        assertEquals(habit.lockInId, entity.lockInId)
        assertEquals(habit.userId, entity.userId)
        assertEquals(habit.title, entity.title)
        assertEquals(habit.createdAt, entity.createdAt)

        val domain = entity.toDomain()
        assertEquals(habit, domain)
    }

    @Test
    fun testCheckInEntity_offlineAndSyncedMapping() {
        val checkIn = CheckIn(
            id = "c_1",
            habitId = "h_1",
            userId = "user_1",
            date = "2026-09-16",
            completed = true,
            createdAt = "2026-09-16T08:00:00Z"
        )

        // Default synced entity
        val syncedEntity = checkIn.toEntity(synced = true)
        assertTrue(syncedEntity.synced)
        assertEquals(checkIn, syncedEntity.toDomain())

        // Offline entity with synced = false
        val offlineEntity = checkIn.toEntity(synced = false)
        assertFalse(offlineEntity.synced)
        assertEquals(checkIn, offlineEntity.toDomain())
    }

    @Test
    fun testHabitStreakEntity_structure() {
        val streakEntity = HabitStreakEntity(
            habitId = "h_1",
            userId = "user_1",
            currentStreak = 14,
            longestStreak = 21,
            totalCompletedDays = 35,
            unusedFreezeCount = 2,
            updatedAt = "2026-09-16"
        )

        assertEquals("h_1", streakEntity.habitId)
        assertEquals(14, streakEntity.currentStreak)
        assertEquals(21, streakEntity.longestStreak)
        assertEquals(35, streakEntity.totalCompletedDays)
        assertEquals(2, streakEntity.unusedFreezeCount)
        assertEquals("2026-09-16", streakEntity.updatedAt)
    }
}
