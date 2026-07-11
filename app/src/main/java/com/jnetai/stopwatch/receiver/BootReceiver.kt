package com.jnetai.stopwatch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jnetai.stopwatch.utils.ErrorLogger
import com.jnetai.stopwatch.utils.SettingsManager
import org.json.JSONArray

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED, "Boot completed - rescheduling alarms")
            try {
                val settings = SettingsManager.getInstance(context)
                val json = settings.getAlarmsJson()
                if (json.isEmpty()) return
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (!obj.optBoolean("enabled", false)) continue
                    val id = obj.optInt("id", 0)
                    val hour = obj.optInt("hour", 8)
                    val minute = obj.optInt("minute", 0)
                    AlarmReceiver.scheduleAlarm(context, id, hour, minute)
                }
            } catch (e: Exception) {
                ErrorLogger.log(ErrorLogger.Codes.GEN_BOOT_FAILED, "Failed to reschedule alarms after boot", e)
            }
        }
    }
}
