package io.github.aikiovade.nightdimmer.domain

/**
 * The two overlay layers that are drawn on top of the screen.
 *
 * Keeping this a pure value means the "how dark / how warm" decision can be
 * unit tested and previewed without touching a [android.view.WindowManager].
 */
data class OverlaySpec(
    /** Alpha of the black dim layer, `0f..1f`. */
    val dimAlpha: Float,
    /** Alpha of the warm blue-light filter layer, `0f..1f`. */
    val tintAlpha: Float,
) {
    companion object {
        /**
         * Amber tint (`#FFFFA000`): removes blue while keeping reds and greens
         * readable. Kept as a plain Int so this file stays free of Android
         * framework calls and can be unit tested on the JVM.
         */
        val TINT_COLOR: Int = 0xFFFFA000u.toInt()

        /** The filter never gets fully opaque, otherwise the screen is unusable. */
        const val MAX_TINT_ALPHA = 0.6f

        fun of(
            dimLevel: Float,
            blueLightEnabled: Boolean,
            blueLightIntensity: Float,
        ): OverlaySpec = OverlaySpec(
            dimAlpha = dimLevel.coerceIn(0f, 1f),
            tintAlpha = if (blueLightEnabled) {
                blueLightIntensity.coerceIn(0f, 1f) * MAX_TINT_ALPHA
            } else {
                0f
            },
        )
    }
}
