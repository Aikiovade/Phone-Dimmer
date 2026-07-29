package com.example

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

object DimmerController {
    private var prefs: SharedPreferences? = null

    val autoDimEnabled = MutableStateFlow(false)
    val autoDimLevel = MutableStateFlow(0.0f)
    
    val deepBlackThemeEnabled = MutableStateFlow(false)

    val isEnabled = MutableStateFlow(false)
    val dimLevel = MutableStateFlow(0.8f) // Default dimmer value (higher is darker)
    
    val blueLightEnabled = MutableStateFlow(false)
    val blueLightIntensity = MutableStateFlow(0.5f)
    
    val scheduleEnabled = MutableStateFlow(false)
    val scheduleStartHour = MutableStateFlow(22)
    val scheduleStartMinute = MutableStateFlow(0)
    val scheduleEndHour = MutableStateFlow(7)
    val scheduleEndMinute = MutableStateFlow(0)

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences("dimmer_prefs", Context.MODE_PRIVATE)
        
        deepBlackThemeEnabled.value = prefs?.getBoolean("deepBlackThemeEnabled", false) ?: false
        autoDimEnabled.value = prefs?.getBoolean("autoDimEnabled", false) ?: false
        isEnabled.value = prefs?.getBoolean("isEnabled", false) ?: false
        dimLevel.value = prefs?.getFloat("dimLevel", 0.8f) ?: 0.8f
        blueLightEnabled.value = prefs?.getBoolean("blueLightEnabled", false) ?: false
        blueLightIntensity.value = prefs?.getFloat("blueLightIntensity", 0.5f) ?: 0.5f
        scheduleEnabled.value = prefs?.getBoolean("scheduleEnabled", false) ?: false
        scheduleStartHour.value = prefs?.getInt("scheduleStartHour", 22) ?: 22
        scheduleStartMinute.value = prefs?.getInt("scheduleStartMinute", 0) ?: 0
        scheduleEndHour.value = prefs?.getInt("scheduleEndHour", 7) ?: 7
        scheduleEndMinute.value = prefs?.getInt("scheduleEndMinute", 0) ?: 0
    }

    fun toggleEnabled(enabled: Boolean, context: Context) {
        isEnabled.value = enabled
        prefs?.edit()?.putBoolean("isEnabled", enabled)?.apply()
        
        if (enabled) {
            val intent = Intent(context, DimmerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
    
    fun toggleAutoDim(enabled: Boolean) {
        autoDimEnabled.value = enabled
        prefs?.edit()?.putBoolean("autoDimEnabled", enabled)?.apply()
    }
    
    fun toggleDeepBlackTheme(enabled: Boolean) {
        deepBlackThemeEnabled.value = enabled
        prefs?.edit()?.putBoolean("deepBlackThemeEnabled", enabled)?.apply()
    }
    
    fun setDimLevel(level: Float) {
        dimLevel.value = level
        prefs?.edit()?.putFloat("dimLevel", level)?.apply()
    }

    fun toggleBlueLight(enabled: Boolean) {
        blueLightEnabled.value = enabled
        prefs?.edit()?.putBoolean("blueLightEnabled", enabled)?.apply()
    }

    fun setBlueLightIntensity(intensity: Float) {
        blueLightIntensity.value = intensity
        prefs?.edit()?.putFloat("blueLightIntensity", intensity)?.apply()
    }
    
    fun toggleSchedule(enabled: Boolean, context: Context) {
        scheduleEnabled.value = enabled
        prefs?.edit()?.putBoolean("scheduleEnabled", enabled)?.apply()
        ScheduleManager.schedule(context)
    }
    
    fun setScheduleStart(hour: Int, minute: Int, context: Context) {
        scheduleStartHour.value = hour
        scheduleStartMinute.value = minute
        prefs?.edit()?.putInt("scheduleStartHour", hour)?.putInt("scheduleStartMinute", minute)?.apply()
        ScheduleManager.schedule(context)
    }
    
    fun setScheduleEnd(hour: Int, minute: Int, context: Context) {
        scheduleEndHour.value = hour
        scheduleEndMinute.value = minute
        prefs?.edit()?.putInt("scheduleEndHour", hour)?.putInt("scheduleEndMinute", minute)?.apply()
        ScheduleManager.schedule(context)
    }
}

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DimmerController.init(context)
        val action = intent.action
        if (action == "com.example.ACTION_START_DIMMER") {
            DimmerController.toggleEnabled(true, context)
        } else if (action == "com.example.ACTION_STOP_DIMMER") {
            DimmerController.toggleEnabled(false, context)
        } else if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Just schedule, init already happened
        }
        ScheduleManager.schedule(context)
    }
}

