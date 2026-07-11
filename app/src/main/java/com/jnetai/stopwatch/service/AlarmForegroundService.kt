package com.jnetai.stopwatch.service

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.jnetai.stopwatch.R
import com.jnetai.stopwatch.StopWatchApplication
import com.jnetai.stopwatch.receiver.AlarmAlertActivity
import com.jnetai.stopwatch.utils.ErrorLogger
import com.jnetai.stopwatch.utils.SettingsManager
import com.jnetai.stopwatch.utils.SoundUtils

class AlarmForegroundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var alarmId: Int = 0
    private var alarmHour: Int = 0
    private var alarmMinute: Int = 0

    companion object {
        const val TAG = "AlarmService"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_ALARM = "com.jnetai.stopwatch.STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.jnetai.stopwatch.SNOOZE_ALARM"
        const val EXTRA_SOUND_PATH = "sound_path"
        const val EXTRA_VOLUME = "volume"
        const val EXTRA_VIBRATE = "vibrate"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_HOUR = "alarm_hour"
        const val EXTRA_ALARM_MINUTE = "alarm_minute"

        fun startAlarm(context: Context, soundPath: String, volume: Int, vibrate: Boolean,
                       id: Int = 0, hour: Int = 0, minute: Int = 0) {
            val intent = Intent(context, AlarmForegroundService::class.java).apply {
                putExtra(EXTRA_SOUND_PATH, soundPath)
                putExtra(EXTRA_VOLUME, volume)
                putExtra(EXTRA_VIBRATE, vibrate)
                putExtra(EXTRA_ALARM_ID, id)
                putExtra(EXTRA_ALARM_HOUR, hour)
                putExtra(EXTRA_ALARM_MINUTE, minute)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    @Suppress("DEPRECATION")
                    context.startService(intent)
                }
            } catch (e: Exception) {
                ErrorLogger.log(ErrorLogger.Codes.ALM_SERVICE_FAILED,
                    "Failed to start AlarmForegroundService", e)
            }
        }

        fun stopAlarm(context: Context) {
            val intent = Intent(context, AlarmForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
            "AlarmForegroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ALARM -> {
                stopAlarmService()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE_ALARM -> {
                snoozeAlarm()
                return START_NOT_STICKY
            }
            else -> {
                val soundPath = intent?.getStringExtra(EXTRA_SOUND_PATH) ?: ""
                val volume = intent?.getIntExtra(EXTRA_VOLUME, 85) ?: 85
                val vibrate = intent?.getBooleanExtra(EXTRA_VIBRATE, true) ?: true
                alarmId = intent?.getIntExtra(EXTRA_ALARM_ID, 0) ?: 0
                alarmHour = intent?.getIntExtra(EXTRA_ALARM_HOUR, 0) ?: 0
                alarmMinute = intent?.getIntExtra(EXTRA_ALARM_MINUTE, 0) ?: 0

                startForeground(NOTIFICATION_ID, createAlarmNotification())

                acquireWakeLock()

                playAlarmSound(soundPath, volume)

                if (vibrate) {
                    SoundUtils.startVibrate(this)
                }

                return START_STICKY
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        cleanupResources()
        super.onDestroy()
    }

    private fun createAlarmNotification(): Notification {
        val stopIntent = Intent(this, AlarmForegroundService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmForegroundService::class.java).apply {
            action = ACTION_SNOOZE_ALARM
        }
        val snoozePendingIntent = PendingIntent.getService(
            this, 1, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, AlarmAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("sound_path", "")
            putExtra("volume", 85)
            putExtra("vibrate", true)
            putExtra("alarm_hour", alarmHour)
            putExtra("alarm_minute", alarmMinute)
            putExtra("alarm_id", alarmId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 2, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, StopWatchApplication.CHANNEL_ALARM_ID)
            .setContentTitle("Alarm")
            .setContentText("Alarm is ringing")
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(openPendingIntent, true)
            .addAction(R.drawable.ic_stop, "Snooze 5 min", snoozePendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .build()
    }

    private fun playAlarmSound(soundPath: String, volume: Int) {
        mediaPlayer = SoundUtils.createMediaPlayer(this, soundPath, volume)
        mediaPlayer?.let { mp ->
            try {
                mp.start()
                ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                    "Alarm sound started: $soundPath at volume $volume%")
            } catch (e: Exception) {
                ErrorLogger.log(ErrorLogger.Codes.ALM_SOUND_FAILED,
                    "Failed to start MediaPlayer playback", e)
            }
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "StopWatch:AlarmWakeLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L) // 10 minute max
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_SYSTEM_SERVICE,
                "Failed to acquire wake lock", e)
        }
    }

    private fun stopAlarmService() {
        SoundUtils.stopVibrate(this)
        SoundUtils.releaseMediaPlayer(mediaPlayer)
        mediaPlayer = null
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun snoozeAlarm() {
        SoundUtils.stopVibrate(this)
        SoundUtils.releaseMediaPlayer(mediaPlayer)
        mediaPlayer = null
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)

        val settings = SettingsManager.getInstance(this)
        val sp = settings.getAlarmSoundPath().ifEmpty { SoundUtils.getDefaultSoundPath(this) }
        val vol = settings.getAlarmVolume().coerceIn(0, 100).let { if (it == 0) 85 else it }
        val vib = settings.isVibrateEnabled()

        val intent = Intent(this, AlarmAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("sound_path", sp)
            putExtra("volume", vol)
            putExtra("vibrate", vib)
            putExtra("alarm_hour", alarmHour)
            putExtra("alarm_minute", alarmMinute)
            putExtra("alarm_id", alarmId)
        }
        val pi = PendingIntent.getActivity(
            this, alarmId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeTime = System.currentTimeMillis() + 5 * 60 * 1000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, snoozeTime, pi
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTime, pi)
        }
        stopSelf()
    }

    private fun cleanupResources() {
        SoundUtils.stopVibrate(this)
        SoundUtils.releaseMediaPlayer(mediaPlayer)
        mediaPlayer = null
        releaseWakeLock()
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                try {
                    it.release()
                } catch (e: Exception) {
                    ErrorLogger.log(ErrorLogger.Codes.GEN_SYSTEM_SERVICE,
                        "Failed to release wake lock", e)
                }
            }
        }
        wakeLock = null
    }
}
