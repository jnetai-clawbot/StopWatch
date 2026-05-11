package com.jnetai.stopwatch.fragments.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * ViewPagerAdapter - Manages the four main fragments (StopWatch, Countdown, Alarm, About)
 * for swipe-based navigation between modes.
 */
class ViewPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> com.jnetai.stopwatch.fragments.StopWatchFragment()
            1 -> com.jnetai.stopwatch.fragments.CountdownFragment()
            2 -> com.jnetai.stopwatch.fragments.AlarmFragment()
            3 -> com.jnetai.stopwatch.fragments.AboutFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

    /**
     * Get the title for a given fragment position.
     */
    fun getPageTitle(position: Int): String {
        return when (position) {
            0 -> "Stopwatch"
            1 -> "Timer"
            2 -> "Alarm"
            3 -> "About"
            else -> ""
        }
    }
}
