package com.jnetai.stopwatch.models

import java.io.Serializable

/**
 * LapEntry - Data model for a single lap record in the stopwatch.
 * Stores the lap number, lap time, and total elapsed time when the lap was recorded.
 */
data class LapEntry(
    val number: Int,
    val lapTime: Long,
    val totalTime: Long
) : Serializable
