package com.nk.motificason.notification

import com.nk.motificason.data.model.DEFAULT_MOTIVATION_MESSAGES
import com.nk.motificason.data.model.MotivationTone
import com.nk.motificason.data.model.NotificationSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class NotificationModelsTest {

    @Test
    fun testNotificationSlots_haveValidTimes() {
        assertEquals(3, NotificationSlot.entries.size)

        for (slot in NotificationSlot.entries) {
            val parsed = LocalTime.parse(slot.defaultTime)
            assertNotNull(parsed)
        }
    }

    @Test
    fun testMotivationTones_fromId() {
        assertEquals(MotivationTone.FRIENDLY, MotivationTone.fromId("Friendly"))
        assertEquals(MotivationTone.COACH, MotivationTone.fromId("Coach"))
        assertEquals(MotivationTone.SAVAGE, MotivationTone.fromId("Savage"))
        assertEquals(MotivationTone.BRAINROT, MotivationTone.fromId("Brainrot"))
        // Case-insensitive fallback
        assertEquals(MotivationTone.SAVAGE, MotivationTone.fromId("savage"))
        assertEquals(MotivationTone.COACH, MotivationTone.fromId("UnknownTone"))
    }

    @Test
    fun testDefaultMotivationMessages_coversAllTonesAndCategories() {
        val categories = listOf(
            "Coding Grind",
            "Gym Mode",
            "Music Training",
            "Study Mode",
            "Dopamine Detox",
            "Sleep Reset",
            "General"
        )

        for (tone in MotivationTone.entries) {
            for (category in categories) {
                val matches = DEFAULT_MOTIVATION_MESSAGES.filter {
                    it.tone.equals(tone.id, ignoreCase = true) &&
                            it.category.equals(category, ignoreCase = true)
                }
                assertTrue(
                    "Missing message for tone: ${tone.id}, category: $category",
                    matches.isNotEmpty()
                )
                assertTrue(
                    "Message content should not be blank",
                    matches.first().message.isNotBlank()
                )
            }
        }
    }

    @Test
    fun testCalculateNextOccurrence_alwaysInFutureAndUnder24Hours() {
        // Test various times of day
        val testTimes = listOf("00:00", "08:00", "12:30", "18:45", "23:59")
        val maxDelayMs = java.util.concurrent.TimeUnit.HOURS.toMillis(24)

        for (timeStr in testTimes) {
            val (delayMs, epochMillis) = NotificationScheduler.calculateNextOccurrence(timeStr)
            assertTrue("Delay should be positive for $timeStr", delayMs > 0)
            assertTrue("Delay should never exceed 24 hours for $timeStr", delayMs <= maxDelayMs)
            assertTrue("Target epoch millis should be in the future for $timeStr", epochMillis > System.currentTimeMillis())
        }
    }
}
