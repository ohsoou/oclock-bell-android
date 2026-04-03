package com.example.oclockbell

import android.content.Context
import android.webkit.JavascriptInterface
import com.example.oclockbell.AlarmPrefs.alarmOn
import com.example.oclockbell.AlarmPrefs.endHour
import com.example.oclockbell.AlarmPrefs.startHour
import com.example.oclockbell.AlarmPrefs.testMode

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

    /** Returns current native state as a JSON string. */
    @JavascriptInterface
    fun getState(): String =
        """{"alarmOn":${context.alarmOn},"startHour":${context.startHour},""" +
        """"endHour":${context.endHour},"testMode":${context.testMode}}"""

    /** Opens system screen to disable battery optimization for this app. */
    @JavascriptInterface
    fun requestBatteryExemption() {
        (context as? MainActivity)?.requestBatteryOptimizationExemption()
    }
}