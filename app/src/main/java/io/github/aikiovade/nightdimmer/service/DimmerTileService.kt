package io.github.aikiovade.nightdimmer.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.aikiovade.nightdimmer.MainActivity
import io.github.aikiovade.nightdimmer.NightDimmerApp
import io.github.aikiovade.nightdimmer.R

/** Quick settings tile that mirrors the main switch. */
class DimmerTileService : TileService() {

    private val repository
        get() = (application as NightDimmerApp).settingsRepository

    override fun onStartListening() {
        super.onStartListening()
        syncTile()
    }

    override fun onClick() {
        super.onClick()

        val isEnabled = repository.snapshot().isEnabled
        if (!isEnabled && !Settings.canDrawOverlays(this)) {
            openApp()
            return
        }

        repository.update { it.copy(isEnabled = !isEnabled) }
        if (isEnabled) {
            DimmerServiceController.stop(this)
        } else if (!DimmerServiceController.start(this)) {
            // Android refused to start the service, so the tile must not claim
            // that the dimmer is running.
            repository.update { it.copy(isEnabled = false) }
        }
        syncTile()
    }

    private fun syncTile() {
        val tile = qsTile ?: return
        tile.state = if (repository.snapshot().isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.updateTile()
    }

    /**
     * The `Intent` overload is the only one available before API 34, where it
     * is deprecated in favour of the `PendingIntent` variant.
     */
    @Suppress("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
