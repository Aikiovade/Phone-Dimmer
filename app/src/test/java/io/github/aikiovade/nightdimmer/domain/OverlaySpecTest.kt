package io.github.aikiovade.nightdimmer.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlaySpecTest {

    @Test
    fun `dimming alone only changes the dim layer`() {
        val spec = OverlaySpec.of(dimLevel = 0.4f, blueLightEnabled = false, blueLightIntensity = 0.9f)

        assertEquals(0.4f, spec.dimAlpha, TOLERANCE)
        assertEquals(0f, spec.tintAlpha, TOLERANCE)
    }

    @Test
    fun `the tint scales with the configured intensity`() {
        val spec = OverlaySpec.of(dimLevel = 0f, blueLightEnabled = true, blueLightIntensity = 0.5f)

        assertEquals(0.5f * OverlaySpec.MAX_TINT_ALPHA, spec.tintAlpha, TOLERANCE)
    }

    @Test
    fun `both layers are combined`() {
        val spec = OverlaySpec.of(dimLevel = 0.8f, blueLightEnabled = true, blueLightIntensity = 1f)

        assertEquals(0.8f, spec.dimAlpha, TOLERANCE)
        assertEquals(OverlaySpec.MAX_TINT_ALPHA, spec.tintAlpha, TOLERANCE)
    }

    @Test
    fun `values are clamped to the drawable range`() {
        val spec = OverlaySpec.of(dimLevel = 1.7f, blueLightEnabled = true, blueLightIntensity = 3f)

        assertEquals(1f, spec.dimAlpha, TOLERANCE)
        assertEquals(OverlaySpec.MAX_TINT_ALPHA, spec.tintAlpha, TOLERANCE)
    }

    @Test
    fun `the tint colour is a warm amber without blue`() {
        val blue = OverlaySpec.TINT_COLOR and 0xFF

        assertEquals(0, blue)
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
