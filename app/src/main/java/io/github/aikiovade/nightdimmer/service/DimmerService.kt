package io.github.aikiovade.nightdimmer.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import io.github.aikiovade.nightdimmer.NightDimmerApp
import io.github.aikiovade.nightdimmer.R
import io.github.aikiovade.nightdimmer.data.SettingsRepository
import io.github.aikiovade.nightdimmer.domain.AutoBrightnessPolicy
import io.github.aikiovade.nightdimmer.domain.OverlaySpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the two overlay windows (dim + warm filter) and
 * keeps them in sync with [SettingsRepository] and [DimmerRuntime].
 */
class DimmerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val policy = AutoBrightnessPolicy()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var runtime: DimmerRuntime

    private var windowManager: WindowManager? = null
    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null
    private var sensorListener: SensorEventListener? = null

    private var dimView: View? = null
    private var tintView: View? = null

    override fun onCreate() {
        super.onCreate()

        val app = application as NightDimmerApp
        settingsRepository = app.settingsRepository
        runtime = app.dimmerRuntime

        windowManager = getSystemService(WindowManager::class.java)
        sensorManager = getSystemService(SensorManager::class.java)
        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        runtime.setLightSensorAvailable(lightSensor != null)

        startForeground(NOTIFICATION_ID, DimmerNotifications.buildOngoing(this))

        if (!attachOverlay()) {
            reportOverlayFailure()
            return
        }

        runtime.setEffectiveDimLevel(settingsRepository.snapshot().dimLevel)
        observeOverlay()
        observeManualLevel()
        observeEnabledState()
        observeSensor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            settingsRepository.update { it.copy(isEnabled = false) }
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        sensorManager?.unregisterListener(sensorListener)
        sensorListener = null
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** @return `false` when the overlay could not be added, e.g. permission revoked. */
    private fun attachOverlay(): Boolean {
        val manager = windowManager ?: return false
        return try {
            val dimOverlay = View(this)
            val tintOverlay = View(this)

            manager.addView(dimOverlay, overlayParams())
            manager.addView(tintOverlay, overlayParams())

            dimView = dimOverlay
            tintView = tintOverlay
            render(OverlaySpec(dimAlpha = 0f, tintAlpha = 0f))
            true
        } catch (exception: RuntimeException) {
            // SecurityException when the overlay permission was revoked, or
            // BadTokenException while the display is being reconfigured.
            removeOverlay()
            false
        }
    }

    private fun overlayParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        dimAmount = 0f
    }

    private fun removeOverlay() {
        val manager = windowManager
        dimView?.let { view -> runCatching { manager?.removeView(view) } }
        tintView?.let { view -> runCatching { manager?.removeView(view) } }
        dimView = null
        tintView = null
    }

    private fun reportOverlayFailure() {
        settingsRepository.update { it.copy(isEnabled = false) }
        DimmerNotifications.notifyActionRequired(
            this,
            getString(R.string.notification_overlay_failed),
        )
        stopSelf()
    }

    private fun observeOverlay() {
        scope.launch {
            combine(
                settingsRepository.settings,
                runtime.effectiveDimLevel,
            ) { settings, dimLevel ->
                OverlaySpec.of(
                    dimLevel = dimLevel,
                    blueLightEnabled = settings.blueLightEnabled,
                    blueLightIntensity = settings.blueLightIntensity,
                )
            }.collect(::render)
        }
    }

    private fun render(spec: OverlaySpec) {
        dimView?.setBackgroundColor(withAlpha(spec.dimAlpha, Color.BLACK))
        tintView?.setBackgroundColor(withAlpha(spec.tintAlpha, OverlaySpec.TINT_COLOR))
    }

    private fun withAlpha(alpha: Float, color: Int): Int =
        ((alpha.coerceIn(0f, 1f) * MAX_ALPHA).toInt() shl 24) or (color and RGB_MASK)

    private fun observeManualLevel() {
        scope.launch {
            settingsRepository.settings
                .distinctUntilChangedBy { it.dimLevel to it.autoDimEnabled }
                .collect { settings ->
                    if (!settings.autoDimEnabled) {
                        policy.reset()
                        runtime.setEffectiveDimLevel(settings.dimLevel)
                    }
                }
        }
    }

    private fun observeEnabledState() {
        scope.launch {
            settingsRepository.settings
                .map { it.isEnabled }
                .distinctUntilChanged()
                .collect { isEnabled -> if (!isEnabled) stopSelf() }
        }
    }

    private fun observeSensor() {
        val manager = sensorManager ?: return
        val sensor = lightSensor ?: return

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val lux = event?.values?.firstOrNull() ?: return
                if (settingsRepository.snapshot().autoDimEnabled) {
                    runtime.setEffectiveDimLevel(policy.onLux(lux))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorListener = listener

        scope.launch {
            settingsRepository.settings
                .map { it.autoDimEnabled }
                .distinctUntilChanged()
                .collect { autoDimEnabled ->
                    if (autoDimEnabled) {
                        policy.seed(runtime.effectiveDimLevel.value)
                        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                    } else {
                        manager.unregisterListener(listener)
                        policy.reset()
                    }
                }
        }
    }

    companion object {
        const val ACTION_STOP = "io.github.aikiovade.nightdimmer.action.STOP_SERVICE"

        private const val NOTIFICATION_ID = 1
        private const val MAX_ALPHA = 255
        private const val RGB_MASK = 0x00FFFFFF
    }
}
