package com.jnetai.stopwatch.utils

import android.content.Context
import android.content.SharedPreferences

class SettingsManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    object Keys {
        const val ALARM_VOLUME = "alarm_volume"
        const val ALARM_SOUND_PATH = "alarm_sound_path"
        const val ALARM_SOUND_NAME = "alarm_sound_name"
        const val VIBRATE_ENABLED = "vibrate_enabled"
        const val ALARM_HOUR = "alarm_hour"
        const val ALARM_MINUTE = "alarm_minute"
        const val ALARM_ENABLED = "alarm_enabled"
        const val THEME_DARK = "theme_dark"
        const val BACKGROUND_SERVICE = "background_service"
        const val SILENT_MODE = "silent_mode"
        const val SOUND_FILES_COUNT = "sound_files_count"
        const val LAST_UPDATED = "last_updated"
        const val PREVIOUS_APP_VERSION = "previous_app_version"
        const val ALARMS_JSON = "alarms_json"
    }

    fun setAlarmVolume(volume: Int) {
        prefs.edit().putInt(Keys.ALARM_VOLUME, volume.coerceIn(0, 100)).apply()
    }

    fun getAlarmVolume(): Int = prefs.getInt(Keys.ALARM_VOLUME, 85)

    fun setAlarmSoundPath(path: String) {
        prefs.edit().putString(Keys.ALARM_SOUND_PATH, path).apply()
    }

    fun getAlarmSoundPath(): String = prefs.getString(Keys.ALARM_SOUND_PATH, "") ?: ""

    fun setAlarmSoundName(name: String) {
        prefs.edit().putString(Keys.ALARM_SOUND_NAME, name).apply()
    }

    fun getAlarmSoundName(): String = prefs.getString(Keys.ALARM_SOUND_NAME, "Default Alarm") ?: "Default Alarm"

    fun setVibrateEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Keys.VIBRATE_ENABLED, enabled).apply()
    }

    fun isVibrateEnabled(): Boolean = prefs.getBoolean(Keys.VIBRATE_ENABLED, true)

    fun setAlarmTime(hour: Int, minute: Int) {
        prefs.edit().putInt(Keys.ALARM_HOUR, hour).putInt(Keys.ALARM_MINUTE, minute).apply()
    }

    fun getAlarmHour(): Int = prefs.getInt(Keys.ALARM_HOUR, 8)
    fun getAlarmMinute(): Int = prefs.getInt(Keys.ALARM_MINUTE, 0)

    fun setAlarmEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Keys.ALARM_ENABLED, enabled).apply()
    }

    fun isAlarmEnabled(): Boolean = prefs.getBoolean(Keys.ALARM_ENABLED, false)

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean(Keys.THEME_DARK, enabled).apply()
    }

    fun isDarkTheme(): Boolean = prefs.getBoolean(Keys.THEME_DARK, true)

    fun setSilentMode(enabled: Boolean) {
        prefs.edit().putBoolean(Keys.SILENT_MODE, enabled).apply()
    }

    fun isSilentMode(): Boolean = prefs.getBoolean(Keys.SILENT_MODE, false)

    fun setBackgroundServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Keys.BACKGROUND_SERVICE, enabled).apply()
    }

    fun isBackgroundServiceEnabled(): Boolean = prefs.getBoolean(Keys.BACKGROUND_SERVICE, true)

    fun getSoundFilesCount(): Int = prefs.getInt(Keys.SOUND_FILES_COUNT, 2)

    fun incrementSoundFilesCount(): Int {
        val newCount = getSoundFilesCount() + 1
        prefs.edit().putInt(Keys.SOUND_FILES_COUNT, newCount).apply()
        return newCount
    }

    fun setPreviousAppVersion(version: String) {
        prefs.edit().putString(Keys.PREVIOUS_APP_VERSION, version).apply()
    }

    fun getPreviousAppVersion(): String = prefs.getString(Keys.PREVIOUS_APP_VERSION, "") ?: ""

    fun setLastUpdated() {
        prefs.edit().putLong(Keys.LAST_UPDATED, System.currentTimeMillis()).apply()
    }

    fun getLastUpdated(): Long = prefs.getLong(Keys.LAST_UPDATED, 0L)

    fun setAlarmsJson(json: String) {
        prefs.edit().putString(Keys.ALARMS_JSON, json).apply()
    }

    fun getAlarmsJson(): String = prefs.getString(Keys.ALARMS_JSON, "") ?: ""

    companion object {
        private const val PREFS_NAME = "stopwatch_settings"

        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
