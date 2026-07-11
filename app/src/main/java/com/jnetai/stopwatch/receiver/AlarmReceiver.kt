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
import org.json.JSONArray
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ALARM_TRIGGERED = "com.jnetai.stopwatch.ALARM_TRIGGERED"
        const val EXTRA_HOUR = "alarm_hour"
        const val EXTRA_MINUTE = "alarm_minute"
        const val EXTRA_ALARM_ID = "alarm_id"

        fun scheduleAlarm(context: Context, id: Int, hour: Int, minute: Int) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val settings = SettingsManager.getInstance(context)
                val soundPath = settings.getAlarmSoundPath().ifEmpty {
                    SoundUtils.getDefaultSoundPath(context)
                }
                val volume = settings.getAlarmVolume().coerceIn(0, 100).let { if (it == 0) 85 else it }
                val vibrate = settings.isVibrateEnabled()

                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_ALARM_TRIGGERED
                    putExtra(EXTRA_HOUR, hour)
                    putExtra(EXTRA_MINUTE, minute)
                    putExtra(EXTRA_ALARM_ID, id)
                    putExtra("sound_path", soundPath)
                    putExtra("volume", volume)
                    putExtra("vibrate", vibrate)
                }
                val pi = PendingIntent.getBroadcast(
                    context, id, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi
                    )
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                }
            } catch (e: Exception) {
                ErrorLogger.log(ErrorLogger.Codes.ALM_SCHEDULE_FAILED, "Failed to schedule alarm $id", e)
            }
        }

        fun cancelAlarm(context: Context, id: Int) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_ALARM_TRIGGERED
                }
                val pi = PendingIntent.getBroadcast(
                    context, id, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pi)
                pi.cancel()
            } catch (e: Exception) {
                ErrorLogger.log(ErrorLogger.Codes.ALM_CANCEL_FAILED, "Failed to cancel alarm $id", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED, "AlarmReceiver: action=$action")

        if (action == ACTION_ALARM_TRIGGERED || action == null) {
            val soundPath = intent.getStringExtra("sound_path")
                ?: SoundUtils.getDefaultSoundPath(context)
            val volume = intent.getIntExtra("volume", 85)
            val vibrate = intent.getBooleanExtra("vibrate", true)
            val hour = intent.getIntExtra(EXTRA_HOUR, 0)
            val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
            val id = intent.getIntExtra(EXTRA_ALARM_ID, 0)

            AlarmForegroundService.startAlarm(context, soundPath, volume, vibrate)

            try {
                val alertIntent = Intent(context, AlarmAlertActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("sound_path", soundPath)
                    putExtra("volume", volume)
                    putExtra("vibrate", vibrate)
                    putExtra("alarm_hour", hour)
                    putExtra("alarm_minute", minute)
                    putExtra("alarm_id", id)
                }
                context.startActivity(alertIntent)
            } catch (e: Exception) {
                ErrorLogger.log(ErrorLogger.Codes.ALM_TRIGGER_FAILED,
                    "Failed to start AlarmAlertActivity, service is running", e)
            }

            val settings = SettingsManager.getInstance(context)
            val json = settings.getAlarmsJson()
            if (json.isNotEmpty()) {
                try {
                    val arr = JSONArray(json)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        if (obj.optInt("id", 0) == id && obj.optBoolean("enabled", false)) {
                            scheduleAlarm(context, id, hour, minute)
                            break
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
