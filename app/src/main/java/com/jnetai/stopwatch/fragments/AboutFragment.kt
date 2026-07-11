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
import com.jnetai.stopwatch.BuildConfig
import com.jnetai.stopwatch.R
import com.jnetai.stopwatch.utils.ErrorLogger
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AboutFragment : Fragment() {

    private var btnShare: Button? = null
    private var btnCheckUpdates: Button? = null
    private var btnOpenRelease: Button? = null
    private var tvVersion: TextView? = null
    private var tvUpdateStatus: TextView? = null

    companion object {
        private const val GITHUB_RELEASES_URL = "https://github.com/jnetai-clawbot/StopWatch/releases/latest"
        private const val GITHUB_API_URL = "https://api.github.com/repos/jnetai-clawbot/StopWatch/releases/latest"
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
            btnCheckUpdates = view.findViewById(R.id.btn_about_releases)
            btnOpenRelease = view.findViewById(R.id.btn_open_release)
            tvUpdateStatus = view.findViewById(R.id.tv_update_status)

            tvVersion?.text = "v${BuildConfig.VERSION_NAME}"

            btnShare?.setOnClickListener { shareApp() }
            btnCheckUpdates?.setOnClickListener { checkForUpdates() }
            btnOpenRelease?.setOnClickListener { openUrl(GITHUB_RELEASES_URL) }
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_UI_THREAD, "Failed to initialize AboutFragment", e)
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
            ErrorLogger.log(ErrorLogger.Codes.GEN_UI_THREAD, "Failed to share app", e)
        }
    }

    private fun checkForUpdates() {
        btnCheckUpdates?.isEnabled = false
        btnCheckUpdates?.text = "Checking..."
        tvUpdateStatus?.text = "Connecting to GitHub..."
        tvUpdateStatus?.visibility = View.VISIBLE
        btnOpenRelease?.visibility = View.GONE

        Thread {
            try {
                val url = URL(GITHUB_API_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("Accept", "application/vnd.github+json")

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(body)
                    val latestTag = json.optString("tag_name", "")

                    requireActivity().runOnUiThread {
                        val current = "v${BuildConfig.VERSION_NAME}"
                        if (latestTag.isNotEmpty()) {
                            if (latestTag == current) {
                                tvUpdateStatus?.text = "You're up to date ($latestTag)"
                                tvUpdateStatus?.setTextColor(0xFF00D4FF.toInt())
                                btnOpenRelease?.visibility = View.GONE
                            } else {
                                tvUpdateStatus?.text = "$latestTag available!"
                                tvUpdateStatus?.setTextColor(0xFF4ADE80.toInt())
                                btnOpenRelease?.visibility = View.VISIBLE
                            }
                            tvUpdateStatus?.visibility = View.VISIBLE
                        } else {
                            tvUpdateStatus?.text = "Could not determine latest version"
                            tvUpdateStatus?.visibility = View.VISIBLE
                        }
                        btnCheckUpdates?.text = "Check for Updates"
                        btnCheckUpdates?.isEnabled = true
                    }
                } else {
                    requireActivity().runOnUiThread {
                        tvUpdateStatus?.text = "GitHub API error (HTTP $responseCode)"
                        tvUpdateStatus?.visibility = View.VISIBLE
                        btnCheckUpdates?.text = "Check for Updates"
                        btnCheckUpdates?.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    tvUpdateStatus?.text = "Update check failed: ${e.localizedMessage ?: "Network error"}"
                    tvUpdateStatus?.visibility = View.VISIBLE
                    btnCheckUpdates?.text = "Check for Updates"
                    btnCheckUpdates?.isEnabled = true
                }
                ErrorLogger.log(ErrorLogger.Codes.GEN_UPDATE_CHECK, "Failed to check for updates", e)
            }
        }.start()
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            ErrorLogger.log(ErrorLogger.Codes.GEN_UI_THREAD, "Failed to open URL: $url", e)
        }
    }
}
