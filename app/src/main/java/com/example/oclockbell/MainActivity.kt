package com.example.oclockbell

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PREFS = "main_activity_requirements"
        private const val KEY_NOTIF_REQUESTED = "notification_permission_requested"
    }

    private enum class Requirement {
        NOTIFICATION,
        EXACT_ALARM,
        BATTERY_OPTIMIZATION
    }

    private lateinit var webView: WebView
    private lateinit var errorOverlay: View
    private lateinit var errorMessage: TextView

    private var currentWebPage: String = "main"
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var mainFrameLoadFailed = false
    private var shouldRecheckRequirements = false
    private var hasInitializedWebView = false
    private var pendingRequirement: Requirement? = null
    private var skipRequirementCheckOnNextResume = false
    private var allowMainScreenUntilExit = false
    private val requirementPrefs by lazy {
        getSharedPreferences(PREFS, MODE_PRIVATE)
    }

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingRequirement = null
                ensureRequirementsAndLoad()
            } else {
                openNotificationSettings()
            }
        }

    private val exactAlarmSettings =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            handleSettingsReturn(Requirement.EXACT_ALARM)
        }

    private val notificationSettings =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            handleSettingsReturn(Requirement.NOTIFICATION)
        }

    private val batteryOptimizationSettings =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            handleSettingsReturn(Requirement.BATTERY_OPTIMIZATION)
        }

    private val locationPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted =
                result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            pendingGeoCallback?.invoke(pendingGeoOrigin, granted, false)
            clearPendingGeolocationRequest()
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.web_view)
        errorOverlay = findViewById(R.id.error_overlay)
        errorMessage = findViewById(R.id.error_message)
        findViewById<Button>(R.id.retry_button).setOnClickListener { loadWebApp() }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setGeolocationEnabled(true)
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            userAgentString = "$userAgentString OClockBellNative/1.0"
        }

        webView.addJavascriptInterface(WebAppInterface(this), "NativeAlarm")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                if (hasLocationPermission()) {
                    callback.invoke(origin, true, false)
                    return
                }

                pendingGeoOrigin = origin
                pendingGeoCallback = callback
                locationPermissions.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val appHost = Uri.parse(BuildConfig.WEB_APP_URL).host
                return if (request.url.host == appHost) {
                    false
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    true
                }
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                mainFrameLoadFailed = false
                hideLoadError()
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (!mainFrameLoadFailed) hideLoadError()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (!request.isForMainFrame) return
                showLoadError(error.description?.toString())
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                if (!request.isForMainFrame || errorResponse.statusCode < 400) return
                showLoadError("서버 응답 ${errorResponse.statusCode}")
            }
        }

        ensureRequirementsAndLoad()
    }

    private fun loadWebApp() {
        hideLoadError()
        webView.loadUrl(BuildConfig.WEB_APP_URL)
    }

    private fun showLoadError(reason: String?) {
        mainFrameLoadFailed = true
        errorMessage.text = if (reason.isNullOrBlank()) {
            getString(R.string.web_load_error_message)
        } else {
            getString(R.string.web_load_error_with_reason, reason)
        }
        errorOverlay.visibility = View.VISIBLE
    }

    private fun hideLoadError() {
        errorOverlay.visibility = View.GONE
    }

    // ── Permissions ───────────────────────────────────────────────────

    private fun ensureRequirementsAndLoad(afterUserAction: Requirement? = null) {
        if (allowMainScreenUntilExit) {
            pendingRequirement = null
            shouldRecheckRequirements = false
            if (!hasInitializedWebView) {
                hasInitializedWebView = true
                loadWebApp()
            }
            return
        }

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED -> {
                if (afterUserAction == Requirement.NOTIFICATION) {
                    closeApp()
                } else {
                    shouldRecheckRequirements = false
                    pendingRequirement = Requirement.NOTIFICATION
                    if (shouldRequestNotificationPermissionDialog()) {
                        requirementPrefs.edit().putBoolean(KEY_NOTIF_REQUESTED, true).apply()
                        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openNotificationSettings()
                    }
                }
            }
            Build.VERSION.SDK_INT in 31..32 &&
                !getSystemService(AlarmManager::class.java).canScheduleExactAlarms() -> {
                if (afterUserAction == Requirement.EXACT_ALARM) {
                    closeApp()
                } else {
                    shouldRecheckRequirements = false
                    pendingRequirement = Requirement.EXACT_ALARM
                    exactAlarmSettings.launch(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    )
                }
            }
            !isIgnoringBatteryOptimizations() -> {
                if (afterUserAction == Requirement.BATTERY_OPTIMIZATION) {
                    closeApp()
                } else {
                    shouldRecheckRequirements = false
                    pendingRequirement = Requirement.BATTERY_OPTIMIZATION
                    requestBatteryOptimizationExemption()
                }
            }
            else -> {
                pendingRequirement = null
                shouldRecheckRequirements = false
                if (!hasInitializedWebView) {
                    hasInitializedWebView = true
                    loadWebApp()
                }
            }
        }
    }

    private fun handleSettingsReturn(requirement: Requirement) {
        val requirementSatisfied = when (requirement) {
            Requirement.NOTIFICATION -> {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
            }
            Requirement.EXACT_ALARM -> {
                Build.VERSION.SDK_INT !in 31..32 ||
                    getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
            }
            Requirement.BATTERY_OPTIMIZATION -> isIgnoringBatteryOptimizations()
        }

        if (requirementSatisfied) {
            pendingRequirement = null
            ensureRequirementsAndLoad()
            return
        }

        pendingRequirement = null
        shouldRecheckRequirements = false
        skipRequirementCheckOnNextResume = true
        allowMainScreenUntilExit = true
        if (!hasInitializedWebView) {
            hasInitializedWebView = true
            loadWebApp()
        }
    }

    fun requestBatteryOptimizationExemption() {
        batteryOptimizationSettings.launch(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
        )
    }

    private fun shouldRequestNotificationPermissionDialog(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val requestedBefore = requirementPrefs.getBoolean(KEY_NOTIF_REQUESTED, false)
        return !requestedBefore || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openNotificationSettings() {
        notificationSettings.launch(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        )
    }

    private fun isIgnoringBatteryOptimizations(): Boolean =
        getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(packageName)

    private fun closeApp() {
        clearPendingGeolocationRequest()
        pendingRequirement = null
        allowMainScreenUntilExit = false
        finishAndRemoveTask()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun clearPendingGeolocationRequest() {
        pendingGeoOrigin = null
        pendingGeoCallback = null
    }

    // ── WebView back navigation ───────────────────────────────────────

    fun onWebPageChanged(page: String) {
        currentWebPage = page
    }

    @Deprecated("Required override")
    override fun onBackPressed() {
        if (currentWebPage == "settings") {
            webView.evaluateJavascript("showPage('main')", null)
        } else {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (skipRequirementCheckOnNextResume) {
            skipRequirementCheckOnNextResume = false
            shouldRecheckRequirements = true
        } else if (shouldRecheckRequirements) {
            ensureRequirementsAndLoad(afterUserAction = pendingRequirement)
        } else {
            shouldRecheckRequirements = true
        }
        webView.onResume()
        webView.resumeTimers()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        webView.pauseTimers()
    }

    override fun onDestroy() {
        clearPendingGeolocationRequest()
        webView.removeJavascriptInterface("NativeAlarm")
        webView.destroy()
        super.onDestroy()
    }
}
