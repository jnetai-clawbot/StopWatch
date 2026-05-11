package com.jnetai.stopwatch.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jnetai.stopwatch.R
import com.jnetai.stopwatch.settings.APP_VERSION
import com.jnetai.stopwatch.settings.APP_VERSION_CODE
import com.jnetai.stopwatch.utils.ErrorLogger
import com.jnetai.stopwatch.utils.UpdateChecker

/**
 * AboutActivity - Shows app information, developer details,
 * version number, and update checking functionality.
 */
class AboutActivity : AppCompatActivity() {

    private var tvVersion: TextView? = null
    private var tvDeveloperSite: TextView? = null
    private var btnVisitSite: Button? = null
    private var btnCheckUpdate: Button? = null
    private var tvDeveloperName: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "About"

        try {
            tvVersion = findViewById(R.id.tv_version_value)
            tvDeveloperSite = findViewById(R.id.tv_developer_site)
            btnVisitSite = findViewById(R.id.btn_visit_site)
            btnCheckUpdate = findViewById(R.id.btn_check_update)
            tvDeveloperName = findViewById(R.id.tv_developer_name)

            // Set version
            val versionName = APP_VERSION
            val versionCode = APP_VERSION_CODE
            tvVersion?.text = "v$versionName (build $versionCode)"

            // Developer info
            tvDeveloperName?.text = "JNetAI"
            tvDeveloperSite?.text = "jnetai.com"

            // Visit developer site
            btnVisitSite?.setOnClickListener {
                openWebsite("https://jnetai.com")
            }

            // Check for updates
            btnCheckUpdate?.setOnClickListener {
                checkForUpdate()
            }

            ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                "AboutActivity displayed (version %s)", versionName)

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_UI_THREAD,
                "Failed to initialize AboutActivity", e)
        }
    }

    private fun openWebsite(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            ErrorLogger.log(ErrorLogger.Codes.GEN_UNEXPECTED,
                "Opening website: %s", url)
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_UI_THREAD,
                "Failed to open website: %s", url, e)
            Toast.makeText(this, "Unable to open browser", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkForUpdate() {
        Toast.makeText(this, "Opening GitHub releases...", Toast.LENGTH_SHORT).show()
        openWebsite(UpdateChecker.getReleasesUrl())
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
