package io.github.aikiovade.nightdimmer.domain

import io.github.aikiovade.nightdimmer.domain.model.ScheduleWindow
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleCalculatorTest {

    private val overnightWindow = ScheduleWindow(startMinutes = 22 * 60, endMinutes = 7 * 60)

    @Test
    fun `before the window the next action enables the dimmer today`() {
        val action = ScheduleCalculator.nextAction(overnightWindow, at(20, 0))

        assertEquals(ScheduleAction.Enable(at(22, 0)), action)
    }

    @Test
    fun `inside the window the next action disables the dimmer in the morning`() {
        val action = ScheduleCalculator.nextAction(overnightWindow, at(23, 0))

        assertEquals(ScheduleAction.Disable(LocalDateTime.of(2026, 1, 16, 7, 0)), action)
    }

    @Test
    fun `after midnight the next action disables the dimmer on the same day`() {
        val action = ScheduleCalculator.nextAction(overnightWindow, at(3, 0))

        assertEquals(ScheduleAction.Disable(at(7, 0)), action)
    }

    /**
     * Regression test: the previous implementation scheduled the start and the
     * stop independently and could therefore keep the dimmer dark for almost a
     * full day when the device booted inside the window.
     */
    @Test
    fun `a reboot inside the window must not wait for the next start`() {
        val action = ScheduleCalculator.nextAction(overnightWindow, at(3, 0))

        assertEquals(ScheduleAction.Disable(at(7, 0)), action)
    }

    @Test
    fun `exactly at the start minute the window is already active`() {
        val action = ScheduleCalculator.nextAction(overnightWindow, at(22, 0))

        assertEquals(ScheduleAction.Disable(LocalDateTime.of(2026, 1, 16, 7, 0)), action)
    }

    @Test
    fun `exactly at the end minute the dimmer turns on at the next start`() {
        val action = ScheduleCalculator.nextAction(overnightWindow, at(7, 0))

        assertEquals(ScheduleAction.Enable(at(22, 0)), action)
    }

    @Test
    fun `a daytime window outside its hours schedules the next day`() {
        val window = ScheduleWindow(startMinutes = 1 * 60, endMinutes = 3 * 60)

        val action = ScheduleCalculator.nextAction(window, at(10, 0))

        assertEquals(ScheduleAction.Enable(LocalDateTime.of(2026, 1, 16, 1, 0)), action)
    }

    @Test
    fun `a disabled window never schedules anything`() {
        val window = ScheduleWindow(startMinutes = 8 * 60, endMinutes = 8 * 60)

        assertNull(ScheduleCalculator.nextAction(window, at(10, 0)))
    }

    private fun at(hour: Int, minute: Int): LocalDateTime =
        LocalDateTime.of(2026, 1, 15, hour, minute)
}
