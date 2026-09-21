package io.github.aikiovade.nightdimmer.domain.model

import io.github.aikiovade.nightdimmer.domain.model.DimmerSettings.Companion.MINUTE_OF_DAY_RANGE
import io.github.aikiovade.nightdimmer.domain.model.DimmerSettings.Companion.MINUTES_PER_DAY

/**
 * A daily time window expressed in minutes from midnight.
 *
 * The window may cross midnight (`22:00 - 07:00`). Start and end being equal
 * means the window is disabled, so a schedule can never run 24/7 by accident.
 */
data class ScheduleWindow(
    val startMinutes: Int,
    val endMinutes: Int,
) {
    init {
        require(startMinutes in MINUTE_OF_DAY_RANGE) { "startMinutes out of range: $startMinutes" }
        require(endMinutes in MINUTE_OF_DAY_RANGE) { "endMinutes out of range: $endMinutes" }
    }

    val isEnabled: Boolean get() = startMinutes != endMinutes

    /** Start is inclusive, end is exclusive, exactly like a half-open interval. */
    fun contains(minuteOfDay: Int): Boolean {
        require(minuteOfDay in MINUTE_OF_DAY_RANGE) { "minuteOfDay out of range: $minuteOfDay" }
        if (!isEnabled) return false
        return if (startMinutes < endMinutes) {
            minuteOfDay >= startMinutes && minuteOfDay < endMinutes
        } else {
            minuteOfDay >= startMinutes || minuteOfDay < endMinutes
        }
    }

    companion object {
        const val MAX_MINUTE_OF_DAY = MINUTES_PER_DAY - 1
    }
}
