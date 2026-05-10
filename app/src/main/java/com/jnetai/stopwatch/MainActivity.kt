package com.jnetai.stopwatch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jnetai.stopwatch.fragments.adapters.ViewPagerAdapter
import com.jnetai.stopwatch.settings.SettingsActivity
import com.jnetai.stopwatch.utils.ErrorLogger

/**
 * MainActivity - The main activity hosting the three-mode stopwatch app.
 * Uses ViewPager2 with TabLayout for swiping between StopWatch, Countdown, and Alarm modes.
 * Handles critical permissions for foreground service and notifications.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var adapter: ViewPagerAdapter
    private var tvAppTitle: TextView? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { (permission, granted) ->
            ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                "Permission %s: %s",
                permission, if (granted) "granted" else "denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            // Setup toolbar
            setSupportActionBar(findViewById(R.id.toolbar))
            supportActionBar?.title = ""

            tvAppTitle = findViewById(R.id.tv_app_title)
            tvAppTitle?.text = "StopWatch"

            // Setup ViewPager with tabs
            viewPager = findViewById(R.id.view_pager)
            tabLayout = findViewById(R.id.tab_layout)

            adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
            viewPager.adapter = adapter

            // Connect TabLayout with ViewPager2
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = adapter.getPageTitle(position)
                tab.setIcon(getTabIcon(position))
            }.attach()

            // Request necessary permissions
            requestRequiredPermissions()

            ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                "MainActivity created successfully")
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_UI_THREAD,
                "Failed to initialize MainActivity", e)
        }
    }

    private fun getTabIcon(position: Int): Int {
        return when (position) {
            0 -> R.drawable.ic_stopwatch
            1 -> R.drawable.ic_timer
            2 -> R.drawable.ic_alarm
            else -> R.drawable.ic_stopwatch
        }
    }

    /**
     * Request permissions needed for alarm functionality.
     */
    private fun requestRequiredPermissions() {
        try {
            val permissionsToRequest = mutableListOf<String>()

            // POST_NOTIFICATIONS (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // SCHEDULE_EXACT_ALARM (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!canScheduleExactAlarms()) {
                    permissionsToRequest.add(Manifest.permission.SCHEDULE_EXACT_ALARM)
                }
            }

            if (permissionsToRequest.isNotEmpty()) {
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_PERMISSION,
                "Failed to request permissions", e)
        }
    }

    /**
     * Check if we can schedule exact alarms (Android 12+)
     */
    private fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                openSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openSettings() {
        try {
            val intent = Intent(this, SettingsActivity::class.java)
            ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                "Opening Settings activity")
            startActivity(intent)
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                "Failed to open settings", e)
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check alarm state
        checkAlarmState()
    }

    private fun checkAlarmState() {
        val intent = intent
        if (intent?.getBooleanExtra("alarm_active", false) == true) {
            // Alarm was active - notify the alarm fragment
            ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                "Alarm active state detected in MainActivity")
            // Clear the flag
            intent.removeExtra("alarm_active")
        }
    }
}
