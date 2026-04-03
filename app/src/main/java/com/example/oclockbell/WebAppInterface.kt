package com.example.oclockbell

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.webkit.JavascriptInterface
import com.example.oclockbell.AlarmPrefs.alarmOn
import com.example.oclockbell.AlarmPrefs.endHour
import com.example.oclockbell.AlarmPrefs.startHour
import com.example.oclockbell.AlarmPrefs.testMode
import com.example.oclockbell.AlarmPrefs.ttsPitch
import com.example.oclockbell.AlarmPrefs.ttsRate
import com.example.oclockbell.AlarmPrefs.ttsVolume
import org.json.JSONObject
import androidx.core.app.NotificationManagerCompat

/**
 * Exposed to JavaScript as `window.NativeAlarm`.
 * All methods are called on a background thread — do NOT touch the UI here.
 */
class WebAppInterface(private val context: Context) {

    /** Called when the web app's alarm toggle changes. */
    @JavascriptInterface
    fun setAlarm(on: Boolean, start: Int, end: Int) {
        context.alarmOn   = on
        context.startHour = start
        context.endHour   = end
        if (on) AlarmScheduler.scheduleNext(context)
        else    AlarmScheduler.cancel(context)
    }

    /** Called when the web app's test-mode toggle changes. */
    @JavascriptInterface
    fun setTestMode(enabled: Boolean) {
        context.testMode = enabled
        // Reschedule with new interval (1 min vs 1 h)
        if (context.alarmOn) AlarmScheduler.scheduleNext(context)
    }

    @JavascriptInterface
    fun setTtsConfig(pitch: Float, rate: Float, volume: Float) {
        context.ttsPitch = pitch
        context.ttsRate = rate
        context.ttsVolume = volume
    }

    @JavascriptInterface
    fun previewTts(text: String) {
        TtsSupport.preview(context, text)
    }

    /** Returns current native state as a JSON string. */
    @JavascriptInterface
    fun getState(): String =
        JSONObject()
            .put("alarmOn", context.alarmOn)
            .put("startHour", context.startHour)
            .put("endHour", context.endHour)
            .put("testMode", context.testMode)
            .put("pitch", context.ttsPitch.toDouble())
            .put("rate", context.ttsRate.toDouble())
            .put("volume", context.ttsVolume.toDouble())
            .put("notificationGranted", notificationsEnabled())
            .put("exactAlarmGranted", exactAlarmEnabled())
            .put("batteryOptimizationIgnored", batteryOptimizationIgnored())
            .toString()

    /** Opens system screen to disable battery optimization for this app. */
    @JavascriptInterface
    fun requestBatteryExemption() {
        (context as? MainActivity)?.requestBatteryOptimizationExemption()
    }

    private fun notificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun exactAlarmEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return alarmManager.canScheduleExactAlarms()
    }

    private fun batteryOptimizationIgnored(): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java)
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
}
