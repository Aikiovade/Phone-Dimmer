package io.github.aikiovade.nightdimmer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.aikiovade.nightdimmer.MainActivity
import io.github.aikiovade.nightdimmer.R

/** Notification channels and builders used by the dimmer. */
object DimmerNotifications {

    const val CHANNEL_ID = "dimmer"

    private const val NOTIFICATION_ID_ONGOING = 1
    private const val NOTIFICATION_ID_ACTION_REQUIRED = 2

    private const val REQUEST_CODE_CONTENT = 10
    private const val REQUEST_CODE_STOP = 11

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    /** The persistent notification that keeps the foreground service alive. */
    fun buildOngoing(context: Context): Notification {
        val stopIntent = PendingIntent.getService(
            context,
            REQUEST_CODE_STOP,
            Intent(context, DimmerService::class.java).setAction(DimmerService.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_dimmer)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text))
            .setContentIntent(contentIntent(context))
            .addAction(0, context.getString(R.string.notification_action_stop), stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * Tells the user that something needs their attention, for example a blocked
     * background start or a revoked overlay permission. Silently ignored when the
     * user did not grant the notification permission.
     */
    fun notifyActionRequired(context: Context, message: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_dimmer)
            .setContentTitle(context.getString(R.string.notification_action_required_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()

        try {
            manager.notify(NOTIFICATION_ID_ACTION_REQUIRED, notification)
        } catch (exception: SecurityException) {
            // Notification permission was revoked between the check and the call.
        }
    }

    private fun contentIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE_CONTENT,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
