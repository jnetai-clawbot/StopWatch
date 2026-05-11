package com.jnetai.stopwatch.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.jnetai.stopwatch.R
import com.jnetai.stopwatch.utils.ErrorLogger

/**
 * AboutFragment - About tab shown in the ViewPager2.
 * Displays app name, version, developer info, website link,
 * share button, and link to GitHub releases.
 */
class AboutFragment : Fragment() {

    private var btnShare: Button? = null
    private var btnReleases: Button? = null
    private var tvVersion: TextView? = null

    companion object {
        private const val APP_VERSION = "1.0.1"
        private const val GITHUB_RELEASES_URL = "https://github.com/jnetai-clawbot/StopWatch/releases/latest"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            tvVersion = view.findViewById(R.id.tv_about_version)
            btnShare = view.findViewById(R.id.btn_about_share)
            btnReleases = view.findViewById(R.id.btn_about_releases)

            tvVersion?.text = "v$APP_VERSION"

            btnShare?.setOnClickListener {
                shareApp()
            }

            btnReleases?.setOnClickListener {
                openUrl(GITHUB_RELEASES_URL)
            }

        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_UI_THREAD,
                "Failed to initialize AboutFragment", e)
        }
    }

    private fun shareApp() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "StopWatch App")
                putExtra(Intent.EXTRA_TEXT,
                    "Check out StopWatch – a feature-packed stopwatch, timer, and alarm app!\n$GITHUB_RELEASES_URL")
            }
            startActivity(Intent.createChooser(shareIntent, "Share StopWatch via"))
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_UI_THREAD,
                "Failed to share app", e)
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_UI_THREAD,
                "Failed to open URL: $url", e)
        }
    }
}
