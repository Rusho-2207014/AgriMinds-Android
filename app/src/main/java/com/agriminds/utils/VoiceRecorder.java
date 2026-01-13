package com.agriminds.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

/**
 * Simple voice-to-text converter (NO audio file recording)
 * Uses Android SpeechRecognizer for real-time text recognition
 */
public class VoiceRecorder {
    private static final String TAG = "VoiceRecorder";
    private SpeechRecognizer speechRecognizer;
    private boolean isRecording = false;
    private VoiceRecorderListener listener;

    public interface VoiceRecorderListener {
        void onRecordingStarted();
        void onRecordingStopped(String audioPath);
        void onTextRecognized(String text);
        void onError(String error);
    }

    public VoiceRecorder(Context context, VoiceRecorderListener listener) {
        this.listener = listener;

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            Log.d(TAG, "✓ Speech recognizer created");
            
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "🎤 Ready - start speaking");
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "🗣️ Speech detected");
                }

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "Speech ended");
                }

                @Override
                public void onError(int error) {
                    Log.e(TAG, "Speech error: " + error);
                    if (error != SpeechRecognizer.ERROR_NO_MATCH && 
                        error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT &&
                        error != SpeechRecognizer.ERROR_CLIENT) {
                        if (listener != null) {
                            listener.onError("Speech recognition error: " + error);
                        }
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        Log.d(TAG, "✅ Recognized: " + text);
                        if (listener != null) {
                            listener.onTextRecognized(text);
                        }
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        Log.d(TAG, "🔄 Partial: " + text);
                        if (listener != null) {
                            listener.onTextRecognized(text);
                        }
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        }
    }

    public void startRecording(Context context) {
        if (isRecording || speechRecognizer == null) return;

        isRecording = true;
        
        if (listener != null) {
            listener.onRecordingStarted();
        }

        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        
        speechRecognizer.startListening(recognizerIntent);
        Log.d(TAG, "✓ Voice-to-text started (text-only mode)");
    }

    public void stopRecording() {
        if (!isRecording) return;

        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }

        isRecording = false;
        
        if (listener != null) {
            listener.onRecordingStopped(null); // No audio file
        }
        
        Log.d(TAG, "✓ Voice-to-text stopped");
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void release() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
}
