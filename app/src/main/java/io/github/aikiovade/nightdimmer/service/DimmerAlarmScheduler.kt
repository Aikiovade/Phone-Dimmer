package io.github.aikiovade.nightdimmer.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.aikiovade.nightdimmer.data.SettingsRepository
import io.github.aikiovade.nightdimmer.domain.ScheduleAction
import io.github.aikiovade.nightdimmer.domain.ScheduleCalculator
import io.github.aikiovade.nightdimmer.domain.model.ScheduleWindow
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Keeps exactly one alarm armed: the next transition of the configured window.
 *
 * Whenever it fires, [DimmerAlarmReceiver] calls [reschedule] again, so the
 * dimmer always has a single "next action" instead of two independent start and
 * stop alarms that used to get out of sync.
 */
class DimmerAlarmScheduler(
    context: Context,
    private val repository: SettingsRepository,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
) {
    private val appContext = context.applicationContext

    fun reschedule() {
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(enablePendingIntent())
        alarmManager.cancel(disablePendingIntent())

        val settings = repository.snapshot()
        if (!settings.scheduleEnabled) return

        val window = ScheduleWindow(settings.scheduleStartMinutes, settings.scheduleEndMinutes)
        val action = nextMeaningfulAction(window, settings.isEnabled, now()) ?: return
        setAlarm(alarmManager, action)
    }

    private fun nextMeaningfulAction(
        window: ScheduleWindow,
        isEnabled: Boolean,
        from: LocalDateTime,
    ): ScheduleAction? {
        var action = ScheduleCalculator.nextAction(window, from)
        var inspected = 0
        while (action != null && inspected < MAX_INSPECTED_ACTIONS) {
            val isNoOp = when (action) {
                is ScheduleAction.Enable -> isEnabled
                is ScheduleAction.Disable -> !isEnabled
            }
            if (!isNoOp) return action
            action = ScheduleCalculator.nextAction(window, action.at)
            inspected++
        }
        return null
    }

    private fun setAlarm(alarmManager: AlarmManager, action: ScheduleAction) {
        val triggerAtMillis = action.at
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val pendingIntent = when (action) {
            is ScheduleAction.Enable -> enablePendingIntent()
            is ScheduleAction.Disable -> disablePendingIntent()
        }

        if (canScheduleExactAlarms(appContext)) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
                return
            } catch (exception: SecurityException) {
                // Permission was revoked in the meantime; fall through to an inexact alarm.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    private fun enablePendingIntent(): PendingIntent = pendingIntent(
        action = ACTION_ENABLE_DIMMER,
        requestCode = REQUEST_CODE_ENABLE,
    )

    private fun disablePendingIntent(): PendingIntent = pendingIntent(
        action = ACTION_DISABLE_DIMMER,
        requestCode = REQUEST_CODE_DISABLE,
    )

    private fun pendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(appContext, DimmerAlarmReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_ENABLE_DIMMER = "io.github.aikiovade.nightdimmer.action.ENABLE_DIMMER"
        const val ACTION_DISABLE_DIMMER = "io.github.aikiovade.nightdimmer.action.DISABLE_DIMMER"

        private const val REQUEST_CODE_ENABLE = 1
        private const val REQUEST_CODE_DISABLE = 2
        private const val MAX_INSPECTED_ACTIONS = 3

        /** `false` when the user has to allow exact alarms in the system settings. */
        fun canScheduleExactAlarms(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
            return alarmManager.canScheduleExactAlarms()
        }
    }
}
