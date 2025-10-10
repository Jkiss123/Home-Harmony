package com.example.furniturecloudy.util

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.fragment.app.Fragment
import java.util.*

class VoiceSearchManager(private val fragment: Fragment) {

    companion object {
        const val VOICE_REQUEST_CODE = 1001
    }

    fun startVoiceRecognition(requestCode: Int) {
        if (!SpeechRecognizer.isRecognitionAvailable(fragment.requireContext())) {
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói tên sản phẩm bạn muốn tìm...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            fragment.startActivityForResult(intent, requestCode)
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun handleVoiceResult(resultCode: Int, data: Intent?): String? {
        return if (resultCode == Activity.RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.get(0)?.trim()
        } else {
            null
        }
    }
}