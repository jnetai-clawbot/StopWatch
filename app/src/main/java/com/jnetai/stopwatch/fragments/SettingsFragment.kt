package com.jnetai.stopwatch.fragments

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.jnetai.stopwatch.R
import com.jnetai.stopwatch.utils.ErrorLogger
import com.jnetai.stopwatch.utils.SettingsManager
import com.jnetai.stopwatch.utils.SoundUtils
import java.io.File

/**
 * SettingsFragment - Settings tab shown in the ViewPager2.
 * Contains all settings controls previously in SettingsActivity.
 */
class SettingsFragment : Fragment() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var soundsDir: File

    // UI Elements
    private var seekBarVolume: SeekBar? = null
    private var tvVolumeValue: TextView? = null
    private var tvCurrentSound: TextView? = null
    private var btnPickSound: Button? = null
    private var btnPreviewSound: Button? = null
    private var btnUploadSound: Button? = null
    private var switchVibrate: Switch? = null
    private var switchSilent: Switch? = null
    private var switchBackground: Switch? = null
    private var btnSaveSettings: Button? = null

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        settingsManager = SettingsManager.getInstance(context)
        soundsDir = SoundUtils.getSoundsDir(context)

        initViews(view)
        loadSettings()
        setupListeners()
    }

    private fun initViews(view: View) {
        seekBarVolume = view.findViewById(R.id.seek_bar_volume)
        tvVolumeValue = view.findViewById(R.id.tv_volume_value)
        tvCurrentSound = view.findViewById(R.id.tv_current_sound)
        btnPickSound = view.findViewById(R.id.btn_pick_sound)
        btnPreviewSound = view.findViewById(R.id.btn_preview_sound)
        btnUploadSound = view.findViewById(R.id.btn_upload_sound)
        switchVibrate = view.findViewById(R.id.switch_vibrate)
        switchSilent = view.findViewById(R.id.switch_silent)
        switchBackground = view.findViewById(R.id.switch_background)
        btnSaveSettings = view.findViewById(R.id.btn_save_settings)
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

        // Preview sound button
        btnPreviewSound?.setOnClickListener {
            previewCurrentSound()
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
                testVibration()
            }
            ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                "Vibrate %s", if (isChecked) "enabled" else "disabled")
        }

        // Silent mode toggle
        switchSilent?.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setSilentMode(isChecked)
            settingsManager.setLastUpdated()
        }

        // Background service toggle
        switchBackground?.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setBackgroundServiceEnabled(isChecked)
            settingsManager.setLastUpdated()
            ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                "Background service %s", if (isChecked) "enabled" else "disabled")
        }

        // Save Settings button
        btnSaveSettings?.setOnClickListener {
            settingsManager.setLastUpdated()
            Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSoundPicker() {
        try {
            soundFileList = SoundUtils.detectSoundFiles(soundsDir)

            if (soundFileList.isEmpty()) {
                Toast.makeText(requireContext(), "No alarm sounds found", Toast.LENGTH_SHORT).show()
                return
            }

            val displayNames = soundFileList.map { it.first }.toTypedArray()
            val currentPath = settingsManager.getAlarmSoundPath()
            val currentIndex = soundFileList.indexOfFirst { it.second == currentPath }
                .coerceAtLeast(0)

            AlertDialog.Builder(requireContext(), R.style.Theme_StopWatch_Dialog)
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

                    // Auto-preview the selected sound
                    playPreviewSound(selected.second)
                }
                .setNegativeButton("Cancel", null)
                .show()

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.SET_SOUND_PICK_FAILED,
                "Failed to show sound picker", e)
            Toast.makeText(requireContext(), "Error loading sounds", Toast.LENGTH_SHORT).show()
        }
    }

    private fun previewCurrentSound() {
        try {
            val currentPath = settingsManager.getAlarmSoundPath()
            if (currentPath.isNotEmpty()) {
                playPreviewSound(currentPath)
            } else {
                // Try default
                val defaultPath = SoundUtils.getDefaultSoundPath(requireContext())
                if (defaultPath.isNotEmpty()) {
                    playPreviewSound(defaultPath)
                } else {
                    Toast.makeText(requireContext(), "No sound selected", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.ALM_SOUND_FAILED,
                "Failed to preview sound", e)
        }
    }

    private fun playPreviewSound(soundPath: String) {
        try {
            val mediaPlayer = SoundUtils.createMediaPlayer(requireContext(), soundPath, 30)
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

    private fun uploadCustomSound(uri: Uri) {
        try {
            val mimeType = requireContext().contentResolver.getType(uri)
            if (mimeType != null && !mimeType.startsWith("audio/")) {
                Toast.makeText(requireContext(), "Please select an MP3 audio file", Toast.LENGTH_LONG).show()
                return
            }

            val savedFile = SoundUtils.saveUploadedSound(soundsDir, uri, requireContext())
            if (savedFile != null) {
                settingsManager.setAlarmSoundPath(savedFile.absolutePath)
                settingsManager.setAlarmSoundName(savedFile.name)
                settingsManager.setLastUpdated()
                tvCurrentSound?.text = savedFile.name

                Toast.makeText(requireContext(), "Sound uploaded: ${savedFile.name}", Toast.LENGTH_SHORT).show()
                ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                    "Custom sound uploaded: %s (%d bytes)",
                    savedFile.name, savedFile.length())
            } else {
                Toast.makeText(requireContext(), "Failed to save uploaded sound", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.SET_SOUND_UPLOAD_FAILED,
                "Failed to upload custom sound", e)
            Toast.makeText(requireContext(), "Error: ${e.localizedMessage ?: "Upload failed"}",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun testVibration() {
        try {
            val context = requireContext()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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
}
