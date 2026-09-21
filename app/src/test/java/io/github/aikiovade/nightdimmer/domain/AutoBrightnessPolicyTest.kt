package io.github.aikiovade.nightdimmer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AutoBrightnessPolicyTest {

    @Test
    fun `raw lux values map to the documented levels`() {
        val policy = AutoBrightnessPolicy()

        assertEquals(0.8f, policy.targetFor(0f), TOLERANCE)
        assertEquals(0.8f, policy.targetFor(1.9f), TOLERANCE)
        assertEquals(0.6f, policy.targetFor(2f), TOLERANCE)
        assertEquals(0.4f, policy.targetFor(49f), TOLERANCE)
        assertEquals(0.2f, policy.targetFor(199f), TOLERANCE)
        assertEquals(0f, policy.targetFor(200f), TOLERANCE)
        assertEquals(0f, policy.targetFor(10_000f), TOLERANCE)
    }

    @Test
    fun `the first measurement is applied without fading`() {
        val policy = AutoBrightnessPolicy()

        assertEquals(0.8f, policy.onLux(0.5f), TOLERANCE)
    }

    @Test
    fun `a real change fades in instead of jumping`() {
        val policy = AutoBrightnessPolicy(smoothingFactor = 0.5f)

        assertEquals(0.8f, policy.onLux(0.5f), TOLERANCE)
        assertEquals(0.4f, policy.onLux(500f), TOLERANCE)
        assertEquals(0.2f, policy.onLux(500f), TOLERANCE)
        assertEquals(0.1f, policy.onLux(500f), TOLERANCE)
    }

    @Test
    fun `hysteresis keeps the level stable around a boundary`() {
        val policy = AutoBrightnessPolicy(smoothingFactor = 1f, hysteresisRatio = 0.25f)

        assertEquals(0f, policy.onLux(250f), TOLERANCE)

        // 180 lx is below the 200 lx boundary but inside the hysteresis band.
        assertEquals(0f, policy.onLux(180f), TOLERANCE)

        // A clear drop finally darkens the screen.
        assertEquals(0.2f, policy.onLux(140f), TOLERANCE)

        // 240 lx is above the boundary again but must not flip back yet.
        assertEquals(0.2f, policy.onLux(240f), TOLERANCE)

        assertEquals(0f, policy.onLux(270f), TOLERANCE)
    }

    @Test
    fun `seeding fades from the previous manual level`() {
        val policy = AutoBrightnessPolicy(smoothingFactor = 0.5f)

        policy.seed(1f)

        assertEquals(0.5f, policy.onLux(1_000f), TOLERANCE)
    }

    @Test
    fun `reset forgets the previous measurement`() {
        val policy = AutoBrightnessPolicy(smoothingFactor = 1f)

        policy.onLux(1_000f)
        policy.reset()

        assertEquals(0.8f, policy.onLux(0.5f), TOLERANCE)
    }

    @Test
    fun `a level table of the wrong size is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            AutoBrightnessPolicy(
                thresholdsLux = floatArrayOf(1f, 2f),
                dimLevels = floatArrayOf(0.5f),
            )
        }
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
