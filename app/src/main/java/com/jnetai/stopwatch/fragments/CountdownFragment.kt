package com.jnetai.stopwatch.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.jnetai.stopwatch.R
import com.jnetai.stopwatch.utils.ErrorLogger
import com.jnetai.stopwatch.utils.SettingsManager
import com.jnetai.stopwatch.utils.SoundUtils

/**
 * CountdownFragment - Implements the countdown timer (egg timer) mode.
 * Users can input hours, minutes, and seconds. The timer counts down
 * and plays an alarm sound when it reaches zero.
 * Supports pause and resume functionality.
 */
class CountdownFragment : Fragment() {

    private var tvCountdown: TextView? = null
    private var etHours: EditText? = null
    private var etMinutes: EditText? = null
    private var etSeconds: EditText? = null
    private var btnStart: Button? = null
    private var btnPause: Button? = null
    private var btnReset: Button? = null

    private var countDownTimer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRunning = false
    private var isPaused = false
    private var remainingMillis: Long = 0L
    private var initialMillis: Long = 0L
    private var timerFinished = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_countdown, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            tvCountdown = view.findViewById(R.id.tv_countdown_time)
            etHours = view.findViewById(R.id.et_hours)
            etMinutes = view.findViewById(R.id.et_minutes)
            etSeconds = view.findViewById(R.id.et_seconds)
            btnStart = view.findViewById(R.id.btn_countdown_start)
            btnPause = view.findViewById(R.id.btn_countdown_pause)
            btnReset = view.findViewById(R.id.btn_countdown_reset)

            setupButtons()
            resetDisplay()

            // Load default values
            etHours?.setText("0")
            etMinutes?.setText("5")
            etSeconds?.setText("0")

