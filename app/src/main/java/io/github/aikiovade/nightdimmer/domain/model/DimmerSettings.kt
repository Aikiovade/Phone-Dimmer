package io.github.aikiovade.nightdimmer.domain.model

/**
 * Everything the user can configure in the app.
 *
 * Instances are immutable; mutate them through
 * [io.github.aikiovade.nightdimmer.data.SettingsRepository.update], which also
 * normalises out-of-range values.
 */
data class DimmerSettings(
    val isEnabled: Boolean = false,
    /** `0f` = no dimming, `1f` = fully black overlay. */
    val dimLevel: Float = DEFAULT_DIM_LEVEL,
    val autoDimEnabled: Boolean = false,
    val blueLightEnabled: Boolean = false,
    /** `0f`..`1f`, scales the warm overlay applied on top of the screen. */
    val blueLightIntensity: Float = DEFAULT_BLUE_LIGHT_INTENSITY,
    val scheduleEnabled: Boolean = false,
    /** Minutes from midnight, `0..1439`. */
    val scheduleStartMinutes: Int = DEFAULT_SCHEDULE_START_MINUTES,
    /** Minutes from midnight, `0..1439`. Equal to the start means "no window". */
    val scheduleEndMinutes: Int = DEFAULT_SCHEDULE_END_MINUTES,
    val deepBlackThemeEnabled: Boolean = false,
) {
    companion object {
        const val DEFAULT_DIM_LEVEL = 0.8f
        const val DEFAULT_BLUE_LIGHT_INTENSITY = 0.5f
        const val DEFAULT_SCHEDULE_START_MINUTES = 22 * 60
        const val DEFAULT_SCHEDULE_END_MINUTES = 7 * 60
        const val MINUTES_PER_DAY = 24 * 60
        val MINUTE_OF_DAY_RANGE = 0 until MINUTES_PER_DAY
    }
}
