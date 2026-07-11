package com.jnetai.stopwatch.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jnetai.stopwatch.R
import com.jnetai.stopwatch.service.AlarmForegroundService
import com.jnetai.stopwatch.utils.ErrorLogger
import com.jnetai.stopwatch.utils.SettingsManager
import com.jnetai.stopwatch.utils.SoundUtils
import java.io.File
import java.io.FileInputStream
import java.util.Calendar

class AlarmAlertActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var soundPath: String = ""
    private var volume: Int = 85
    private var vibrate: Boolean = true
    private var alarmHour: Int = 0
    private var alarmMinute: Int = 0
    private var alarmId: Int = 0

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            dismissAlarm()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_alert)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        handleIntent(intent)
        registerReceiver(stopReceiver, IntentFilter("com.jnetai.stopwatch.STOP_ALARM"),
            Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        stopAlarm()
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        soundPath = intent.getStringExtra("sound_path") ?: SoundUtils.getDefaultSoundPath(this)
        volume = intent.getIntExtra("volume", 85)
        vibrate = intent.getBooleanExtra("vibrate", true)
        alarmHour = intent.getIntExtra("alarm_hour", 0)
        alarmMinute = intent.getIntExtra("alarm_minute", 0)
        alarmId = intent.getIntExtra("alarm_id", 0)

        val timeStr = String.format("%02d:%02d", alarmHour, alarmMinute)
        findViewById<TextView>(R.id.tv_alert_label).text = "Alarm"
        findViewById<TextView>(R.id.tv_alert_time).text = timeStr

        findViewById<Button>(R.id.btn_dismiss).setOnClickListener { dismissAlarm() }
        findViewById<Button>(R.id.btn_snooze).setOnClickListener { snoozeAlarm() }

        acquireWakeLock()
        startVibration()
        playAlarmSound()
    }

    private fun playAlarmSound() {
        try {
            val soundFile = File(soundPath)
            if (soundFile.exists() && soundFile.isFile && soundFile.length() > 0) {
                val fis = FileInputStream(soundFile)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(fis.fd)
                    prepare()
                    fis.close()
                    val volumeFloat = (volume.toFloat() / 100f).coerceIn(0f, 1f)
                    setVolume(volumeFloat, volumeFloat)
                    isLooping = true
                    start()
                }
            } else {
                val defaultUri = android.media.RingtoneManager.getDefaultUri(
                    android.media.RingtoneManager.TYPE_ALARM
                )
                if (defaultUri != null) {
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        setDataSource(this@AlarmAlertActivity, defaultUri)
                        prepare()
                        val volumeFloat = (volume.toFloat() / 100f).coerceIn(0f, 1f)
                        setVolume(volumeFloat, volumeFloat)
                        isLooping = true
                        start()
                    }
                } else {
                    toneGenerator = ToneGenerator(
                        android.media.AudioManager.STREAM_ALARM,
                        (volume * 2.55).toInt().coerceIn(0, 100)
                    )
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 0)
                }
            }
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.ALM_SOUND_FAILED, "Failed to play alarm sound", e)
            try {
                toneGenerator = ToneGenerator(
                    android.media.AudioManager.STREAM_ALARM, 100
                )
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 0)
            } catch (_: Exception) {}
        }
    }

    private fun startVibration() {
        if (!vibrate) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 1000, 500), 1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 1000, 500), 1)
            }
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_PERMISSION, "Failed to start vibration", e)
        }
    }

    private fun stopVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.cancel()
        } catch (_: Exception) {}
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "StopWatch:AlarmWakeLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L)
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_SYSTEM_SERVICE, "Failed to acquire wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                try { it.release() } catch (_: Exception) {}
            }
        }
        wakeLock = null
    }

    private fun dismissAlarm() {
        stopAlarm()
        AlarmForegroundService.stopAlarm(this)
        rescheduleForNextDay()
        finish()
    }

    private fun snoozeAlarm() {
        stopAlarm()
        AlarmForegroundService.stopAlarm(this)
        val ctx = applicationContext
        val settings = SettingsManager.getInstance(ctx)
        val sp = settings.getAlarmSoundPath().ifEmpty { SoundUtils.getDefaultSoundPath(ctx) }
        val vol = settings.getAlarmVolume().coerceIn(0, 100).let { if (it == 0) 85 else it }
        val vib = settings.isVibrateEnabled()

        val intent = Intent(ctx, AlarmAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("sound_path", sp)
            putExtra("volume", vol)
            putExtra("vibrate", vib)
            putExtra("alarm_hour", alarmHour)
            putExtra("alarm_minute", alarmMinute)
            putExtra("alarm_id", alarmId)
        }
        val pi = PendingIntent.getActivity(
            ctx, alarmId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeTime = System.currentTimeMillis() + 5 * 60 * 1000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, snoozeTime, pi
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTime, pi)
        }
        finish()
    }

    private fun rescheduleForNextDay() {
        try {
            val ctx = applicationContext
            val settings = SettingsManager.getInstance(ctx)
            val sp = settings.getAlarmSoundPath().ifEmpty { SoundUtils.getDefaultSoundPath(ctx) }
            val vol = settings.getAlarmVolume().coerceIn(0, 100).let { if (it == 0) 85 else it }
            val vib = settings.isVibrateEnabled()

            val intent = Intent(ctx, AlarmAlertActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("sound_path", sp)
                putExtra("volume", vol)
                putExtra("vibrate", vib)
                putExtra("alarm_hour", alarmHour)
                putExtra("alarm_minute", alarmMinute)
                putExtra("alarm_id", alarmId)
            }
            val pi = PendingIntent.getActivity(
                ctx, alarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarmHour)
                set(Calendar.MINUTE, alarmMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.ALM_SCHEDULE_FAILED, "Failed to reschedule alarm", e)
        }
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
        } catch (_: Exception) {}
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
        stopVibration()
        releaseWakeLock()
    }

    override fun onBackPressed() {}

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(stopReceiver) } catch (_: Exception) {}
        stopAlarm()
    }
}
