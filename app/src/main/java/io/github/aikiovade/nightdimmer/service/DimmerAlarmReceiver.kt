package io.github.aikiovade.nightdimmer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.aikiovade.nightdimmer.NightDimmerApp
import io.github.aikiovade.nightdimmer.R
import io.github.aikiovade.nightdimmer.data.SettingsRepository
import io.github.aikiovade.nightdimmer.domain.model.ScheduleWindow
import java.time.LocalDateTime

/**
 * Handles the two schedule alarms and device reboots.
 *
 * The receiver is declared `android:exported="false"`, so no other app can flip
 * the dimmer by sending a broadcast, and every intent it handles is explicit or
 * a protected system broadcast.
 */
class DimmerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? NightDimmerApp ?: return
        val repository = app.settingsRepository

        when (intent.action) {
            DimmerAlarmScheduler.ACTION_ENABLE_DIMMER -> enable(context, repository)
            DimmerAlarmScheduler.ACTION_DISABLE_DIMMER -> disable(context, repository)
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> restore(context, repository)
        }

        DimmerAlarmScheduler(context, repository).reschedule()
    }

    private fun enable(context: Context, repository: SettingsRepository) {
        repository.update { it.copy(isEnabled = true) }
        if (!DimmerServiceController.start(context)) {
            repository.update { it.copy(isEnabled = false) }
            DimmerNotifications.notifyActionRequired(
                context,
                context.getString(R.string.notification_start_blocked),
            )
        }
    }

    private fun disable(context: Context, repository: SettingsRepository) {
        repository.update { it.copy(isEnabled = false) }
        DimmerServiceController.stop(context)
    }

    /**
     * Restores the dimmer after a reboot.
     *
     * The first version persisted `isEnabled` but never brought the overlay
     * back, so the UI claimed the dimmer was active while the screen stayed
     * bright. Here the service is started again, and when the reboot happens
     * inside an enabled schedule window the dimmer is turned on as well.
     */
    private fun restore(context: Context, repository: SettingsRepository) {
        val settings = repository.snapshot()
        val window = ScheduleWindow(settings.scheduleStartMinutes, settings.scheduleEndMinutes)
        val minuteOfDay = LocalDateTime.now().let { it.hour * 60 + it.minute }
        val insideWindow = settings.scheduleEnabled && window.contains(minuteOfDay)

        when {
            insideWindow -> enable(context, repository)
            settings.isEnabled -> if (!DimmerServiceController.start(context)) {
                repository.update { it.copy(isEnabled = false) }
            }
        }
    }
}
