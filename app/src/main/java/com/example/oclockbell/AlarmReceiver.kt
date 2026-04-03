package com.example.oclockbell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.oclockbell.AlarmPrefs.alarmOn
import com.example.oclockbell.AlarmPrefs.isInRange
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Reschedule immediately — keeps the chain alive even if app is killed
        if (context.alarmOn) AlarmScheduler.scheduleNext(context)

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Only play audio if alarm is on and within the configured range
        if (!context.alarmOn || !context.isInRange(hour)) return

        val svc = Intent(context, AlarmService::class.java)
            .putExtra(AlarmService.EXTRA_HOUR, hour)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc)
        } else {
            context.startService(svc)
        }
    }
}