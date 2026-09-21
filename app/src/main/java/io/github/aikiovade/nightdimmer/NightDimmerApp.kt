package io.github.aikiovade.nightdimmer

import android.app.Application
import io.github.aikiovade.nightdimmer.data.SettingsRepository
import io.github.aikiovade.nightdimmer.data.SharedPreferencesKeyValueStore
import io.github.aikiovade.nightdimmer.service.DimmerNotifications
import io.github.aikiovade.nightdimmer.service.DimmerRuntime

/**
 * Tiny hand-written object graph.
 *
 * The app is far too small for a dependency injection framework, but the
 * process-scoped singletons still need one owner. Everything here is created
 * lazily with the application context, so nothing can leak an Activity.
 */
class NightDimmerApp : Application() {

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(SharedPreferencesKeyValueStore(this))
    }

    val dimmerRuntime: DimmerRuntime by lazy { DimmerRuntime() }

    override fun onCreate() {
        super.onCreate()
        DimmerNotifications.ensureChannel(this)
    }
}
