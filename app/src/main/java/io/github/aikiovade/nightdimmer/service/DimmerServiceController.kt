package io.github.aikiovade.nightdimmer.service

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Starts and stops [DimmerService] without ever crashing the caller.
 *
 * Starting a foreground service from the background is restricted on Android 12+
 * and can throw [IllegalStateException] (concretely
 * `ForegroundServiceStartNotAllowedException`). The dimmer holds the
 * `SYSTEM_ALERT_WINDOW` permission, which is one of the documented exemptions,
 * but the permission can be revoked at any moment, so the result is still
 * reported to the caller instead of being thrown away.
 */
object DimmerServiceController {

    /** @return `true` when the service was started, `false` when Android refused. */
    fun start(context: Context): Boolean {
        if (!Settings.canDrawOverlays(context)) return false
        val intent = Intent(context, DimmerService::class.java)
        return try {
            ContextCompat.startForegroundService(context, intent)
            true
        } catch (exception: IllegalStateException) {
            false
        }
    }

    fun stop(context: Context) {
        runCatching { context.stopService(Intent(context, DimmerService::class.java)) }
    }
}
