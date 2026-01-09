package com.agriminds.utils;

import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

public class AudioPlayerManager {
    private static final String TAG = "AudioPlayerManager";
    private MediaPlayer mediaPlayer;

    public void play(String audioPath, OnCompletionListener listener) {
        stop(); // Stop potential previous playback

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioPath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(mp -> {
                if (listener != null)
                    listener.onCompletion();
                stop();
            });
        } catch (IOException e) {
            Log.e(TAG, "play: ", e);
            stop();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void release() {
        stop();
    }

    public interface OnCompletionListener {
        void onCompletion();
    }
}
