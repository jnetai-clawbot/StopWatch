package com.jnetai.stopwatch.fragments.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> com.jnetai.stopwatch.fragments.StopWatchFragment()
            1 -> com.jnetai.stopwatch.fragments.CountdownFragment()
            2 -> com.jnetai.stopwatch.fragments.AlarmFragment()
            3 -> com.jnetai.stopwatch.fragments.AboutFragment()
            4 -> com.jnetai.stopwatch.fragments.SettingsFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