object ScheduleManager {
    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val startIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = "com.example.ACTION_START_DIMMER"
        }
        val stopIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = "com.example.ACTION_STOP_DIMMER"
        }
        
        val startPending = PendingIntent.getBroadcast(context, 1, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopPending = PendingIntent.getBroadcast(context, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        
        alarmManager.cancel(startPending)
        alarmManager.cancel(stopPending)
        
        if (!DimmerController.scheduleEnabled.value) {
            return
        }
        
        val now = Calendar.getInstance()
        
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, DimmerController.scheduleStartHour.value)
            set(Calendar.MINUTE, DimmerController.scheduleStartMinute.value)
            set(Calendar.SECOND, 0)
        }
        if (startCal.before(now)) {
            startCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        val stopCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, DimmerController.scheduleEndHour.value)
            set(Calendar.MINUTE, DimmerController.scheduleEndMinute.value)
            set(Calendar.SECOND, 0)
        }
        if (stopCal.before(now)) {
            stopCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startCal.timeInMillis, startPending)
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, stopCal.timeInMillis, stopPending)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, startCal.timeInMillis, startPending)
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, stopCal.timeInMillis, stopPending)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, startCal.timeInMillis, startPending)
            alarmManager.set(AlarmManager.RTC_WAKEUP, stopCal.timeInMillis, stopPending)
        }
    }
}

class DimmerService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null
    private var sensorListener: SensorEventListener? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        
        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val lux = it.values[0]
                    val targetDim = when {
                        lux < 2f -> 0.8f
                        lux < 10f -> 0.6f
                        lux < 50f -> 0.4f
                        lux < 200f -> 0.2f
                        else -> 0.0f
                    }
                    DimmerController.autoDimLevel.value = targetDim
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        startForegroundService()
        
        overlayView = View(this).apply {
            setBackgroundColor(Color.BLACK)
            alpha = DimmerController.dimLevel.value
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        windowManager?.addView(overlayView, params)

        scope.launch {
            combine(
                DimmerController.dimLevel,
                DimmerController.autoDimEnabled,
                DimmerController.autoDimLevel,
                DimmerController.blueLightEnabled,
                DimmerController.blueLightIntensity
            ) { dim, autoEnabled, autoLevel, blueEnabled, blueIntensity ->
                val activeDim = if (autoEnabled) autoLevel else dim
                val finalAlpha = if (blueEnabled) maxOf(activeDim, blueIntensity) else activeDim
                val red = if (blueEnabled) (255 * blueIntensity).toInt() else 0
                val green = if (blueEnabled) (120 * blueIntensity).toInt() else 0
                
                overlayView?.setBackgroundColor(Color.argb((finalAlpha * 255).toInt(), red, green, 0))
                overlayView?.alpha = 1f
            }.collectLatest { }
        }
        
        scope.launch {
            DimmerController.autoDimEnabled.collectLatest { enabled ->
                if (enabled) {
                    lightSensor?.let {
                        sensorManager?.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
                    }
                } else {
                    sensorManager?.unregisterListener(sensorListener)
                }
            }
        }
        
        scope.launch {
            DimmerController.isEnabled.collectLatest { enabled ->
                if (!enabled) {
                    stopSelf()
                }
            }
        }
    }

    private fun startForegroundService() {
        val channelId = "dimmer_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Dimmer Service", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Screen Dimmer Active")
            .setContentText("Tap to adjust settings")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
            
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(sensorListener)
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
