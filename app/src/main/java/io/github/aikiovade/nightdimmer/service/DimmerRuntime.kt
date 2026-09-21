package io.github.aikiovade.nightdimmer.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-scoped state that only exists while the dimmer runs.
 *
 * [DimmerService] is the writer, the UI is the reader. It is deliberately kept
 * out of [io.github.aikiovade.nightdimmer.data.SettingsRepository] because none
 * of it should ever be persisted: after a restart the effective level is
 * derived from the settings again.
 */
class DimmerRuntime {

    private val _effectiveDimLevel = MutableStateFlow(0f)

    /** The dim level that is currently applied to the screen. */
    val effectiveDimLevel: StateFlow<Float> = _effectiveDimLevel.asStateFlow()

    private val _lightSensorAvailable = MutableStateFlow(true)

    /** `false` on devices without an ambient light sensor. */
    val lightSensorAvailable: StateFlow<Boolean> = _lightSensorAvailable.asStateFlow()

    fun setEffectiveDimLevel(level: Float) {
        _effectiveDimLevel.value = level.coerceIn(0f, 1f)
    }

    fun setLightSensorAvailable(available: Boolean) {
        _lightSensorAvailable.value = available
    }
}
