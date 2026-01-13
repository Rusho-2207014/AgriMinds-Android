package com.agriminds.utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * HYBRID approach: Uses AudioRecord (low-level) + SpeechRecognizer
 * This MIGHT work where MediaRecorder fails because AudioRecord
 * uses a different audio pipeline that may share better
 */
public class VoiceRecorderHybrid {
    private static final String TAG = "VoiceRecorderHybrid";
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private SpeechRecognizer speechRecognizer;
    private String audioFilePath;
    private boolean isRecording = false;
    private Thread recordingThread;
    private VoiceRecorderListener listener;

    public interface VoiceRecorderListener {
        void onRecordingStarted();

        void onRecordingStopped(String audioPath);

        void onTextRecognized(String text);

        void onError(String error);
    }

    public VoiceRecorderHybrid(Context context, VoiceRecorderListener listener) {
        this.listener = listener;

        // Initialize speech recognizer
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            Log.d(TAG, "Speech recognizer created");
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "🎤 Ready for speech");
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "🗣️ Speech detected");
                }

                @Override
                public void onRmsChanged(float rmsdB) {
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
                    Log.e(TAG, "Speech error: " + error);
                    if (error != SpeechRecognizer.ERROR_NO_MATCH &&
                            error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
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
                public void onEvent(int eventType, Bundle params) {
                }
            });
        }
    }

    public void startRecording(Context context) {
        if (isRecording)
            return;

        try {
            // Create audio directory
            File audioDir = new File(context.getFilesDir(), "audio");
            audioDir.mkdirs();
            audioFilePath = new File(audioDir, "voice_" + System.currentTimeMillis() + ".pcm").getAbsolutePath();

            // Calculate buffer size
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

            // Create AudioRecord (low-level, might share better than MediaRecorder)
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize * 2);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize");
                if (listener != null) {
                    listener.onError("Failed to initialize audio recording");
                }
                return;
            }

            isRecording = true;
            audioRecord.startRecording();
            Log.d(TAG, "✓ AudioRecord started (low-level API)");

            // Start recording in background thread
            recordingThread = new Thread(() -> {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(audioFilePath);
                    byte[] buffer = new byte[bufferSize];

                    while (isRecording) {
                        int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                        if (bytesRead > 0) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }

                    Log.d(TAG, "✓ Audio saved: " + audioFilePath);
                } catch (Exception e) {
                    Log.e(TAG, "Recording error: " + e.getMessage());
                } finally {
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (Exception e) {
                    }
                }
            });
            recordingThread.start();

            if (listener != null) {
                listener.onRecordingStarted();
            }

            // Start speech recognition AFTER AudioRecord is running
            if (speechRecognizer != null) {
                android.content.Intent recognizerIntent = new android.content.Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                speechRecognizer.startListening(recognizerIntent);
                Log.d(TAG, "✓ SpeechRecognizer started with AudioRecord");
            }

            Log.d(TAG, "HYBRID recording started");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start: " + e.getMessage());
            if (listener != null) {
                listener.onError(e.getMessage());
            }
        }
    }

    public void stopRecording() {
        if (!isRecording)
            return;

        try {
            if (speechRecognizer != null) {
                speechRecognizer.stopListening();
            }

            isRecording = false;

            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }

            if (recordingThread != null) {
                recordingThread.join(1000);
            }

            if (listener != null) {
                listener.onRecordingStopped(audioFilePath);
            }

            Log.d(TAG, "Recording stopped");
        } catch (Exception e) {
            Log.e(TAG, "Stop error: " + e.getMessage());
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void release() {
        if (audioRecord != null) {
            if (isRecording) {
                stopRecording();
            }
            audioRecord.release();
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}
