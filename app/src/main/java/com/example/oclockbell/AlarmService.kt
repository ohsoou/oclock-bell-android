package com.example.oclockbell

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener

class AlarmService : Service() {

    companion object {
        const val EXTRA_HOUR = "hour"
        const val CHANNEL_ID = "oclock_alarm"
        const val NOTIF_ID   = 1

        private val HOUR_KO = listOf(
            "자정이에요~", "한시!", "두시!", "세시!", "네시!", "다섯시!",
            "여섯시!", "일곱시!", "여덟시!", "아홉시!", "열시!", "열한시!",
            "열두시!", "한시!", "두시!", "세시!", "네시!", "다섯시!",
            "여섯시!", "일곱시!", "여덟시!", "아홉시!", "열시!", "열한시!"
        )
    }

    private var tts: TextToSpeech? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hour = intent?.getIntExtra(EXTRA_HOUR, -1) ?: -1

        // startForeground must be called within 5 s (Android 12+)
        startForeground(NOTIF_ID, buildNotification(hour))

        if (hour in 0..23) {
            playTts(hour) { stopSelf() }
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    // ── TTS ──────────────────────────────────────────────────────────

    private fun playTts(hour: Int, onDone: () -> Unit) {
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) {
                mainHandler.post(onDone)
                return@TextToSpeech
            }

            tts?.let { TtsSupport.applySavedConfig(it, this) }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?)  { mainHandler.post(onDone) }
                override fun onError(id: String?) { mainHandler.post(onDone) }
            })

            tts?.speak(
                HOUR_KO[hour],
                TextToSpeech.QUEUE_FLUSH,
                TtsSupport.buildSpeakParams(this),
                "oclock_$hour"
            )
        }
    }

    // ── Notification ─────────────────────────────────────────────────

    private fun buildNotification(hour: Int): Notification {
        val label = if (hour in 0..23) HOUR_KO[hour] else "정시 알람"
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle("정시 알람 🔔")
                .setContentText(label)
                .setContentIntent(tapIntent)
                .setAutoCancel(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle("정시 알람 🔔")
                .setContentText(label)
                .setContentIntent(tapIntent)
                .setAutoCancel(true)
                .build()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val ch = NotificationChannel(
            CHANNEL_ID,
            "정시 알람",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "매 정시 알람 소리"
            setSound(
                android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(ch)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
