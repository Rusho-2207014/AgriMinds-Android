package com.agriminds.utils;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class VoiceRecorderManager {
    private static final String TAG = "VoiceRecorderManager";
    private MediaRecorder mediaRecorder;
    private String currentFilePath;
    private Context context;

    public VoiceRecorderManager(Context context) {
        this.context = context;
    }

    public void startRecording(String fileNameRequest) {
        // Create file
        File outputDir = context.getExternalFilesDir(null); // App-specific external storage
        File outputFile = null;
        try {
            outputFile = File.createTempFile(fileNameRequest, ".3gp", outputDir);
            currentFilePath = outputFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "prepareRecording: ", e);
            return;
        }

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(currentFilePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            Log.d(TAG, "startRecording: Recording started at " + currentFilePath);
        } catch (IOException e) {
            Log.e(TAG, "startRecording: ", e);
            currentFilePath = null;
        }
    }

    public String stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception e) {
                // If stopped immediately after start, it might throw exception
                Log.e(TAG, "stopRecording: ", e);
            }
            mediaRecorder = null;
            return currentFilePath;
        }
        return null;
    }

    public void release() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
}
