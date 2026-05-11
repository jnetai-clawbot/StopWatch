package com.jnetai.stopwatch

import android.app.Application
import com.jnetai.stopwatch.utils.SoundUtils

class StopWatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
        SoundUtils.copyDefaultSounds(this)
    }

    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val alarmChannel = android.app.NotificationChannel(
                CHANNEL_ALARM_ID,
                "Alarm Notifications",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for alarm ringing"
            }
            val serviceChannel = android.app.NotificationChannel(
                CHANNEL_SERVICE_ID,
                "Background Service",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Indicates the app is running in background"
            }
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager?.createNotificationChannel(alarmChannel)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_ALARM_ID = "alarm_channel"
        const val CHANNEL_SERVICE_ID = "service_channel"
        lateinit var instance: StopWatchApplication
            private set
    }
}
