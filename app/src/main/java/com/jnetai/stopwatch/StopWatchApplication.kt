package com.jnetai.stopwatch

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.jnetai.stopwatch.utils.ErrorLogger

/**
 * StopWatchApplication - Application class that initializes global state,
 * notification channels, and error handling on app startup.
 */
class StopWatchApplication : Application() {

    companion object {
        const val CHANNEL_ALARM_ID = "alarm_channel"
        const val CHANNEL_ALARM_NAME = "Alarm Notifications"
        const val CHANNEL_SERVICE_ID = "service_channel"
        const val CHANNEL_SERVICE_NAME = "Background Service"

        @Volatile
        private lateinit var instance: StopWatchApplication

        fun getInstance(): StopWatchApplication = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
        ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
            "Application started", null)
    }

    /**
     * Create notification channels for alarm and foreground service.
     * Required for Android 8.0+ (API 26+).
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Alarm notification channel - high importance
                val alarmChannel = NotificationChannel(
                    CHANNEL_ALARM_ID,
                    CHANNEL_ALARM_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alarm ringing notifications"
                    enableVibration(true)
                    setSound(null, null) // We handle sound ourselves
                    enableLights(true)
                }

                // Foreground service channel - low importance
                val serviceChannel = NotificationChannel(
                    CHANNEL_SERVICE_ID,
                    CHANNEL_SERVICE_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Background service running indicator"
                    setSound(null, null)
                    enableVibration(false)
                }

                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(alarmChannel)
                notificationManager.createNotificationChannel(serviceChannel)

            } catch (e: Exception) {
                ErrorLogger.log(ErrorLogger.Codes.ALM_NOTIFICATION_FAILED,
                    "Failed to create notification channels", e)
            }
        }
    }
}
