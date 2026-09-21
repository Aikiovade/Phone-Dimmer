package io.github.aikiovade.nightdimmer.data

import io.github.aikiovade.nightdimmer.domain.model.DimmerSettings
import io.github.aikiovade.nightdimmer.domain.model.DimmerSettings.Companion.MINUTES_PER_DAY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for every user setting.
 *
 * The whole app (UI, service, alarm receiver) observes this one flow instead of
 * keeping a dozen independent `MutableStateFlow`s scattered across a global
 * controller object, which is what made the first version lose slider changes
 * on restart and drift out of sync with the running service.
 */
class SettingsRepository(private val store: KeyValueStore) {

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<DimmerSettings> = _settings.asStateFlow()

    /** Current settings without collecting the flow. */
    fun snapshot(): DimmerSettings = _settings.value

    /**
     * Applies [transform] to the current settings.
     *
     * @param persist when `false` the change only lives in memory, which is what
     * dragging a slider wants: the overlay follows the finger while the value is
     * written to disk once at the end via [persist].
     */
    fun update(persist: Boolean = true, transform: (DimmerSettings) -> DimmerSettings) {
        val current = _settings.value
        val updated = transform(current).normalized()
        if (updated == current) return
        _settings.value = updated
        if (persist) write(updated)
    }

    /** Writes the in-memory state to the backing store. */
    fun persist() {
        write(_settings.value)
    }

    private fun read(): DimmerSettings = DimmerSettings(
        isEnabled = store.getBoolean(KEY_ENABLED, false),
        dimLevel = store.getFloat(KEY_DIM_LEVEL, DimmerSettings.DEFAULT_DIM_LEVEL),
        autoDimEnabled = store.getBoolean(KEY_AUTO_DIM_ENABLED, false),
        blueLightEnabled = store.getBoolean(KEY_BLUE_LIGHT_ENABLED, false),
        blueLightIntensity = store.getFloat(
            KEY_BLUE_LIGHT_INTENSITY,
            DimmerSettings.DEFAULT_BLUE_LIGHT_INTENSITY,
        ),
        scheduleEnabled = store.getBoolean(KEY_SCHEDULE_ENABLED, false),
        scheduleStartMinutes = store.getInt(
            KEY_SCHEDULE_START_MINUTES,
            DimmerSettings.DEFAULT_SCHEDULE_START_MINUTES,
        ),
        scheduleEndMinutes = store.getInt(
            KEY_SCHEDULE_END_MINUTES,
            DimmerSettings.DEFAULT_SCHEDULE_END_MINUTES,
        ),
        deepBlackThemeEnabled = store.getBoolean(KEY_DEEP_BLACK_THEME, false),
    ).normalized()

    private fun write(settings: DimmerSettings) {
        store.putBoolean(KEY_ENABLED, settings.isEnabled)
        store.putFloat(KEY_DIM_LEVEL, settings.dimLevel)
        store.putBoolean(KEY_AUTO_DIM_ENABLED, settings.autoDimEnabled)
        store.putBoolean(KEY_BLUE_LIGHT_ENABLED, settings.blueLightEnabled)
        store.putFloat(KEY_BLUE_LIGHT_INTENSITY, settings.blueLightIntensity)
        store.putBoolean(KEY_SCHEDULE_ENABLED, settings.scheduleEnabled)
        store.putInt(KEY_SCHEDULE_START_MINUTES, settings.scheduleStartMinutes)
        store.putInt(KEY_SCHEDULE_END_MINUTES, settings.scheduleEndMinutes)
        store.putBoolean(KEY_DEEP_BLACK_THEME, settings.deepBlackThemeEnabled)
    }

    /** Keeps values inside their documented ranges, also for corrupted preferences. */
    private fun DimmerSettings.normalized(): DimmerSettings = copy(
        dimLevel = dimLevel.coerceIn(0f, 1f),
        blueLightIntensity = blueLightIntensity.coerceIn(0f, 1f),
        scheduleStartMinutes = scheduleStartMinutes.coerceIn(0, MINUTES_PER_DAY - 1),
        scheduleEndMinutes = scheduleEndMinutes.coerceIn(0, MINUTES_PER_DAY - 1),
    )

    private companion object {
        const val KEY_ENABLED = "isEnabled"
        const val KEY_DIM_LEVEL = "dimLevel"
        const val KEY_AUTO_DIM_ENABLED = "autoDimEnabled"
        const val KEY_BLUE_LIGHT_ENABLED = "blueLightEnabled"
        const val KEY_BLUE_LIGHT_INTENSITY = "blueLightIntensity"
        const val KEY_SCHEDULE_ENABLED = "scheduleEnabled"
        const val KEY_SCHEDULE_START_MINUTES = "scheduleStartMinutes"
        const val KEY_SCHEDULE_END_MINUTES = "scheduleEndMinutes"
        const val KEY_DEEP_BLACK_THEME = "deepBlackThemeEnabled"
    }
}
