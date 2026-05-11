package com.jnetai.stopwatch.service

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
import com.jnetai.stopwatch.MainActivity
import com.jnetai.stopwatch.R
import com.jnetai.stopwatch.StopWatchApplication
import com.jnetai.stopwatch.utils.ErrorLogger
import com.jnetai.stopwatch.utils.SettingsManager
import com.jnetai.stopwatch.utils.SoundUtils

/**
 * AlarmForegroundService - Runs as a foreground service to play alarm sound
 * and vibrate even when the app is in the background or the phone is locked.
 * Acquires a wake lock to ensure the alarm sounds reliably.
 */
class AlarmForegroundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val TAG = "AlarmService"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_ALARM = "com.jnetai.stopwatch.STOP_ALARM"
        const val EXTRA_SOUND_PATH = "sound_path"
        const val EXTRA_VOLUME = "volume"
        const val EXTRA_VIBRATE = "vibrate"

        /**
         * Start the alarm foreground service.
         */
        fun startAlarm(context: Context, soundPath: String, volume: Int, vibrate: Boolean) {
            val intent = Intent(context, AlarmForegroundService::class.java).apply {
                putExtra(EXTRA_SOUND_PATH, soundPath)
                putExtra(EXTRA_VOLUME, volume)
                putExtra(EXTRA_VIBRATE, vibrate)
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

        /**
         * Stop the alarm service from anywhere.
         */
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
            else -> {
                // Start the alarm
                val soundPath = intent?.getStringExtra(EXTRA_SOUND_PATH) ?: ""
                val volume = intent?.getIntExtra(EXTRA_VOLUME, 85) ?: 85
                val vibrate = intent?.getBooleanExtra(EXTRA_VIBRATE, true) ?: true

                startForeground(NOTIFICATION_ID, createAlarmNotification())

                // Acquire wake lock to ensure alarm plays
                acquireWakeLock()

                // Play sound
                playAlarmSound(soundPath, volume)

                // Vibrate if enabled
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

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("alarm_active", true)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 1, openIntent,
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
