package com.agriminds.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class TTSManager {
    private static final String TAG = "TTSManager";
    private TextToSpeech textToSpeech;
    private boolean isInitialized = false;

    public TTSManager(Context context) {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("bn", "BD"));

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported");
                    // Fallback to minimal Bengali if region specific fails
                    textToSpeech.setLanguage(new Locale("bn"));
                } else {
                    isInitialized = true;
                    // Set speech rate for clearer pronunciation
                    textToSpeech.setSpeechRate(0.85f); // Slightly slower for clarity
                    textToSpeech.setPitch(1.0f); // Normal pitch
                    Log.d(TAG, "TTS initialized with Bengali language");
                }
            } else {
                Log.e(TAG, "Initialization failed");
            }
        });
    }

    public void speak(String text, OnSpeakListener listener) {
        if (text == null || text.trim().isEmpty()) {
            if (listener != null)
                listener.onError("No text to speak");
            return;
        }

        if (isInitialized) {
            int result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID");
            if (result == TextToSpeech.SUCCESS) {
                Log.d(TAG, "Speaking: " + text);
                if (listener != null)
                    listener.onStart();
            } else {
                Log.e(TAG, "TTS speak failed");
                if (listener != null)
                    listener.onError("Failed to speak");
            }
        } else {
            Log.e(TAG, "TTS not initialized");
            if (listener != null)
                listener.onError("Text-to-speech not ready. Please install Bengali language pack.");
        }
    }

    public void speak(String text) {
        speak(text, null);
    }

    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            Log.d(TAG, "TTS stopped");
        }
    }

    public boolean isSpeaking() {
        if (textToSpeech != null) {
            return textToSpeech.isSpeaking();
        }
        return false;
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    public boolean isReady() {
        return isInitialized;
    }

    public interface OnSpeakListener {
        void onStart();

        void onError(String error);
    }
}
