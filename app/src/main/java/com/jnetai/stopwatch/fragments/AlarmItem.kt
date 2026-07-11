package com.jnetai.stopwatch.fragments

data class AlarmItem(
    val id: Int,
    var hour: Int = 8,
    var minute: Int = 0,
    var enabled: Boolean = false
)
