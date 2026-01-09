package com.agriminds.utils;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class VoiceRecorder {
    private static final String TAG = "VoiceRecorder";
    private MediaRecorder mediaRecorder;
    private SpeechRecognizer speechRecognizer;
    private String audioFilePath;
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

        // Initialize speech recognizer for text conversion
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "Ready for speech");
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "Speech started");
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // Audio level changed
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                }

                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "Speech ended");
                }

                @Override
                public void onError(int error) {
                    String errorMessage = getErrorText(error);
                    Log.e(TAG, "Speech recognition error: " + errorMessage);
                    // Don't report "no speech match" or "speech timeout" as errors
                    // These are common and recording continues regardless
                    if (error != SpeechRecognizer.ERROR_NO_MATCH &&
                            error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        if (listener != null) {
                            listener.onError(errorMessage);
                        }
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        Log.d(TAG, "Recognized text: " + text);
                        if (listener != null) {
                            listener.onTextRecognized(text);
                        }
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });
        }
    }

    public void startRecording(Context context) {
        if (isRecording) {
            return;
        }

        try {
            // Create audio file
            File audioDir = new File(context.getFilesDir(), "audio");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }

            audioFilePath = new File(audioDir, "voice_" + System.currentTimeMillis() + ".3gp").getAbsolutePath();

            // Start media recorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFilePath);

            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;

            if (listener != null) {
                listener.onRecordingStarted();
            }

            // Start speech recognition
            if (speechRecognizer != null) {
                android.content.Intent recognizerIntent = new android.content.Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US"); // English for better compatibility
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                speechRecognizer.startListening(recognizerIntent);
            }

            Log.d(TAG, "Recording started: " + audioFilePath);

        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording: " + e.getMessage());
            if (listener != null) {
                listener.onError("Failed to start recording: " + e.getMessage());
            }
        }
    }

    public void stopRecording() {
        if (!isRecording) {
            return;
        }

        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }

            if (speechRecognizer != null) {
                speechRecognizer.stopListening();
            }

            isRecording = false;

            if (listener != null) {
                listener.onRecordingStopped(audioFilePath);
            }

            Log.d(TAG, "Recording stopped: " + audioFilePath);

        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording: " + e.getMessage());
            if (listener != null) {
                listener.onError("Error stopping recording: " + e.getMessage());
            }
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void release() {
        if (mediaRecorder != null) {
            if (isRecording) {
                mediaRecorder.stop();
            }
            mediaRecorder.release();
            mediaRecorder = null;
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "RecognitionService busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Speech recognition error";
        }
    }
}
