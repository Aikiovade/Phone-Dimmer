package io.github.aikiovade.nightdimmer.data

import io.github.aikiovade.nightdimmer.domain.model.DimmerSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryTest {

    private val store = InMemoryKeyValueStore()
    private val repository = SettingsRepository(store)

    @Test
    fun `defaults match the documented values`() {
        assertEquals(DimmerSettings(), repository.snapshot())
    }

    @Test
    fun `persisted updates survive a restart`() {
        repository.update { it.copy(dimLevel = 0.35f, blueLightEnabled = true) }

        val reloaded = SettingsRepository(store).snapshot()

        assertEquals(0.35f, reloaded.dimLevel, TOLERANCE)
        assertEquals(true, reloaded.blueLightEnabled)
    }

    @Test
    fun `in-memory updates are only written when asked`() {
        repository.update(persist = false) { it.copy(dimLevel = 0.25f) }

        assertEquals(0.25f, repository.snapshot().dimLevel, TOLERANCE)
        assertEquals(
            DimmerSettings.DEFAULT_DIM_LEVEL,
            SettingsRepository(store).snapshot().dimLevel,
            TOLERANCE,
        )

        repository.persist()

        assertEquals(0.25f, SettingsRepository(store).snapshot().dimLevel, TOLERANCE)
    }

    @Test
    fun `out of range values are clamped on write`() {
        repository.update {
            it.copy(
                dimLevel = 1.8f,
                blueLightIntensity = -0.5f,
                scheduleStartMinutes = 5000,
                scheduleEndMinutes = -10,
            )
        }

        val settings = repository.snapshot()
        assertEquals(1f, settings.dimLevel, TOLERANCE)
        assertEquals(0f, settings.blueLightIntensity, TOLERANCE)
        assertEquals(DimmerSettings.MINUTES_PER_DAY - 1, settings.scheduleStartMinutes)
        assertEquals(0, settings.scheduleEndMinutes)
    }

    @Test
    fun `corrupted stored values are clamped on read`() {
        // Raw preference keys on purpose: this guards the on-disk contract.
        val corrupted = InMemoryKeyValueStore(
            mapOf(
                "dimLevel" to 12f,
                "scheduleStartMinutes" to -42,
            ),
        )

        val settings = SettingsRepository(corrupted).snapshot()

        assertEquals(1f, settings.dimLevel, TOLERANCE)
        assertEquals(0, settings.scheduleStartMinutes)
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
