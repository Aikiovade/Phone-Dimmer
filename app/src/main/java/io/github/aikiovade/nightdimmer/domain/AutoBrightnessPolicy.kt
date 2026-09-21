package io.github.aikiovade.nightdimmer.domain

/**
 * Maps the ambient light level reported by the light sensor to a dim level.
 *
 * Two safeguards keep the overlay from flickering, which the first version of
 * this app suffered from because it re-evaluated raw thresholds on every
 * sensor event:
 *
 *  * a hysteresis band around every threshold, so small fluctuations next to a
 *    boundary do not toggle the level back and forth;
 *  * exponential smoothing of the emitted value, so a real change fades in
 *    instead of jumping.
 *
 * The class is intentionally free of Android dependencies and therefore unit
 * testable.
 */
class AutoBrightnessPolicy(
    private val thresholdsLux: FloatArray = DEFAULT_THRESHOLDS_LUX,
    private val dimLevels: FloatArray = DEFAULT_DIM_LEVELS,
    private val hysteresisRatio: Float = DEFAULT_HYSTERESIS_RATIO,
    private val smoothingFactor: Float = DEFAULT_SMOOTHING_FACTOR,
) {
    init {
        require(dimLevels.size == thresholdsLux.size + 1) {
            "dimLevels must contain exactly one more entry than thresholdsLux"
        }
        require(thresholdsLux.isNotEmpty()) { "at least one threshold is required" }
        require(hysteresisRatio in 0f..1f) { "hysteresisRatio must be in 0f..1f" }
        require(smoothingFactor in 0f..1f) { "smoothingFactor must be in 0f..1f" }
    }

    private var levelIndex: Int? = null
    private var smoothed: Float? = null

    /** Forgets the previous measurements, e.g. when auto-dimming is switched off. */
    fun reset() {
        levelIndex = null
        smoothed = null
    }

    /**
     * Seeds the smoothed value so the next [onLux] call fades from [dimLevel]
     * instead of jumping. Used when auto-dimming takes over from a manual level.
     */
    fun seed(dimLevel: Float) {
        smoothed = dimLevel.coerceIn(0f, 1f)
    }

    /** The level that [lux] maps to without hysteresis or smoothing. */
    fun targetFor(lux: Float): Float = dimLevels[rawIndexFor(lux)]

    /** Feeds a new measurement and returns the dim level that should be applied. */
    fun onLux(lux: Float): Float {
        val index = resolveIndex(lux)
        levelIndex = index

        val target = dimLevels[index]
        val previous = smoothed
        val next = if (previous == null) {
            target
        } else {
            previous + smoothingFactor * (target - previous)
        }
        smoothed = next
        return next.coerceIn(0f, 1f)
    }

    private fun rawIndexFor(lux: Float): Int {
        val index = thresholdsLux.indexOfFirst { lux < it }
        return if (index == -1) thresholdsLux.size else index
    }

    private fun resolveIndex(lux: Float): Int {
        var index = levelIndex ?: return rawIndexFor(lux)
        // Move towards darker levels only when the measurement drops clearly
        // below the boundary of the level we are about to enter.
        while (index > 0 && lux < thresholdsLux[index - 1] * (1f - hysteresisRatio)) {
            index--
        }
        // Move towards brighter levels only when the measurement rises clearly
        // above the boundary we are about to leave.
        while (index < thresholdsLux.size && lux > thresholdsLux[index] * (1f + hysteresisRatio)) {
            index++
        }
        return index
    }

    companion object {
        /** Levels from darkest to brightest; entry `i` applies below `DEFAULT_THRESHOLDS_LUX[i]`. */
        val DEFAULT_THRESHOLDS_LUX = floatArrayOf(2f, 10f, 50f, 200f)
        val DEFAULT_DIM_LEVELS = floatArrayOf(0.8f, 0.6f, 0.4f, 0.2f, 0.0f)
        const val DEFAULT_HYSTERESIS_RATIO = 0.25f
        const val DEFAULT_SMOOTHING_FACTOR = 0.5f
    }
}
