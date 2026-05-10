package com.jnetai.stopwatch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jnetai.stopwatch.utils.ErrorLogger
import com.jnetai.stopwatch.utils.SettingsManager

/**
 * BootReceiver - Reschedules alarms after device reboot to ensure
 * the alarm clock continues working even after the phone is restarted.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                "Boot completed - rescheduling alarms")

            try {
                val settings = SettingsManager.getInstance(context)

                // Check if alarm was enabled before reboot
                if (settings.isAlarmEnabled()) {
                    val hour = settings.getAlarmHour()
                    val minute = settings.getAlarmMinute()

                    ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                        "Rescheduling alarm for %02d:%02d after reboot",
                        hour, minute)

                    AlarmReceiver.scheduleAlarm(context, hour, minute)
                } else {
                    ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                        "No active alarm to reschedule after reboot")
                }
            } catch (e: Exception) {
                ErrorLogger.log(ErrorLogger.Codes.GEN_BOOT_FAILED,
                    "Failed to reschedule alarms after boot", e)
            }
        }
    }
}
