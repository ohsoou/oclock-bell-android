package com.example.oclockbell

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    private val exactAlarmSettings =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { /* no-op */ }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).also { setContentView(it) }

        webView.settings.apply {
            javaScriptEnabled                = true
            domStorageEnabled                = true
            databaseEnabled                  = true
            cacheMode                        = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            // Allow web app to detect it's running inside native wrapper
            userAgentString                  = "$userAgentString OClockBellNative/1.0"
        }

        // Expose native functions to JavaScript as window.NativeAlarm
        webView.addJavascriptInterface(WebAppInterface(this), "NativeAlarm")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val appHost = Uri.parse(BuildConfig.WEB_APP_URL).host
                return if (request.url.host == appHost) {
                    false   // load inside WebView
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    true    // open external URLs in browser
                }
            }
        }

        webView.loadUrl(BuildConfig.WEB_APP_URL)

        requestPermissions()
    }

    // ── Permissions ───────────────────────────────────────────────────

    private fun requestPermissions() {
        // POST_NOTIFICATIONS — Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        // SCHEDULE_EXACT_ALARM — only API 31-32 needs runtime request
        // API 33+ uses USE_EXACT_ALARM which is granted automatically
        if (Build.VERSION.SDK_INT in 31..32) {
            val am = getSystemService(AlarmManager::class.java)
            if (!am.canScheduleExactAlarms()) {
                exactAlarmSettings.launch(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                )
            }
        }

        requestBatteryOptimizationExemption()
    }

    fun requestBatteryOptimizationExemption() {
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }
    }

    // ── WebView back navigation ───────────────────────────────────────

    @Deprecated("Required override")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
