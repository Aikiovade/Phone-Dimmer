package com.example

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DimmerTileService : TileService() {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var job: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        DimmerController.init(this)
        job = scope.launch {
            DimmerController.isEnabled.collectLatest { isEnabled ->
                updateTileState(isEnabled)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        job?.cancel()
    }

    override fun onClick() {
        super.onClick()
        val isEnabled = DimmerController.isEnabled.value
        
        if (!isEnabled && !Settings.canDrawOverlays(this)) {
            // Need permission, launch app
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // For Android 14+, startActivityAndCollapse requires PendingIntent
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }

        DimmerController.toggleEnabled(!isEnabled, this)
        updateTileState(!isEnabled)
    }

    private fun updateTileState(isEnabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Dimmer"
        tile.updateTile()
    }
}
