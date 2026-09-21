package io.github.aikiovade.nightdimmer.ui

import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.aikiovade.nightdimmer.NightDimmerApp
import io.github.aikiovade.nightdimmer.data.SettingsRepository
import io.github.aikiovade.nightdimmer.domain.model.DimmerSettings
import io.github.aikiovade.nightdimmer.service.DimmerAlarmScheduler
import io.github.aikiovade.nightdimmer.service.DimmerRuntime
import io.github.aikiovade.nightdimmer.service.DimmerServiceController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Presentation state and the only place that decides when the service is
 * started or stopped.
 */
class DimmerViewModel(private val app: NightDimmerApp) : ViewModel() {

    private val repository: SettingsRepository = app.settingsRepository
    private val runtime: DimmerRuntime = app.dimmerRuntime
    private val alarmScheduler = DimmerAlarmScheduler(app, repository)

    val settings: StateFlow<DimmerSettings> = repository.settings
    val effectiveDimLevel: StateFlow<Float> = runtime.effectiveDimLevel
    val lightSensorAvailable: StateFlow<Boolean> = runtime.lightSensorAvailable

    private val _overlayPermissionGranted = MutableStateFlow(Settings.canDrawOverlays(app))
    val overlayPermissionGranted: StateFlow<Boolean> = _overlayPermissionGranted.asStateFlow()

    private val _exactAlarmsAllowed =
        MutableStateFlow(DimmerAlarmScheduler.canScheduleExactAlarms(app))
    val exactAlarmsAllowed: StateFlow<Boolean> = _exactAlarmsAllowed.asStateFlow()

    private val _startBlocked = MutableStateFlow(false)
    val startBlocked: StateFlow<Boolean> = _startBlocked.asStateFlow()

    /** Set when the user asked to enable the dimmer but the permission is missing. */
    private var pendingEnable = false

    /**
     * Called every time the screen comes back to the foreground: the user may
     * have granted the overlay permission in the system settings in the
     * meantime, and a pending "enable" request can now be completed.
     */
    fun onScreenResumed() {
        refreshPermissions()
        if (pendingEnable && _overlayPermissionGranted.value) {
            pendingEnable = false
            setDimmerEnabled(true)
        }
    }

    fun refreshPermissions() {
        _overlayPermissionGranted.value = Settings.canDrawOverlays(app)
        _exactAlarmsAllowed.value = DimmerAlarmScheduler.canScheduleExactAlarms(app)
    }

    fun setDimmerEnabled(enabled: Boolean) {
        if (!enabled) {
            pendingEnable = false
            _startBlocked.value = false
            repository.update { it.copy(isEnabled = false) }
            DimmerServiceController.stop(app)
            return
        }

        if (!_overlayPermissionGranted.value) {
            // Remember the request, the caller opens the system settings next.
            pendingEnable = true
            return
        }

        _startBlocked.value = false
        repository.update { it.copy(isEnabled = true) }
        if (!DimmerServiceController.start(app)) {
            repository.update { it.copy(isEnabled = false) }
            _startBlocked.value = true
        }
    }

    fun setDimLevel(level: Float, persist: Boolean) {
        repository.update(persist = persist) { it.copy(dimLevel = level) }
    }

    fun persistPendingChanges() {
        repository.persist()
    }

    fun setAutoDimEnabled(enabled: Boolean) {
        repository.update { it.copy(autoDimEnabled = enabled) }
    }

    fun setBlueLightEnabled(enabled: Boolean) {
        repository.update { it.copy(blueLightEnabled = enabled) }
    }

    fun setBlueLightIntensity(intensity: Float, persist: Boolean) {
        repository.update(persist = persist) { it.copy(blueLightIntensity = intensity) }
    }

    fun setDeepBlackThemeEnabled(enabled: Boolean) {
        repository.update { it.copy(deepBlackThemeEnabled = enabled) }
    }

    fun setScheduleEnabled(enabled: Boolean) {
        repository.update { it.copy(scheduleEnabled = enabled) }
        reschedule()
    }

    fun setSchedule(startMinutes: Int, endMinutes: Int) {
        repository.update {
            it.copy(scheduleStartMinutes = startMinutes, scheduleEndMinutes = endMinutes)
        }
        reschedule()
    }

    private fun reschedule() {
        viewModelScope.launch { alarmScheduler.reschedule() }
    }

    companion object {
        fun factory(app: NightDimmerApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DimmerViewModel(app) as T
            }
    }
}
