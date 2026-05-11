package com.jnetai.stopwatch

import android.app.Application
import com.jnetai.stopwatch.utils.SoundUtils

class StopWatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        SoundUtils.copyDefaultSounds(this)
    }

    companion object {
        lateinit var instance: StopWatchApplication
            private set
    }
}
