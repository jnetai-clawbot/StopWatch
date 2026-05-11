package com.jnetai.stopwatch.fragments

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.jnetai.stopwatch.R
import com.jnetai.stopwatch.receiver.AlarmReceiver
import com.jnetai.stopwatch.service.AlarmForegroundService
import com.jnetai.stopwatch.utils.ErrorLogger
import com.jnetai.stopwatch.utils.SettingsManager
import com.jnetai.stopwatch.utils.SoundUtils
import java.util.Calendar

/**
 * AlarmFragment - Implements the alarm clock mode.
 * Allows the user to set an alarm time, toggle it on/off,
 * and displays the current alarm status. Uses AlarmManager
 * for reliable system-level alarm scheduling.
 */
class AlarmFragment : Fragment() {

    private var tvAlarmTime: TextView? = null
    private var tvAlarmStatus: TextView? = null
    private var btnSetAlarm: Button? = null
    private var switchAlarm: Switch? = null
    private var btnStopAlarm: Button? = null

    private var alarmHour: Int = 8
    private var alarmMinute: Int = 0
    private var alarmEnabled = false
    private var isAlarmRinging = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_alarm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            tvAlarmTime = view.findViewById(R.id.tv_alarm_time)
            tvAlarmStatus = view.findViewById(R.id.tv_alarm_status)
            btnSetAlarm = view.findViewById(R.id.btn_set_alarm)
            switchAlarm = view.findViewById(R.id.switch_alarm)
            btnStopAlarm = view.findViewById(R.id.btn_stop_alarm)

            loadSettings()
            setupButtons()
            updateDisplay()

            // Check if alarm is ringing from intent
            val isAlarmActive = activity?.intent?.getBooleanExtra("alarm_active", false) ?: false
            if (isAlarmActive) {
                onAlarmTriggered()
            }

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.ALM_SET_FAILED,
                "Failed to initialize Alarm view", e)
        }
    }

    private fun setupButtons() {
        btnSetAlarm?.setOnClickListener {
            showTimePicker()
        }

        switchAlarm?.setOnCheckedChangeListener { _, isChecked ->
            alarmEnabled = isChecked
            val settings = SettingsManager.getInstance(requireContext())
            settings.setAlarmEnabled(isChecked)

            if (isChecked) {
                scheduleAlarm()
            } else {
                cancelAlarm()
            }
            updateDisplay()
        }

        btnStopAlarm?.setOnClickListener {
            stopAlarm()
        }
    }

    private fun showTimePicker() {
        try {
            val timePickerDialog = TimePickerDialog(
                requireContext(),
                R.style.Theme_StopWatch_TimePicker,                { _, hourOfDay, minute ->
                    alarmHour = hourOfDay
                    alarmMinute = minute
                    val settings = SettingsManager.getInstance(requireContext())
                    settings.setAlarmTime(hourOfDay, minute)

                    if (alarmEnabled) {
                        // Re-schedule with new time
                        cancelAlarm()
                        scheduleAlarm()
                    }
                    updateDisplay()
                },
                alarmHour,
                alarmMinute,
                true // 24-hour format
            )

            // Apply dark theme programmatically if needed
            timePickerDialog.show()

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.ALM_SET_FAILED,
                "Failed to show time picker", e)
        }
    }

    private fun scheduleAlarm() {
        AlarmReceiver.scheduleAlarm(requireContext(), alarmHour, alarmMinute)
        ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
            "Alarm scheduled: %02d:%02d", alarmHour, alarmMinute)
    }

    private fun cancelAlarm() {
        AlarmReceiver.cancelAlarm(requireContext())
    }

    private fun stopAlarm() {
        AlarmForegroundService.stopAlarm(requireContext())
        SoundUtils.stopVibrate(requireContext())
        isAlarmRinging = false
        updateDisplay()
    }

    /**
     * Called when the alarm is triggered (from the service or notification).
     */
    fun onAlarmTriggered() {
        isAlarmRinging = true
        updateDisplay()
    }

    private fun loadSettings() {
        try {
            val settings = SettingsManager.getInstance(requireContext())
            alarmHour = settings.getAlarmHour()
            alarmMinute = settings.getAlarmMinute()
            alarmEnabled = settings.isAlarmEnabled()

            ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                "Alarm settings loaded: %02d:%02d enabled=%s",
                alarmHour, alarmMinute, alarmEnabled)

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.SET_LOAD_FAILED,
                "Failed to load alarm settings", e)
        }
    }

    private fun updateDisplay() {
        // Update alarm time display
        val timeStr = String.format("%02d:%02d", alarmHour, alarmMinute)
        tvAlarmTime?.text = timeStr

        // Update switch state without triggering listener
        switchAlarm?.setOnCheckedChangeListener(null)
        switchAlarm?.isChecked = alarmEnabled
        setupButtons() // Re-set listener

        // Update status text
        if (isAlarmRinging) {
            tvAlarmStatus?.text = "🔔 ALARM RINGING!"
            tvAlarmStatus?.visibility = View.VISIBLE
            btnStopAlarm?.visibility = View.VISIBLE
            btnSetAlarm?.isEnabled = false
            switchAlarm?.isEnabled = false
        } else if (alarmEnabled) {
            tvAlarmStatus?.text = "Alarm set for $timeStr"
            tvAlarmStatus?.visibility = View.VISIBLE
            btnStopAlarm?.visibility = View.GONE
            btnSetAlarm?.isEnabled = true
            switchAlarm?.isEnabled = true
        } else {
            tvAlarmStatus?.visibility = View.GONE
            btnStopAlarm?.visibility = View.GONE
            btnSetAlarm?.isEnabled = true
            switchAlarm?.isEnabled = true
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload settings in case they changed while we were away
        loadSettings()
        updateDisplay()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("alarm_hour", alarmHour)
        outState.putInt("alarm_minute", alarmMinute)
        outState.putBoolean("alarm_enabled", alarmEnabled)
        outState.putBoolean("is_ringing", isAlarmRinging)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            alarmHour = savedInstanceState.getInt("alarm_hour", 8)
            alarmMinute = savedInstanceState.getInt("alarm_minute", 0)
            alarmEnabled = savedInstanceState.getBoolean("alarm_enabled", false)
            isAlarmRinging = savedInstanceState.getBoolean("is_ringing", false)
            updateDisplay()
        }
    }

    companion object {
        private const val TAG = "AlarmFragment"
    }
}
