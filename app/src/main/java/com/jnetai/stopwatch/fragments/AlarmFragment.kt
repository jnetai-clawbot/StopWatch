package com.jnetai.stopwatch.fragments

import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.jnetai.stopwatch.R
import com.jnetai.stopwatch.receiver.AlarmReceiver
import com.jnetai.stopwatch.utils.ErrorLogger
import com.jnetai.stopwatch.utils.SettingsManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.atomic.AtomicInteger

class AlarmFragment : Fragment() {

    private var alarmsContainer: LinearLayout? = null
    private var btnAddAlarm: Button? = null
    private val alarms = mutableListOf<AlarmItem>()
    private val countdownViews = mutableMapOf<Int, TextView>()
    private val handler = Handler(Looper.getMainLooper())
    private var tickRunnable: Runnable? = null
    private var nextId = AtomicInteger(1000)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_alarm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            alarmsContainer = view.findViewById(R.id.alarms_container)
            btnAddAlarm = view.findViewById(R.id.btn_add_alarm)
            loadAlarms()
            btnAddAlarm?.setOnClickListener { addNewAlarm() }
            startTicking()
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.ALM_SET_FAILED, "Failed to initialize Alarm view", e)
        }
    }

    private fun loadAlarms() {
        val settings = SettingsManager.getInstance(requireContext())
        val json = settings.getAlarmsJson()
        alarms.clear()
        var maxId = 999
        if (json.isNotEmpty()) {
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optInt("id", 0)
                    if (id > maxId) maxId = id
                    alarms.add(AlarmItem(
                        id = id,
                        hour = obj.optInt("hour", 8),
                        minute = obj.optInt("minute", 0),
                        enabled = obj.optBoolean("enabled", false)
                    ))
                }
            } catch (_: Exception) {}
        }
        if (alarms.isEmpty()) {
            alarms.add(AlarmItem(id = 1000, hour = 8, minute = 0))
            maxId = 1000
        }
        nextId.set(maxId + 1)
        rebuildAlarmViews()
    }

    private fun saveAlarms() {
        val arr = JSONArray()
        for (a in alarms) {
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("hour", a.hour)
                put("minute", a.minute)
                put("enabled", a.enabled)
            })
        }
        SettingsManager.getInstance(requireContext()).setAlarmsJson(arr.toString())
    }

    private fun rebuildAlarmViews() {
        alarmsContainer?.removeAllViews()
        countdownViews.clear()
        for (alarm in alarms) {
            val row = createAlarmRow(alarm)
            alarmsContainer?.addView(row)
        }
    }

    private fun createAlarmRow(alarm: AlarmItem): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(8) }
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            setBackgroundColor(0xFF1E1E1E.toInt())
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val timeText = TextView(ctx).apply {
            text = String.format("%02d:%02d", alarm.hour, alarm.minute)
            textSize = 20f
            setTextColor(0xFF00BCD4.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setOnClickListener { showTimePicker(alarm) }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f)
        }
        row.addView(timeText)

        val countdownText = TextView(ctx).apply {
            text = ""
            textSize = 11f
            setTextColor(0xFF9E9E9E.toInt())
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.8f)
        }
        row.addView(countdownText)
        countdownViews[alarm.id] = countdownText

        val switch = SwitchCompat(ctx).apply {
            isChecked = alarm.enabled
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dpToPx(4) }
            setOnCheckedChangeListener { _, checked ->
                alarm.enabled = checked
                if (checked) scheduleAlarm(alarm) else cancelAlarm(alarm)
                saveAlarms()
                updateCountdown(alarm, countdownText)
            }
        }
        row.addView(switch)

        val deleteBtn = Button(ctx).apply {
            text = "✕"
            textSize = 12f
            setTextColor(0xFFEF4444.toInt())
            setBackgroundColor(0x00000000)
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                if (alarms.size <= 1) {
                    Toast.makeText(ctx, "Cannot delete last alarm", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                cancelAlarm(alarm)
                alarms.remove(alarm)
                saveAlarms()
                rebuildAlarmViews()
            }
        }
        row.addView(deleteBtn)

        return row
    }

    private fun showTimePicker(alarm: AlarmItem) {
        TimePickerDialog(
            requireContext(),
            R.style.Theme_StopWatch_TimePicker,
            { _, h, m ->
                cancelAlarm(alarm)
                alarm.hour = h
                alarm.minute = m
                if (alarm.enabled) scheduleAlarm(alarm)
                saveAlarms()
                rebuildAlarmViews()
            },
            alarm.hour, alarm.minute, true
        ).show()
    }

    private fun addNewAlarm() {
        val id = nextId.getAndIncrement()
        val alarm = AlarmItem(id = id, hour = 8, minute = 0)
        alarms.add(alarm)
        saveAlarms()
        rebuildAlarmViews()
    }

    private fun scheduleAlarm(alarm: AlarmItem) {
        AlarmReceiver.scheduleAlarm(requireContext().applicationContext, alarm.id, alarm.hour, alarm.minute)
    }

    private fun cancelAlarm(alarm: AlarmItem) {
        AlarmReceiver.cancelAlarm(requireContext().applicationContext, alarm.id)
    }

    private fun startTicking() {
        tickRunnable = object : Runnable {
            override fun run() {
                updateAllCountdowns()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(tickRunnable!!)
    }

    private fun updateAllCountdowns() {
        for (alarm in alarms) {
            val tv = countdownViews[alarm.id] ?: continue
            updateCountdown(alarm, tv)
        }
    }

    private fun updateCountdown(alarm: AlarmItem, tv: TextView) {
        if (!alarm.enabled) {
            tv.text = ""
            return
        }
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val diffMs = target.timeInMillis - now.timeInMillis
        if (diffMs <= 0) {
            tv.text = "NOW"
            return
        }
        val totalSecs = diffMs / 1000
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        tv.text = if (hours > 0) "${hours}h ${mins}m ${secs}s" else "${mins}m ${secs}s"
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        loadAlarms()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tickRunnable?.let { handler.removeCallbacks(it) }
    }
}
