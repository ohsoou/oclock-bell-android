package com.example.oclockbell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.oclockbell.AlarmPrefs.alarmOn

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Restore alarm schedule after device reboot
        if (context.alarmOn) AlarmScheduler.scheduleNext(context)
    }
}