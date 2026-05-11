package com.jnetai.stopwatch.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jnetai.stopwatch.R
import com.jnetai.stopwatch.utils.ErrorLogger
import com.jnetai.stopwatch.models.LapEntry
import com.jnetai.stopwatch.fragments.adapters.LapAdapter

/**
 * StopWatchFragment - Implements the stopwatch mode with start, stop,
 * lap, and reset functionality. Displays elapsed time with millisecond precision
 * and maintains a lap history list.
 */
class StopWatchFragment : Fragment() {

    private var tvTime: TextView? = null
    private var btnStart: Button? = null
    private var btnLap: Button? = null
    private var btnReset: Button? = null
    private var rvLaps: RecyclerView? = null
    private var lapAdapter: LapAdapter? = null

    private var handler: Handler? = null
    private var startTime: Long = 0L
    private var pausedTime: Long = 0L
    private var isRunning = false
    private var isPaused = false
    private var lapCount = 0
    private val laps = mutableListOf<LapEntry>()
    private var lastLapTime: Long = 0L
    private var accumulatedTime: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stopwatch, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            tvTime = view.findViewById(R.id.tv_stopwatch_time)
            btnStart = view.findViewById(R.id.btn_start)
            btnLap = view.findViewById(R.id.btn_lap)
            btnReset = view.findViewById(R.id.btn_reset)
            rvLaps = view.findViewById(R.id.rv_laps)

            lapAdapter = LapAdapter(laps)
            rvLaps?.layoutManager = LinearLayoutManager(requireContext())
            rvLaps?.adapter = lapAdapter

            setupButtons()
            handler = Handler(Looper.getMainLooper())

            // Restore state if available
            if (savedInstanceState != null) {
                restoreState(savedInstanceState)
            }

            updateUI()

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.SWP_TIMER_INIT,
                "Failed to initialize StopWatch view", e)
        }
    }

    private fun setupButtons() {
        btnStart?.setOnClickListener {
            if (!isRunning || isPaused) {
                startStopwatch()
            } else {
                pauseStopwatch()
            }
        }

        btnLap?.setOnClickListener {
            if (isRunning && !isPaused) {
                recordLap()
            }
        }

        btnReset?.setOnClickListener {
            resetStopwatch()
        }
    }

    private fun startStopwatch() {
        try {
            if (isPaused) {
                // Resume from pause: accumulatedTime already holds the total elapsed
                // before pause. Just reset startTime so (now - startTime) adds the
                // post-resume portion correctly.
                startTime = SystemClock.elapsedRealtime()
                isPaused = false
            } else {
                // Fresh start
                startTime = SystemClock.elapsedRealtime()
                accumulatedTime = 0L
                lastLapTime = 0L
                lapCount = 0
            }

            isRunning = true
            isPaused = false
            updateTimer()
            updateUI()

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.SWP_START_FAILED,
                "Failed to start stopwatch", e)
        }
    }

    private fun pauseStopwatch() {
        try {
            isPaused = true
            pausedTime = SystemClock.elapsedRealtime()
            // Capture accumulated time so far
            accumulatedTime += (pausedTime - startTime)
            handler?.removeCallbacksAndMessages(null)
            updateUI()
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.SWP_PAUSE_FAILED,
                "Failed to pause stopwatch", e)
        }
    }

    private fun resetStopwatch() {
        try {
            handler?.removeCallbacksAndMessages(null)
            isRunning = false
            isPaused = false
            startTime = 0L
            pausedTime = 0L
            accumulatedTime = 0L
            lastLapTime = 0L
            lapCount = 0
            laps.clear()
            lapAdapter?.notifyDataSetChanged()
            tvTime?.text = "00:00:00.00"
            updateUI()
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.SWP_RESET_FAILED,
                "Failed to reset stopwatch", e)
        }
    }

    private fun recordLap() {
        try {
            val now = SystemClock.elapsedRealtime()
            val totalElapsed: Long
            if (isPaused) {
                totalElapsed = accumulatedTime
            } else {
                totalElapsed = accumulatedTime + (now - startTime)
            }

            val lapElapsed = if (lastLapTime == 0L) {
                totalElapsed
            } else {
                totalElapsed - lastLapTime
            }

            lapCount++
            val lapEntry = LapEntry(
                number = lapCount,
                lapTime = lapElapsed,
                totalTime = totalElapsed
            )

            laps.add(0, lapEntry) // Most recent at top
            lapAdapter?.notifyItemInserted(0)
            rvLaps?.scrollToPosition(0)

            lastLapTime = totalElapsed

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.SWP_LAP_FAILED,
                "Failed to record lap", e)
        }
    }

    private fun updateTimer() {
        if (!isRunning || isPaused) return

        handler?.postDelayed({
            if (isRunning && !isPaused) {
                updateDisplay()
                updateTimer()
            }
        }, 16) // ~60 FPS
    }

    private fun updateDisplay() {
        try {
            val now = SystemClock.elapsedRealtime()
            val elapsed = accumulatedTime + (now - startTime)
            tvTime?.text = formatTime(elapsed)
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.SWP_START_FAILED,
                "Failed to update timer display", e)
        }
    }

    private fun updateUI() {
        if (isRunning && !isPaused) {
            btnStart?.text = "Pause"
            btnLap?.isEnabled = true
            btnReset?.isEnabled = false
        } else if (isRunning && isPaused) {
            btnStart?.text = "Resume"
            btnLap?.isEnabled = false
            btnReset?.isEnabled = true
        } else {
            btnStart?.text = "Start"
            btnLap?.isEnabled = false
            btnReset?.isEnabled = true // Allow reset when stopped
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("saved_running", isRunning)
        outState.putBoolean("saved_paused", isPaused)
        outState.putLong("saved_start_time", startTime)
        outState.putLong("saved_paused_time", pausedTime)
        outState.putLong("saved_accumulated", accumulatedTime)
        outState.putSerializable("saved_laps", ArrayList(laps))
    }

    @Suppress("UNCHECKED_CAST")
    private fun restoreState(savedInstanceState: Bundle) {
        isRunning = savedInstanceState.getBoolean("saved_running", false)
        isPaused = savedInstanceState.getBoolean("saved_paused", false)
        startTime = savedInstanceState.getLong("saved_start_time", 0L)
        pausedTime = savedInstanceState.getLong("saved_paused_time", 0L)
        accumulatedTime = savedInstanceState.getLong("saved_accumulated", 0L)

        val savedLaps = savedInstanceState.getSerializable("saved_laps")
        if (savedLaps is ArrayList<*>) {
            laps.clear()
            savedLaps.forEach { entry ->
                if (entry is LapEntry) laps.add(entry)
            }
        }

        if (isRunning && !isPaused) {
            updateTimer()
        }

        updateDisplay()
    }

    override fun onPause() {
        super.onPause()
        // Don't stop timer on pause - keep running in background
    }

    override fun onResume() {
        super.onResume()
        if (isRunning && !isPaused) {
            updateTimer()
        }
    }

    override fun onDestroyView() {
        handler?.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "StopWatchFragment"

        fun formatTime(millis: Long): String {
            val totalMillis = millis.coerceAtLeast(0)
            val hours = totalMillis / 3600000
            val minutes = (totalMillis % 3600000) / 60000
            val seconds = (totalMillis % 60000) / 1000
            val centiseconds = (totalMillis % 1000) / 10
            return String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, centiseconds)
        }
    }
}
