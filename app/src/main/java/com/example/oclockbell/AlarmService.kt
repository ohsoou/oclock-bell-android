package com.example.oclockbell

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioManager: AudioManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioManager = getSystemService(AudioManager::class.java)
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
        requestAudioFocus()
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
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle("정시 알람 🔔")
                .setContentText(label)
                .setContentIntent(tapIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
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
            setSound(null, null)
            enableVibration(false)
            setBypassDnd(true)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(ch)
    }

    private fun requestAudioFocus() {
        val manager = audioManager ?: return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { }
                .build()
            audioFocusRequest = request
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let(manager::abandonAudioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(null)
        }
        audioFocusRequest = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        abandonAudioFocus()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
