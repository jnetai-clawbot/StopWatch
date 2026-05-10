package com.jnetai.stopwatch.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * SoundUtils - Handles alarm sound playback with configurable volume,
 * vibrate patterns, and sound file management. Used by all three modes
 * (stopwatch, countdown, alarm) for audio alerts.
 */
object SoundUtils {

    private const val TAG = "SoundUtils"

    // --- Media Player Management ---

    /**
     * Create and configure a MediaPlayer for the given sound file path.
     * Returns null if media player creation fails.
     */
    fun createMediaPlayer(context: Context, soundPath: String, volume: Int): MediaPlayer? {
        return ErrorLogger.tryOrNull(ErrorLogger.Codes.ALM_SOUND_FAILED,
            "Failed to create MediaPlayer for sound: $soundPath") {

            val mediaPlayer = MediaPlayer()

            // Set audio attributes for alarm stream
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(android.media.AudioManager.STREAM_ALARM)
                    .build()
            )

            // Set up sound source
            val soundFile = File(soundPath)
            if (soundFile.exists() && soundFile.isFile) {
                val fis = FileInputStream(soundFile)
                mediaPlayer.setDataSource(fis.fd)
            } else {
                // Fallback to default alarm sound
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                if (defaultUri != null) {
                    mediaPlayer.setDataSource(context, defaultUri)
                } else {
                    throw IllegalStateException("No default alarm sound available")
                }
            }

            mediaPlayer.prepare()

            // Set volume (0.0 to 1.0 based on 0-100 scale)
            val volumeFloat = (volume.toFloat() / 100f).coerceIn(0f, 1f)
            mediaPlayer.setVolume(volumeFloat, volumeFloat)

            // Enable looping for persistent alarm
            mediaPlayer.isLooping = true

            mediaPlayer
        }
    }

    /**
     * Safely stop and release a MediaPlayer.
     */
    fun releaseMediaPlayer(mediaPlayer: MediaPlayer?) {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (e: Exception) {
                ErrorLogger.log(ErrorLogger.Codes.ALM_SOUND_FAILED,
                    "Error releasing MediaPlayer", e)
            }
        }
    }

    // --- Vibration ---

    /**
     * Start vibration alarm pattern (long bursts for alarm).
     */
    fun startVibrate(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Pattern: on 1s, off 0.5s, repeat
                val pattern = longArrayOf(0, 1000, 500)
                vibrator.vibrate(
                    VibrationEffect.createWaveform(pattern, 1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 1000, 500), 1)
            }
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_PERMISSION,
                "Failed to start vibration", e)
        }
    }

    /**
     * Stop vibration.
     */
    fun stopVibrate(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.cancel()
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.ALM_SOUND_FAILED,
                "Failed to stop vibration", e)
        }
    }

    // --- Sound File Management ---

    /**
     * Auto-detect all alarm sound files in the sounds directory.
     * Returns list of (displayName, absolutePath) pairs.
     */
    fun detectSoundFiles(soundsDir: File): List<Pair<String, String>> {
        return ErrorLogger.tryOrNull(ErrorLogger.Codes.SET_SOUND_PICK_FAILED,
            "Failed to scan sounds directory: ${soundsDir.absolutePath}") {
            val sounds = mutableListOf<Pair<String, String>>()

            if (!soundsDir.exists() || !soundsDir.isDirectory) {
                ErrorLogger.log(ErrorLogger.Codes.SET_SOUND_PICK_FAILED,
                    "Sounds directory does not exist: ${soundsDir.absolutePath}")
                return@tryOrNull sounds
            }

            val files = soundsDir.listFiles()
            if (files != null) {
                // Filter for alarm sound files (alarmN.mp3)
                val alarmFiles = files.filter { file ->
                    file.isFile && file.name.matches(Regex("alarm\\d+\\.mp3"))
                }.sortedBy { file ->
                    // Extract number from filename for sorting
                    val num = file.name.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                    num
                }

                for (file in alarmFiles) {
                    sounds.add(Pair(file.name, file.absolutePath))
                }
            }

            sounds
        } ?: emptyList()
    }

    /**
     * Copy an uploaded MP3 file to the sounds directory with auto-named filename.
     * Pattern: alarmN.mp3 where N is the next available number.
     */
    fun saveUploadedSound(soundsDir: File, sourceUri: Uri, context: Context): File? {
        return ErrorLogger.tryOrNull(ErrorLogger.Codes.SET_SOUND_UPLOAD_FAILED,
            "Failed to save uploaded sound from URI: $sourceUri") {

            // Ensure sounds directory exists
            if (!soundsDir.exists()) {
                soundsDir.mkdirs()
            }

            // Find the next available number
            val existing = detectSoundFiles(soundsDir)
            val nextNum = if (existing.isEmpty()) {
                0
            } else {
                val maxNum = existing.maxOfOrNull { (name, _) ->
                    name.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                } ?: -1
                maxNum + 1
            }

            val destFile = File(soundsDir, "alarm${nextNum}.mp3")

            // Copy content from URI to destination file
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (destFile.exists() && destFile.length() > 0) {
                android.util.Log.i(TAG, "Uploaded sound saved: ${destFile.absolutePath}" +
                        " (${destFile.length()} bytes)")
                destFile
            } else {
                throw IllegalStateException("Failed to write uploaded sound file")
            }
        }
    }

    /**
     * Get the default alarm sound path as fallback.
     * Returns path to alarm0.mp3 in the app's sounds directory.
     */
    fun getDefaultSoundPath(context: Context): String {
        val soundsDir = getSoundsDir(context)
        val defaultFile = File(soundsDir, "alarm0.mp3")
        return if (defaultFile.exists()) {
            defaultFile.absolutePath
        } else {
            // Fallback to any existing alarm sound
            val existing = detectSoundFiles(soundsDir)
            if (existing.isNotEmpty()) {
                existing.first().second
            } else {
                ""
            }
        }
    }

    /**
     * Get the sounds directory path.
     * First checks the app's assets (bundled sounds), then app's external files dir.
     */
    fun getSoundsDir(context: Context): File {
        return File(context.filesDir, "sounds").also {
            if (!it.exists()) it.mkdirs()
        }
    }
}
