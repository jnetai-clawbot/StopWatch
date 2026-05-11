package com.jnetai.stopwatch.fragments.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jnetai.stopwatch.R
import com.jnetai.stopwatch.models.LapEntry

/**
 * LapAdapter - RecyclerView adapter for displaying lap history.
 * Shows lap number, lap time, and total time for each recorded lap.
 */
class LapAdapter(
    private val laps: List<LapEntry>
) : RecyclerView.Adapter<LapAdapter.LapViewHolder>() {

    private val TAG = "LapAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LapViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lap, parent, false)
        return LapViewHolder(view)
    }

    override fun onBindViewHolder(holder: LapViewHolder, position: Int) {
        val lap = laps[position]
        holder.bind(lap)
    }

    override fun getItemCount(): Int = laps.size

    class LapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLapNumber: TextView = itemView.findViewById(R.id.tv_lap_number)
        private val tvLapTime: TextView = itemView.findViewById(R.id.tv_lap_time)
        private val tvTotalTime: TextView = itemView.findViewById(R.id.tv_total_time)

        fun bind(lap: LapEntry) {
            tvLapNumber.text = "Lap ${lap.number}"
            tvLapTime.text = formatTime(lap.lapTime)
            tvTotalTime.text = formatTime(lap.totalTime)
        }

        private fun formatTime(millis: Long): String {
            val totalMillis = millis.coerceAtLeast(0)
            val hours = totalMillis / 3600000
            val minutes = (totalMillis % 3600000) / 60000
            val seconds = (totalMillis % 60000) / 1000
            val centiseconds = (totalMillis % 1000) / 10
            return String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, centiseconds)
        }
    }
}
