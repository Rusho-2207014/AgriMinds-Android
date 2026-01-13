package com.agriminds.utils;

import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Simple audio recorder using MediaRecorder for voice messages
 */
public class AudioRecorder {
    private static final String TAG = "AudioRecorder";
    private MediaRecorder mediaRecorder;
    private String currentFilePath;
    private boolean isRecording = false;
    private AudioRecorderListener listener;

    public interface AudioRecorderListener {
        void onRecordingStarted(String filePath);

        void onRecordingStopped(String filePath);

        void onError(String error);
    }

    public AudioRecorder(AudioRecorderListener listener) {
        this.listener = listener;
    }

    public void startRecording(Context context) {
        if (isRecording) {
            Log.w(TAG, "Already recording");
            return;
        }

        try {
            // Create file for recording
            File audioDir = new File(context.getFilesDir(), "audio");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }

            File audioFile = new File(audioDir, "voice_" + System.currentTimeMillis() + ".m4a");
            currentFilePath = audioFile.getAbsolutePath();

            // Initialize MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(currentFilePath);

            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            Log.d(TAG, "Recording started: " + currentFilePath);

            if (listener != null) {
                listener.onRecordingStarted(currentFilePath);
            }

        } catch (IOException e) {
            Log.e(TAG, "Recording failed", e);
            isRecording = false;
            if (listener != null) {
                listener.onError("Failed to start recording: " + e.getMessage());
            }
        }
    }

    public void stopRecording() {
        if (!isRecording || mediaRecorder == null) {
            Log.w(TAG, "Not recording");
            return;
        }

        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;

            Log.d(TAG, "Recording stopped: " + currentFilePath);

            if (listener != null) {
                listener.onRecordingStopped(currentFilePath);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
            if (listener != null) {
                listener.onError("Failed to stop recording: " + e.getMessage());
            }
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public String getCurrentFilePath() {
        return currentFilePath;
    }

    public void release() {
        if (mediaRecorder != null) {
            if (isRecording) {
                try {
                    mediaRecorder.stop();
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping on release", e);
                }
            }
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
        }
    }
}
