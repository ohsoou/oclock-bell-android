package com.example.oclockbell

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.oclockbell.AlarmPrefs.ttsPitch
import com.example.oclockbell.AlarmPrefs.ttsRate
import com.example.oclockbell.AlarmPrefs.ttsVolume
import java.util.Locale

object TtsSupport {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun applySavedConfig(tts: TextToSpeech, context: Context) {
        tts.language = Locale.KOREAN
        tts.setPitch(context.ttsPitch)
        tts.setSpeechRate(context.ttsRate)
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
    }

    fun buildSpeakParams(context: Context): Bundle =
        Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, context.ttsVolume)
        }

    fun preview(context: Context, text: String) {
        val appContext = context.applicationContext
        var tts: TextToSpeech? = null

        fun shutdown() {
            val engine = tts ?: return
            mainHandler.post {
                engine.shutdown()
                if (tts === engine) tts = null
            }
        }

        tts = TextToSpeech(appContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                shutdown()
                return@TextToSpeech
            }

            val engine = tts ?: return@TextToSpeech
            applySavedConfig(engine, appContext)
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) = shutdown()
                override fun onError(utteranceId: String?) = shutdown()
            })
            engine.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                buildSpeakParams(appContext),
                "preview_${System.currentTimeMillis()}"
            )
        }
    }
}
