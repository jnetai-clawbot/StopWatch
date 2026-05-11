package com.jnetai.stopwatch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jnetai.stopwatch.fragments.adapters.ViewPagerAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = getString(R.string.app_name)

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    settings.SettingsActivity.open(this)
                    true
                }
                else -> false
            }
        }
        toolbar.inflateMenu(R.menu.menu_main)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_stopwatch)
                1 -> getString(R.string.tab_countdown)
                2 -> getString(R.string.tab_alarm)
                else -> ""
            }
        }.attach()

        requestPermissions()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissions.add(android.Manifest.permission.SCHEDULE_EXACT_ALARM)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 100)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            for (i in permissions.indices) {
                val permission = permissions[i]
                val granted = grantResults[i] == android.content.pm.PackageManager.PERMISSION_GRANTED
                android.util.Log.d("MainActivity", "Permission $permission: $granted")
            }
        }
    }
}