            if (savedInstanceState != null) {
                restoreState(savedInstanceState)
            }

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.CDN_START_FAILED,
                "Failed to initialize Countdown view", e)
        }
    }

    private fun setupButtons() {
        btnStart?.setOnClickListener {
            if (!isRunning && !timerFinished) {
                startCountdown()
            } else if (timerFinished) {
                resetCountdown()
                startCountdown()
            }
        }

        btnPause?.setOnClickListener {
            if (isRunning && !isPaused) {
                pauseCountdown()
            } else if (isPaused) {
                resumeCountdown()
            }
        }

        btnReset?.setOnClickListener {
            resetCountdown()
        }
    }

    private fun startCountdown() {
        try {
            if (remainingMillis <= 0) {
                // Parse input values
                val hours = etHours?.text.toString().toLongOrNull() ?: 0
                val minutes = etMinutes?.text.toString().toLongOrNull() ?: 0
                val seconds = etSeconds?.text.toString().toLongOrNull() ?: 0

                if (hours < 0 || minutes < 0 || seconds < 0 ||
                    minutes > 59 || seconds > 59) {
                    tvCountdown?.text = "Invalid input"
                    return
                }

                if (hours == 0L && minutes == 0L && seconds == 0L) {
                    tvCountdown?.text = "Set time > 0"
                    return
                }

                remainingMillis = (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L)
                initialMillis = remainingMillis
            }

            // Hide input fields
            etHours?.visibility = View.GONE
            etMinutes?.visibility = View.GONE
            etSeconds?.visibility = View.GONE

            isRunning = true
            isPaused = false
            timerFinished = false

            countDownTimer = object : CountDownTimer(remainingMillis, 100) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingMillis = millisUntilFinished
                    tvCountdown?.text = formatCountdownTime(millisUntilFinished)
                }

                override fun onFinish() {
                    timerFinished = true
                    isRunning = false
                    tvCountdown?.text = "00:00:00"
                    playAlarm()
                    updateUI()
                }
            }.start()

            updateUI()

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.CDN_START_FAILED,
                "Failed to start countdown", e)
            tvCountdown?.text = "Error"
        }
    }

    private fun pauseCountdown() {
        try {
            countDownTimer?.cancel()
            isPaused = true
            isRunning = true
            updateUI()
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.CDN_PAUSE_FAILED,
                "Failed to pause countdown", e)
        }
    }

    private fun resumeCountdown() {
        try {
            isPaused = false
            isRunning = true

            countDownTimer = object : CountDownTimer(remainingMillis, 100) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingMillis = millisUntilFinished
                    tvCountdown?.text = formatCountdownTime(millisUntilFinished)
                }

                override fun onFinish() {
                    timerFinished = true
                    isRunning = false
                    tvCountdown?.text = "00:00:00"
                    playAlarm()
                    updateUI()
                }
            }.start()

            updateUI()

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.CDN_START_FAILED,
                "Failed to resume countdown", e)
        }
    }

    private fun resetCountdown() {
        try {
            countDownTimer?.cancel()
            countDownTimer = null
            isRunning = false
            isPaused = false
            timerFinished = false
            remainingMillis = initialMillis

            // Stop any playing alarm
            stopAlarm()

            // Show input fields
            etHours?.visibility = View.VISIBLE
            etMinutes?.visibility = View.VISIBLE
            etSeconds?.visibility = View.VISIBLE

            resetDisplay()
            updateUI()

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.CDN_RESET_FAILED,
                "Failed to reset countdown", e)
        }
    }

    private fun playAlarm() {
        try {
            val context = requireContext()
            val settings = SettingsManager.getInstance(context)
            val soundPath = settings.getAlarmSoundPath().ifEmpty {
                SoundUtils.getDefaultSoundPath(context)
            }
            val volume = settings.getAlarmVolume()

            // Vibrate if enabled
            if (settings.isVibrateEnabled()) {
                SoundUtils.startVibrate(context)
            }

            mediaPlayer = SoundUtils.createMediaPlayer(context, soundPath, volume)
            mediaPlayer?.start()

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.ALM_TRIGGER_FAILED,
                "Failed to play countdown alarm", e)
        }
    }

    private fun stopAlarm() {
        SoundUtils.stopVibrate(requireContext())
        SoundUtils.releaseMediaPlayer(mediaPlayer)
        mediaPlayer = null
    }

    private fun resetDisplay() {
        tvCountdown?.text = "00:00:00"
    }

    private fun updateUI() {
        if (timerFinished) {
            btnStart?.text = "Restart"
            btnStart?.isEnabled = true
            btnPause?.text = "Pause"
            btnPause?.isEnabled = false
            btnReset?.isEnabled = true
        } else if (!isRunning) {
            btnStart?.text = "Start"
            btnStart?.isEnabled = true
            btnPause?.text = "Pause"
            btnPause?.isEnabled = false
            btnReset?.isEnabled = true
        } else if (isRunning && !isPaused) {
            btnStart?.text = "Start"
            btnStart?.isEnabled = false
            btnPause?.text = "Pause"
            btnPause?.isEnabled = true
            btnReset?.isEnabled = true
        } else if (isPaused) {
            btnStart?.text = "Start"
            btnStart?.isEnabled = false
            btnPause?.text = "Resume"
            btnPause?.isEnabled = true
            btnReset?.isEnabled = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("remaining_millis", remainingMillis)
        outState.putLong("initial_millis", initialMillis)
        outState.putBoolean("is_running", isRunning)
        outState.putBoolean("is_paused", isPaused)
        outState.putBoolean("timer_finished", timerFinished)
        // Save input values so they reappear
        outState.putString("hours_text", etHours?.text.toString())
        outState.putString("minutes_text", etMinutes?.text.toString())
        outState.putString("seconds_text", etSeconds?.text.toString())
    }

    private fun restoreState(savedInstanceState: Bundle) {
        remainingMillis = savedInstanceState.getLong("remaining_millis", 0L)
        initialMillis = savedInstanceState.getLong("initial_millis", 0L)
        isRunning = savedInstanceState.getBoolean("is_running", false)
        isPaused = savedInstanceState.getBoolean("is_paused", false)
        timerFinished = savedInstanceState.getBoolean("timer_finished", false)

        // Restore input text
        etHours?.setText(savedInstanceState.getString("hours_text", "0"))
        etMinutes?.setText(savedInstanceState.getString("minutes_text", "5"))
        etSeconds?.setText(savedInstanceState.getString("seconds_text", "0"))

        if (timerFinished) {
            tvCountdown?.text = "00:00:00"
        } else if (isRunning || remainingMillis > 0) {
            tvCountdown?.text = formatCountdownTime(remainingMillis)
            // Hide inputs if timer was active
            etHours?.visibility = View.GONE
            etMinutes?.visibility = View.GONE
            etSeconds?.visibility = View.GONE
        }

        updateUI()
    }

    override fun onPause() {
        super.onPause()
        // Keep running in background, don't cancel
    }

    override fun onDestroyView() {
        // Don't stop timer on destroy - it should keep running
        // But clean up media player
        if (!isRunning) {
            stopAlarm()
        }
        super.onDestroyView()
    }

    override fun onDestroy() {
        // Only clean up alarm if we're done
        if (!isRunning && !isPaused) {
            stopAlarm()
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CountdownFragment"

        fun formatCountdownTime(millis: Long): String {
            val totalSeconds = (millis / 1000).coerceAtLeast(0)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }
}
