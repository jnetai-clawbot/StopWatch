package com.jnetai.stopwatch.utils

import android.content.Context
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings

/**
 * SettingsManager - Persists all app settings using SharedPreferences.
 * Provides type-safe access to alarm volume, sound selection, vibrate mode,
 * and other user preferences. Settings survive app restarts.
 */
class SettingsManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- Keys ---
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
        const val SOUND_FILES_COUNT = "sound_files_count"
        const val LAST_UPDATED = "last_updated"
        const val PREVIOUS_APP_VERSION = "previous_app_version"
    }

    // --- Volume (0-100) ---
    fun setAlarmVolume(volume: Int) {
        val clamped = volume.coerceIn(0, 100)
        prefs.edit().putInt(Keys.ALARM_VOLUME, clamped).apply()
    }

    fun getAlarmVolume(): Int {
        return prefs.getInt(Keys.ALARM_VOLUME, 85)
    }

    // --- Sound Path ---
    fun setAlarmSoundPath(path: String) {
        prefs.edit().putString(Keys.ALARM_SOUND_PATH, path).apply()
    }

    fun getAlarmSoundPath(): String {
        return prefs.getString(Keys.ALARM_SOUND_PATH, "") ?: ""
    }

    // --- Sound Name (display) ---
    fun setAlarmSoundName(name: String) {
        prefs.edit().putString(Keys.ALARM_SOUND_NAME, name).apply()
    }

    fun getAlarmSoundName(): String {
        return prefs.getString(Keys.ALARM_SOUND_NAME, "Default Alarm") ?: "Default Alarm"
    }

    // --- Vibrate ---
    fun setVibrateEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Keys.VIBRATE_ENABLED, enabled).apply()
    }

    fun isVibrateEnabled(): Boolean {
        return prefs.getBoolean(Keys.VIBRATE_ENABLED, true)
    }

    // --- Alarm Time ---
    fun setAlarmTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(Keys.ALARM_HOUR, hour)
            .putInt(Keys.ALARM_MINUTE, minute)
            .apply()
    }

    fun getAlarmHour(): Int = prefs.getInt(Keys.ALARM_HOUR, 8)
    fun getAlarmMinute(): Int = prefs.getInt(Keys.ALARM_MINUTE, 0)

    fun setAlarmEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Keys.ALARM_ENABLED, enabled).apply()
    }

    fun isAlarmEnabled(): Boolean = prefs.getBoolean(Keys.ALARM_ENABLED, false)

    // --- Theme ---
    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean(Keys.THEME_DARK, enabled).apply()
    }

    fun isDarkTheme(): Boolean = prefs.getBoolean(Keys.THEME_DARK, true)

    // --- Background Service ---
    fun setBackgroundServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Keys.BACKGROUND_SERVICE, enabled).apply()
    }

    fun isBackgroundServiceEnabled(): Boolean =
        prefs.getBoolean(Keys.BACKGROUND_SERVICE, true)

    // --- Sound Files Count (for auto-naming uploads) ---
    fun getSoundFilesCount(): Int = prefs.getInt(Keys.SOUND_FILES_COUNT, 2)

    fun incrementSoundFilesCount(): Int {
        val newCount = getSoundFilesCount() + 1
        prefs.edit().putInt(Keys.SOUND_FILES_COUNT, newCount).apply()
        return newCount
    }

    // --- App Version Tracking ---
    fun setPreviousAppVersion(version: String) {
        prefs.edit().putString(Keys.PREVIOUS_APP_VERSION, version).apply()
    }

    fun getPreviousAppVersion(): String =
        prefs.getString(Keys.PREVIOUS_APP_VERSION, "") ?: ""

    // --- Last Updated Timestamp ---
    fun setLastUpdated() {
        prefs.edit().putLong(Keys.LAST_UPDATED, System.currentTimeMillis()).apply()
    }

    fun getLastUpdated(): Long = prefs.getLong(Keys.LAST_UPDATED, 0L)

    // --- Debug: Export all settings ---
    fun exportAll(): Map<String, Any?> {
        return mapOf(
            Keys.ALARM_VOLUME to getAlarmVolume(),
            Keys.ALARM_SOUND_PATH to getAlarmSoundPath(),
            Keys.ALARM_SOUND_NAME to getAlarmSoundName(),
            Keys.VIBRATE_ENABLED to isVibrateEnabled(),
            Keys.ALARM_HOUR to getAlarmHour(),
            Keys.ALARM_MINUTE to getAlarmMinute(),
            Keys.ALARM_ENABLED to isAlarmEnabled(),
            Keys.THEME_DARK to isDarkTheme(),
            Keys.BACKGROUND_SERVICE to isBackgroundServiceEnabled(),
            Keys.SOUND_FILES_COUNT to getSoundFilesCount(),
            "last_updated_date" to (if (getLastUpdated() > 0)
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                    .format(java.util.Date(getLastUpdated()))
            else "never")
        )
    }

    companion object {
        private const val PREFS_NAME = "stopwatch_settings"

        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
