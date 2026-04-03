package com.example.oclockbell

import android.content.Context
import android.content.SharedPreferences

object AlarmPrefs {
    private const val PREFS = "oclock_prefs"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var Context.alarmOn: Boolean
        get()      = prefs(this).getBoolean("alarmOn", false)
        set(value) = prefs(this).edit().putBoolean("alarmOn", value).apply()

    var Context.startHour: Int
        get()      = prefs(this).getInt("startHour", 8)
        set(value) = prefs(this).edit().putInt("startHour", value).apply()

    var Context.endHour: Int
        get()      = prefs(this).getInt("endHour", 22)
        set(value) = prefs(this).edit().putInt("endHour", value).apply()

    var Context.testMode: Boolean
        get()      = prefs(this).getBoolean("testMode", false)
        set(value) = prefs(this).edit().putBoolean("testMode", value).apply()

    var Context.ttsPitch: Float
        get()      = prefs(this).getFloat("ttsPitch", 1.5f)
        set(value) = prefs(this).edit().putFloat("ttsPitch", value).apply()

    var Context.ttsRate: Float
        get()      = prefs(this).getFloat("ttsRate", 0.8f)
        set(value) = prefs(this).edit().putFloat("ttsRate", value).apply()

    var Context.ttsVolume: Float
        get()      = prefs(this).getFloat("ttsVolume", 1.0f)
        set(value) = prefs(this).edit().putFloat("ttsVolume", value).apply()

    fun Context.isInRange(hour: Int): Boolean {
        val s = startHour; val e = endHour
        return if (s <= e) hour in s until e else hour >= s || hour < e
    }
}
