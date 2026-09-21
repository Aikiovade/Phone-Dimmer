package io.github.aikiovade.nightdimmer.domain

import io.github.aikiovade.nightdimmer.domain.model.ScheduleWindow
import java.time.LocalDateTime

/** The next thing the schedule wants to happen. */
sealed interface ScheduleAction {
    /** Wall-clock instant at which the dimmer must be switched on. */
    data class Enable(override val at: LocalDateTime) : ScheduleAction

    /** Wall-clock instant at which the dimmer must be switched off. */
    data class Disable(override val at: LocalDateTime) : ScheduleAction

    val at: LocalDateTime
}

/**
 * Pure schedule arithmetic.
 *
 * The previous implementation computed the start and the stop alarms
 * independently, which produced a wrong pair whenever "now" was already inside
 * the window (for example right after a reboot at 03:00 with a 22:00-07:00
 * window: the dimmer stayed dark until 22:00 the next day). This calculator
 * always answers a single question - "what is the next transition?" - so the
 * caller cannot end up with a mismatched pair.
 */
object ScheduleCalculator {

    fun nextAction(window: ScheduleWindow, now: LocalDateTime): ScheduleAction? {
        if (!window.isEnabled) return null
        val minuteOfDay = now.hour * 60 + now.minute
        return if (window.contains(minuteOfDay)) {
            ScheduleAction.Disable(nextOccurrence(now, window.endMinutes))
        } else {
            ScheduleAction.Enable(nextOccurrence(now, window.startMinutes))
        }
    }

    private fun nextOccurrence(now: LocalDateTime, minuteOfDay: Int): LocalDateTime {
        val today = now.toLocalDate().atTime(minuteOfDay / 60, minuteOfDay % 60)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }
}
