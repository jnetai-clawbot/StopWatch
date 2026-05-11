package com.jnetai.stopwatch.settings

import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jnetai.stopwatch.R
import com.jnetai.stopwatch.utils.ErrorLogger
import com.jnetai.stopwatch.utils.SettingsManager
import com.jnetai.stopwatch.utils.SoundUtils
import java.io.File

const val APP_VERSION = "1.0.1"
const val APP_VERSION_CODE = 2

/**
 * SettingsActivity - Settings page for the StopWatch app.
 * Provides controls for:
 * - Alarm volume level
 * - Alarm sound selection (auto-detected from sounds folder)
 * - Custom sound upload (MP3 files only)
 * - Vibrate toggle
 * - Background service toggle
 * - About section with developer info and update check
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var soundsDir: File

    // UI Elements
    private var seekBarVolume: SeekBar? = null
    private var tvVolumeValue: TextView? = null
    private var tvCurrentSound: TextView? = null
    private var btnPickSound: Button? = null
    private var btnUploadSound: Button? = null
    private var switchVibrate: Switch? = null
    private var switchBackground: Switch? = null
    private var tvAboutVersion: TextView? = null
    private var tvAboutSite: TextView? = null
    private var btnVisitSite: Button? = null
    private var switchSilent: Switch? = null
    private var btnShareApp: Button? = null
    private var btnUpdateCheck: Button? = null
    private var btnResetSettings: Button? = null

    // Sound picker dialog
    private var soundFileList: List<Pair<String, String>> = emptyList()
    private var selectedSoundIndex = 0

    // File picker for custom MP3 uploads
    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            uploadCustomSound(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        settingsManager = SettingsManager.getInstance(this)

        // Get the sounds directory (app's internal sounds folder)
        soundsDir = SoundUtils.getSoundsDir(this)

        initViews()
        loadSettings()
        setupListeners()
    }

    private fun initViews() {
        seekBarVolume = findViewById(R.id.seek_bar_volume)
        tvVolumeValue = findViewById(R.id.tv_volume_value)
        tvCurrentSound = findViewById(R.id.tv_current_sound)
        btnPickSound = findViewById(R.id.btn_pick_sound)
        btnUploadSound = findViewById(R.id.btn_upload_sound)
        switchVibrate = findViewById(R.id.switch_vibrate)
        switchBackground = findViewById(R.id.switch_background)
        tvAboutVersion = findViewById(R.id.tv_about_version)
        tvAboutSite = findViewById(R.id.tv_about_site)
        btnVisitSite = findViewById(R.id.btn_visit_site)
        switchSilent = findViewById(R.id.switch_silent)
        btnShareApp = findViewById(R.id.btn_share_app)
        btnUpdateCheck = findViewById(R.id.btn_update_check)
        btnResetSettings = findViewById(R.id.btn_reset_settings)
    }

    private fun loadSettings() {
        try {
            // Volume
            val volume = settingsManager.getAlarmVolume()
            seekBarVolume?.progress = volume
            tvVolumeValue?.text = "$volume%"

            // Current sound
            val soundName = settingsManager.getAlarmSoundName()
            tvCurrentSound?.text = soundName

            // Vibrate
            switchVibrate?.isChecked = settingsManager.isVibrateEnabled()

            // Silent mode
            switchSilent?.isChecked = settingsManager.isSilentMode()

            // Background service
            switchBackground?.isChecked = settingsManager.isBackgroundServiceEnabled()

            // About info
            val versionName = APP_VERSION
            val versionCode = APP_VERSION_CODE
            tvAboutVersion?.text = "v$versionName (build $versionCode)"
            tvAboutSite?.text = "jnetai.com"

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.SET_LOAD_FAILED,
                "Failed to load settings in UI", e)
        }
    }

    private fun setupListeners() {
        // Volume slider
        seekBarVolume?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvVolumeValue?.text = "$progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val volume = seekBar?.progress ?: 85
                settingsManager.setAlarmVolume(volume)
                settingsManager.setLastUpdated()
                ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                    "Alarm volume set to %d%%", volume)
            }
        })

        // Sound picker
        btnPickSound?.setOnClickListener {
            showSoundPicker()
        }

        // Upload custom sound
        btnUploadSound?.setOnClickListener {
            pickFileLauncher.launch("audio/mpeg")
        }

        // Vibrate toggle
        switchVibrate?.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setVibrateEnabled(isChecked)
            settingsManager.setLastUpdated()

            if (isChecked) {
                // Test vibrate
                testVibration()
            }
            ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                "Vibrate %s", if (isChecked) "enabled" else "disabled")
        }

        // Background service toggle
        switchBackground?.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setBackgroundServiceEnabled(isChecked)
            settingsManager.setLastUpdated()
            ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                "Background service %s", if (isChecked) "enabled" else "disabled")
        }

        // About - Visit website
        btnVisitSite?.setOnClickListener {
            openWebsite("https://jnetai.com")
        }

        // Silent mode toggle
        switchSilent?.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setSilentMode(isChecked)
            settingsManager.setLastUpdated()
        }

        // Share app
        btnShareApp?.setOnClickListener {
            shareApp()
        }

        // Update check
        btnUpdateCheck?.setOnClickListener {
            checkForUpdates()
        }

        // Reset settings
        btnResetSettings?.setOnClickListener {
            showResetConfirmDialog()
        }
    }

    private fun showSoundPicker() {
        try {
            // Detect available sound files
            soundFileList = SoundUtils.detectSoundFiles(soundsDir)

            if (soundFileList.isEmpty()) {
                Toast.makeText(this, "No alarm sounds found", Toast.LENGTH_SHORT).show()
                return
            }

            val displayNames = soundFileList.map { it.first }.toTypedArray()
            val currentPath = settingsManager.getAlarmSoundPath()
            val currentIndex = soundFileList.indexOfFirst { it.second == currentPath }
                .coerceAtLeast(0)

            AlertDialog.Builder(this, R.style.Theme_StopWatch_Dialog)
                .setTitle("Select Alarm Sound")
                .setSingleChoiceItems(displayNames, currentIndex) { _, which ->
                    selectedSoundIndex = which
                }
                .setPositiveButton("OK") { _, _ ->
                    val selected = soundFileList[selectedSoundIndex]
                    settingsManager.setAlarmSoundPath(selected.second)
                    settingsManager.setAlarmSoundName(selected.first)
                    settingsManager.setLastUpdated()
                    tvCurrentSound?.text = selected.first
                    ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                        "Alarm sound set to: %s", selected.first)

                    // Play a preview of the selected sound
                    playPreviewSound(selected.second)
                }
                .setNegativeButton("Cancel", null)
                .show()

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.SET_SOUND_PICK_FAILED,
                "Failed to show sound picker", e)
            Toast.makeText(this, "Error loading sounds", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadCustomSound(uri: Uri) {
        try {
            // Verify it's an MP3 file
            val mimeType = contentResolver.getType(uri)
            if (mimeType != null && !mimeType.startsWith("audio/")) {
                Toast.makeText(this, "Please select an MP3 audio file", Toast.LENGTH_LONG).show()
                return
            }

            val savedFile = SoundUtils.saveUploadedSound(soundsDir, uri, this)
            if (savedFile != null) {
                settingsManager.setAlarmSoundPath(savedFile.absolutePath)
                settingsManager.setAlarmSoundName(savedFile.name)
                settingsManager.setLastUpdated()
                tvCurrentSound?.text = savedFile.name

                Toast.makeText(this, "Sound uploaded: ${savedFile.name}", Toast.LENGTH_SHORT).show()
                ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                    "Custom sound uploaded: %s (%d bytes)",
                    savedFile.name, savedFile.length())
            } else {
                Toast.makeText(this, "Failed to save uploaded sound", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.SET_SOUND_UPLOAD_FAILED,
                "Failed to upload custom sound", e)
            Toast.makeText(this, "Error: ${e.localizedMessage ?: "Upload failed"}",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun playPreviewSound(soundPath: String) {
        try {
            val mediaPlayer = SoundUtils.createMediaPlayer(this, soundPath, 30) // 30% volume for preview
            mediaPlayer?.apply {
                isLooping = false
                setOnCompletionListener { release() }
                start()
            }
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.ALM_SOUND_FAILED,
                "Failed to play preview sound", e)
        }
    }

    private fun testVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_PERMISSION,
                "Failed to test vibration", e)
        }
    }

    private fun shareApp() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "StopWatch App")
                putExtra(Intent.EXTRA_TEXT,
                    "Check out StopWatch – a feature-packed stopwatch, timer, and alarm app!\n" +
                    "https://github.com/jnetai-clawbot/StopWatch/releases/latest")
            }
            startActivity(Intent.createChooser(shareIntent, "Share StopWatch via"))
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_UI_THREAD, "Failed to share app", e)
        }
    }

    private fun openWebsite(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_UI_THREAD,
                "Failed to open website: %s", url, e)
            Toast.makeText(this, "Unable to open browser", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAbout() {
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
    }

    private fun checkForUpdates() {
        Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()

        val currentVersion = APP_VERSION
        val releaseUrl = com.jnetai.stopwatch.utils.UpdateChecker.getReleasesUrl()

        // Open web browser to GitHub releases page
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl))
            startActivity(browserIntent)
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_UPDATE_CHECK,
                "Failed to open browser for updates", e)
            Toast.makeText(this, "Visit: $releaseUrl", Toast.LENGTH_LONG).show()
        }
    }

    private fun showResetConfirmDialog() {
        AlertDialog.Builder(this, R.style.Theme_StopWatch_Dialog)
            .setTitle("Reset Settings")
            .setMessage("Reset all settings to their default values?")
            .setPositiveButton("Reset") { _, _ ->
                resetToDefaults()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetToDefaults() {
        try {
            settingsManager.setAlarmVolume(85)
            settingsManager.setAlarmSoundPath("")
            settingsManager.setAlarmSoundName("Default Alarm")
            settingsManager.setVibrateEnabled(true)
            settingsManager.setSilentMode(false)
            settingsManager.setBackgroundServiceEnabled(true)
            settingsManager.setAlarmEnabled(false)
            settingsManager.setLastUpdated()

            loadSettings()
            Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
            ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED, "Settings reset to defaults")

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.SET_SAVE_FAILED,
                "Failed to reset settings", e)
            Toast.makeText(this, "Failed to reset settings", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
