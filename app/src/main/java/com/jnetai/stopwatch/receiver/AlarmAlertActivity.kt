package com.jnetai.stopwatch.receiver

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jnetai.stopwatch.R
import com.jnetai.stopwatch.service.AlarmForegroundService
import com.jnetai.stopwatch.utils.ErrorLogger
import com.jnetai.stopwatch.utils.SettingsManager
import com.jnetai.stopwatch.utils.SoundUtils

class AlarmAlertActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var alarmId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_alert)

        // Keep screen on and show when locked
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        alarmId = intent.getLongExtra("alarm_id", -1L)
        val alarmLabel = intent.getStringExtra("alarm_label") ?: "Alarm"

        findViewById<TextView>(R.id.tv_alert_label).text = alarmLabel
        findViewById<TextView>(R.id.tv_alert_time).text = java.text.SimpleDateFormat(
            "HH:mm", java.util.Locale.US
        ).format(java.util.Date())

        findViewById<Button>(R.id.btn_dismiss).setOnClickListener { dismissAlarm() }
        findViewById<Button>(R.id.btn_snooze).setOnClickListener { snoozeAlarm() }

        // Start playing sound
        playAlarmSound()
    }

    private fun playAlarmSound() {
        try {
            val settingsManager = SettingsManager.getInstance(this)
            val volume = settingsManager.getAlarmVolume()
            val soundPath = settingsManager.getAlarmSoundPath()
            val soundToPlay = if (soundPath.isNotEmpty()) soundPath
                else SoundUtils.getDefaultSoundPath(this)

            mediaPlayer = SoundUtils.createMediaPlayer(this, soundToPlay, volume)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()

            // Vibrate if enabled
            if (settingsManager.isVibrateEnabled()) {
                SoundUtils.startVibrate(this)
            }
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.SRV_ALARM_FAILED, "Failed to play alarm sound", e)
        }
    }

    private fun dismissAlarm() {
        stopAlarm()
        finish()
    }

    private fun snoozeAlarm() {
        stopAlarm()
        // Reschedule alarm in 5 minutes
        if (alarmId >= 0) {
            val intent = Intent(this, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarmId)
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                this, alarmId.toInt(), intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 5 * 60 * 1000L,
                pendingIntent
            )
        }
        finish()
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
            SoundUtils.stopVibrate(this)
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.SRV_ALARM_FAILED, "Error stopping alarm", e)
        }

        // Stop the foreground service
        stopService(Intent(this, AlarmForegroundService::class.java))
    }

    override fun onBackPressed() {
        // Don't allow back press to dismiss alarm
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}
