package com.jnetai.stopwatch.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jnetai.stopwatch.service.AlarmForegroundService
import com.jnetai.stopwatch.utils.ErrorLogger
import com.jnetai.stopwatch.utils.SettingsManager
import com.jnetai.stopwatch.utils.SoundUtils

/**
 * AlarmReceiver - Receives system alarm broadcasts and starts the
 * AlarmForegroundService to play the alarm sound.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "AlarmReceiver"
        const val ACTION_ALARM_TRIGGERED = "com.jnetai.stopwatch.ALARM_TRIGGERED"
        const val REQUEST_CODE_ALARM = 2001
        const val EXTRA_HOUR = "alarm_hour"
        const val EXTRA_MINUTE = "alarm_minute"

        /**
         * Schedule the alarm using AlarmManager.
         */
        fun scheduleAlarm(context: Context, hour: Int, minute: Int) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_ALARM_TRIGGERED
                    putExtra(EXTRA_HOUR, hour)
                    putExtra(EXTRA_MINUTE, minute)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_ALARM,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Calculate trigger time
                val calendar = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, minute)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)

                    // If the time is already past today, schedule for tomorrow
                    if (before(java.util.Calendar.getInstance())) {
                        add(java.util.Calendar.DAY_OF_YEAR, 1)
                    }
                }

                val triggerTime = calendar.timeInMillis

                // Schedule the alarm
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }

                ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                    "Alarm scheduled for %02d:%02d (trigger: %s)",
                    hour, minute, java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                        java.util.Locale.US).format(java.util.Date(triggerTime)))

            } catch (e: Exception) {
                ErrorLogger.log(ErrorLogger.Codes.ALM_SCHEDULE_FAILED,
                    "Failed to schedule alarm for %02d:%02d".format(hour, minute), e)
            }
        }

        /**
         * Cancel the scheduled alarm.
         */
        fun cancelAlarm(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_ALARM_TRIGGERED
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_ALARM,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                    "Alarm cancelled")
            } catch (e: Exception) {
                ErrorLogger.log(ErrorLogger.Codes.ALM_CANCEL_FAILED,
                    "Failed to cancel alarm", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
            "AlarmReceiver triggered with action: ${intent.action}")

        if (intent.action == ACTION_ALARM_TRIGGERED || intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Get settings
            val settings = SettingsManager.getInstance(context)
            val soundPath = settings.getAlarmSoundPath().ifEmpty {
                SoundUtils.getDefaultSoundPath(context)
            }
            val volume = settings.getAlarmVolume().coerceIn(0, 100).let {
                if (it == 0) 85 else it
            }
            val vibrate = settings.isVibrateEnabled()
            val silentMode = settings.isSilentMode()

            // Start foreground service to play alarm (skip sound if silent mode)
            if (silentMode) {
                // Vibrate only in silent mode
                if (vibrate) {
                    SoundUtils.startVibrate(context)
                }
            } else {
                AlarmForegroundService.startAlarm(context, soundPath, volume, vibrate)
            }

            // Re-schedule for next day if it was a scheduled alarm
            if (settings.isAlarmEnabled()) {
                val hour = intent.getIntExtra(EXTRA_HOUR, settings.getAlarmHour())
                val minute = intent.getIntExtra(EXTRA_MINUTE, settings.getAlarmMinute())
                scheduleAlarm(context, hour, minute)
            }
        }
    }
}
