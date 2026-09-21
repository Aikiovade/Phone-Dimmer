package io.github.aikiovade.nightdimmer.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleWindowTest {

    @Test
    fun `a window inside one day is a half open interval`() {
        val window = ScheduleWindow(startMinutes = 1 * 60, endMinutes = 3 * 60)

        assertFalse(window.contains(0))
        assertTrue(window.contains(1 * 60))
        assertTrue(window.contains(2 * 60))
        assertFalse(window.contains(3 * 60))
        assertFalse(window.contains(4 * 60))
    }

    @Test
    fun `a window crossing midnight covers both sides of the day`() {
        val window = ScheduleWindow(startMinutes = 22 * 60, endMinutes = 7 * 60)

        assertTrue(window.contains(22 * 60))
        assertTrue(window.contains(23 * 60 + 59))
        assertTrue(window.contains(0))
        assertTrue(window.contains(6 * 60 + 59))
        assertFalse(window.contains(7 * 60))
        assertFalse(window.contains(12 * 60))
    }

    @Test
    fun `equal start and end disable the window`() {
        val window = ScheduleWindow(startMinutes = 8 * 60, endMinutes = 8 * 60)

        assertFalse(window.isEnabled)
        assertFalse(window.contains(8 * 60))
    }

    @Test
    fun `times outside the day are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { ScheduleWindow(-1, 60) }
        assertThrows(IllegalArgumentException::class.java) {
            ScheduleWindow(0, DimmerSettings.MINUTES_PER_DAY)
        }
    }
}
