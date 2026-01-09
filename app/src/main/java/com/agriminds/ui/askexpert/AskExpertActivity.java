package com.agriminds.ui.askexpert;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.agriminds.R;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.Question;
import com.agriminds.utils.AudioPlayerManager;
import com.agriminds.utils.VoiceRecorder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class AskExpertActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;

    // Multiple API keys for rotation (60+ requests/day instead of 20)
    private static final String[] GEMINI_API_KEYS = {
            "AIzaSyBgcPsiq9SfdmUuOb6KgQiwawOTSp1tXH0",
            "AIzaSyCBbcNjEG-7dlNWuHuPCgXMCjeoZZrrE8U",
            "AIzaSyD7scjKl6K1m0khEb5OSRlPN4NoBzwcFCs"
    };
    private static int currentKeyIndex = 0;

    // AssemblyAI API key for speech-to-text (free tier: 5 hours/month)
    private static final String ASSEMBLYAI_API_KEY = "8e4e7c3d0a8d4c4d9f3e2b1a5c6d7e8f";

    private TextInputEditText editName, editQuestion;
    private Spinner spinnerCategory;
    private Button btnSubmit;
    private MaterialButton btnRecordVoice, btnPlayVoice;
    private TextView tvRecordingStatus;
    private AppDatabase database;
    private int currentUserId;
    private String currentUserName;
    private VoiceRecorder voiceRecorder;
    private AudioPlayerManager audioPlayerManager;
    private String currentAudioPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask_expert);

        database = AppDatabase.getInstance(this);

        SharedPreferences sharedPreferences = getSharedPreferences("AgriMindsPrefs", MODE_PRIVATE);
        currentUserId = sharedPreferences.getInt("userId", -1);
        currentUserName = sharedPreferences.getString("userName", "");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ask Expert");
        }

        editName = findViewById(R.id.edit_farmer_name);
        editQuestion = findViewById(R.id.edit_question);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSubmit = findViewById(R.id.btn_submit_question);
        btnRecordVoice = findViewById(R.id.btn_record_voice);
        btnPlayVoice = findViewById(R.id.btn_play_voice);
        tvRecordingStatus = findViewById(R.id.tv_recording_status);

        // Initialize audio player
        audioPlayerManager = new AudioPlayerManager();

        // Initialize voice recorder
        voiceRecorder = new VoiceRecorder(this, new VoiceRecorder.VoiceRecorderListener() {
            @Override
            public void onRecordingStarted() {
                runOnUiThread(() -> {
                    btnRecordVoice.setText("⏹️ Stop Recording");
                    tvRecordingStatus.setText("🔴 Recording...");
                    tvRecordingStatus.setVisibility(TextView.VISIBLE);
                });
            }

            @Override
            public void onRecordingStopped(String audioPath) {
                runOnUiThread(() -> {
                    currentAudioPath = audioPath;
                    btnRecordVoice.setText("🎤 Record Voice");
                    btnPlayVoice.setVisibility(MaterialButton.VISIBLE);
                    tvRecordingStatus.setText("✅ Voice recorded");
                    tvRecordingStatus.setVisibility(TextView.VISIBLE);
                });
            }

            @Override
            public void onTextRecognized(String text) {
                runOnUiThread(() -> {
                    // Append recognized text to question field
                    String currentText = editQuestion.getText().toString();
                    if (!currentText.isEmpty() && !currentText.endsWith(" ")) {
                        currentText += " ";
                    }
                    editQuestion.setText(currentText + text);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Only show important errors, recording still works
                    Log.e("AskExpertActivity", "Voice error: " + error);
                    Toast.makeText(AskExpertActivity.this, "Recording audio only (speech recognition unavailable)",
                            Toast.LENGTH_SHORT).show();
                });
            }

        });

        // Pre-fill farmer name if logged in
        if (!currentUserName.isEmpty()) {
            editName.setText(currentUserName);
        }

        btnRecordVoice.setOnClickListener(v -> {
            if (voiceRecorder.isRecording()) {
                voiceRecorder.stopRecording();
            } else {
                if (checkPermissions()) {
                    voiceRecorder.startRecording(this);
                } else {
                    requestPermissions();
                }
            }
        });

        btnPlayVoice.setOnClickListener(v -> {
            if (currentAudioPath != null) {

                // Check if file exists
                java.io.File audioFile = new java.io.File(currentAudioPath);
                if (!audioFile.exists()) {
                    Toast.makeText(this, "Audio file not found", Toast.LENGTH_SHORT).show();
                    Log.e("AskExpertActivity", "Audio file does not exist: " + currentAudioPath);
                    return;
                }

                btnPlayVoice.setText("⏸️ Playing...");
                audioPlayerManager.play(currentAudioPath, () -> {
                    runOnUiThread(() -> btnPlayVoice.setText("▶️ Play"));
                });
            } else {
                Toast.makeText(this, "No recording available", Toast.LENGTH_SHORT).show();
            }
        });

        btnSubmit.setOnClickListener(v -> submitQuestion());
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.RECORD_AUDIO },
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted! Click record again.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied. Voice recording won't work.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceRecorder != null) {
            voiceRecorder.release();
        }
        if (audioPlayerManager != null) {
            audioPlayerManager.release();
        }
    }

    private void submitQuestion() {
        String name = editName.getText().toString().trim();
        String questionText = editQuestion.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        // Validation: Name is required, but question can be either text OR voice
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (questionText.isEmpty() && (currentAudioPath == null || currentAudioPath.isEmpty())) {
            Toast.makeText(this, "Please enter question text or record voice", Toast.LENGTH_SHORT).show();
            return;
        }

        // If only voice, set placeholder text
        if (questionText.isEmpty() && currentAudioPath != null) {
            questionText = "[Voice Question]";
        }

        // Save question to database
        final String finalQuestionText = questionText;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Question question = new Question();
            question.setFarmerId(currentUserId);
            question.setFarmerName(name);
            question.setCategory(category);
            question.setQuestionText(finalQuestionText);
            question.setImageUrl(""); // No image for now
            question.setAudioPath(currentAudioPath); // Save voice recording path
            question.setStatus("Pending");
            question.setAnswerCount(0);
            question.setCreatedAt(System.currentTimeMillis());

            long questionId = database.questionDao().insertQuestion(question);

            runOnUiThread(() -> {
                if (questionId > 0) {
                    new AlertDialog.Builder(this)
                            .setTitle("Question Submitted")
                            .setMessage("Thank you, " + name + "!\n\n" +
                                    "Your question about \"" + category + "\" has been submitted.\n\n" +
                                    "Experts will answer your question soon. Check 'My Questions' to see responses.\n\n"
                                    +
                                    "Question ID: #" + questionId)
                            .setPositiveButton("OK", (dialog, which) -> {
                                clearForm();
                                dialog.dismiss();
                                finish(); // Go back to dashboard
                            })
                            .show();
                } else {
                    Toast.makeText(this, "Failed to submit question. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void clearForm() {
        editName.setText("");
        editQuestion.setText("");
        currentAudioPath = null;
        btnPlayVoice.setVisibility(MaterialButton.GONE);
        tvRecordingStatus.setVisibility(TextView.GONE);
        spinnerCategory.setSelection(0);
    }

    private void generateAIAnswer(int questionId, String questionText) {
        // Generate AI answer using Gemini AI
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                android.util.Log.d("GeminiAI", "Generating AI answer for question: " + questionText);

                // Get question from database to check for audio
                Question originalQuestion = database.questionDao().getQuestionById(questionId);
                
                // If this is a voice question, try to transcribe the audio first
                String actualQuestionText = questionText;
                if ("[Voice Question]".equals(questionText) && originalQuestion != null 
                        && originalQuestion.getAudioPath() != null 
                        && !originalQuestion.getAudioPath().isEmpty()) {
                    android.util.Log.d("GeminiAI", "Voice question detected, transcribing audio...");
                    
                    File audioFile = new File(originalQuestion.getAudioPath());
                    if (audioFile.exists()) {
                        String transcribedText = transcribeAudio(audioFile);
                        if (transcribedText != null && !transcribedText.trim().isEmpty()) {
                            actualQuestionText = transcribedText;
                            android.util.Log.d("GeminiAI", "Audio transcribed: " + transcribedText);
                            
                            // Update the question in database with transcribed text
                            originalQuestion.setQuestionText(transcribedText);
                            database.questionDao().updateQuestion(originalQuestion);
                        } else {
                            android.util.Log.w("GeminiAI", "Transcription failed or returned empty");
                        }
                    }
                }

                // Create JSON request
                JSONObject requestJson = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();

                JSONObject textPart = new JSONObject();
                String promptText;
                if ("[Voice Question]".equals(actualQuestionText)) {
                    // Transcription failed - provide general advice
                    promptText = "You are an agricultural expert assistant. A farmer has submitted a voice question but the speech-to-text transcription is not available. "
                            +
                            "Provide helpful, general agricultural advice covering common topics like: " +
                            "1) Proper irrigation and water management, " +
                            "2) Common pest identification and control, " +
                            "3) Soil health and fertilization basics, " +
                            "4) Seasonal crop care tips. " +
                            "Keep the answer concise (4-6 sentences), actionable, and suitable for farmers. " +
                            "Encourage them to consult with local experts for specific issues.";
                } else {
                    // Use the transcribed or original question text
                    promptText = "You are an agricultural expert assistant. A farmer has asked: \"" + actualQuestionText
                            + "\"\n\n" +
                            "Provide a helpful, practical answer about agriculture, crop management, pest control, or farming techniques. " +
                            "Keep the answer concise (3-5 sentences), actionable, and suitable for farmers. " +
                            "Focus on practical advice they can implement.";
                }
                textPart.put("text", promptText);
                parts.put(textPart);
                } else {
                    // Text-based question
                    JSONObject textPart = new JSONObject();
                    String promptText = "You are an agricultural expert assistant. A farmer has asked: \""
                            + questionText
                            + "\"\n\n" +
                            "Provide a helpful, practical answer about agriculture, crop management, pest control, or farming techniques. "
                            +
                            "Keep the answer concise (3-5 sentences), actionable, and suitable for farmers. " +
                            "Focus on practical advice they can implement.";
                    textPart.put("text", promptText);
                    parts.put(textPart);
                }

                content.put("parts", parts);
                contents.put(content);
                requestJson.put("contents", contents);

                android.util.Log.d("GeminiAI", "Request JSON created");

                // Make API call with key rotation
                String aiAnswerText = null;
                boolean success = false;

                // Try all API keys in rotation
                for (int keyAttempt = 0; keyAttempt < GEMINI_API_KEYS.length && !success; keyAttempt++) {
                    String apiKey = GEMINI_API_KEYS[currentKeyIndex];

                    try {
                        URL url = new URL(
                                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                                        + apiKey);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);
                        conn.setConnectTimeout(30000);
                        conn.setReadTimeout(30000);

                        android.util.Log.d("GeminiAI",
                                "Sending request to Gemini API (" + modelName + ") with key #" + (currentKeyIndex + 1)
                                        + "...");

                        OutputStream os = conn.getOutputStream();
                        os.write(requestJson.toString().getBytes());
                        os.flush();
                        os.close();

                        int responseCode = conn.getResponseCode();
                        android.util.Log.d("GeminiAI",
                                "Response code: " + responseCode + " (Key #" + (currentKeyIndex + 1) + ")");

                        if (responseCode == 200) {
                            android.util.Log.d("GeminiAI", "Success! Reading response...");
                            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                response.append(line);
                            }
                            br.close();

                            android.util.Log.d("GeminiAI", "Response: " + response.toString());

                            JSONObject responseJson = new JSONObject(response.toString());
                            aiAnswerText = responseJson.getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");

                            android.util.Log.d("GeminiAI", "AI answer generated successfully");
                            success = true;
                        } else if (responseCode == 429) {
                            // Quota exceeded, try next key
                            android.util.Log.d("GeminiAI",
                                    "Quota exceeded for key #" + (currentKeyIndex + 1) + ", switching to next key");
                            currentKeyIndex = (currentKeyIndex + 1) % GEMINI_API_KEYS.length;
                        } else {
                            // Other error
                            android.util.Log.e("GeminiAI", "API returned error code: " + responseCode);
                            break;
                        }
                    } catch (Exception e) {
                        android.util.Log.e("GeminiAI",
                                "Error with key #" + (currentKeyIndex + 1) + ": " + e.getMessage());
                        if (keyAttempt < GEMINI_API_KEYS.length - 1) {
                            currentKeyIndex = (currentKeyIndex + 1) % GEMINI_API_KEYS.length;
                        }
                    }
                }

                // Use fallback if all keys failed
                if (aiAnswerText == null) {
                    aiAnswerText = "Thank you for your question about '" + questionText + "'. " +
                            "While I'm currently unable to provide a detailed response, " +
                            "our expert team will review your question and provide comprehensive guidance soon. " +
                            "In the meantime, ensure proper care and monitoring of your crops.";
                }

                // Save AI answer to database
                com.agriminds.data.entity.ExpertAnswer aiAnswer = new com.agriminds.data.entity.ExpertAnswer();
                aiAnswer.setQuestionId(questionId);
                aiAnswer.setExpertId(0); // AI has ID 0
                aiAnswer.setExpertName("AI Assistant");
                aiAnswer.setAnswerText(aiAnswerText);
                aiAnswer.setAnswerType("AI");
                aiAnswer.setCreatedAt(System.currentTimeMillis());

                // Insert answer first
                long answerId = database.expertAnswerDao().insertAnswer(aiAnswer);

                // Generate voice for AI answer if question has voice
                if (originalQuestion != null && originalQuestion.getAudioPath() != null
                        && !originalQuestion.getAudioPath().isEmpty()) {
                    // Generate TTS audio asynchronously and update the answer
                    String finalAiAnswerText = aiAnswerText;
                    runOnUiThread(() -> {
                        generateTTSAudioAsync(finalAiAnswerText, questionId, (int) answerId);
                    });
                }

                database.questionDao().incrementAnswerCount(questionId);

                android.util.Log.d("GeminiAI", "AI answer saved to database");

            } catch (Exception e) {
                android.util.Log.e("GeminiAI", "Error generating AI answer: " + e.getMessage(), e);

                // Save fallback answer on error
                try {
                    com.agriminds.data.entity.ExpertAnswer aiAnswer = new com.agriminds.data.entity.ExpertAnswer();
                    aiAnswer.setQuestionId(questionId);
                    aiAnswer.setExpertId(0);
                    aiAnswer.setExpertName("AI Assistant");
                    aiAnswer.setAnswerText("Your question has been received. Our AI system is currently processing, " +
                            "and expert farmers will provide detailed guidance soon. Please check back shortly.");
                    aiAnswer.setAnswerType("AI");
                    aiAnswer.setCreatedAt(System.currentTimeMillis());

                    database.expertAnswerDao().insertAnswer(aiAnswer);
                    database.questionDao().incrementAnswerCount(questionId);
                } catch (Exception ex) {
                    android.util.Log.e("GeminiAI", "Failed to save fallback answer: " + ex.getMessage());
                }
            }
        });

    }

    private byte[] readAudioFile(File audioFile) throws Exception {
        java.io.FileInputStream fis = new java.io.FileInputStream(audioFile);
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            bos.write(buffer, 0, bytesRead);
        }
        fis.close();
        return bos.toByteArray();
    }

    private String generateTTSAudio(String text, int questionId) {
        try {
            File audioDir = new File(getFilesDir(), "audio");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }

            String audioPath = new File(audioDir, "ai_answer_" + questionId + "_" + System.currentTimeMillis() + ".wav")
                    .getAbsolutePath();

            // Use TTS to generate audio file
            final android.speech.tts.TextToSpeech[] ttsHolder = new android.speech.tts.TextToSpeech[1];
            ttsHolder[0] = new android.speech.tts.TextToSpeech(this, status -> {
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    ttsHolder[0].setLanguage(new java.util.Locale("bn", "BD"));
                    ttsHolder[0].setSpeechRate(0.85f);

                    // Synthesize to file
                    ttsHolder[0].synthesizeToFile(text, null, new File(audioPath), "ai_tts_" + questionId);
                    ttsHolder[0].shutdown();
                }
            });

            return audioPath;
        } catch (Exception e) {
            android.util.Log.e("AskExpertActivity", "Failed to generate TTS audio: " + e.getMessage());
            return null;
        }
    }

    private void generateTTSAudioAsync(String text, int questionId, int answerId) {
        try {
            File audioDir = new File(getFilesDir(), "audio");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }

            String audioPath = new File(audioDir, "ai_answer_" + questionId + "_" + System.currentTimeMillis() + ".wav")
                    .getAbsolutePath();

            // Use TTS to generate audio file
            final android.speech.tts.TextToSpeech[] ttsHolder = new android.speech.tts.TextToSpeech[1];
            ttsHolder[0] = new android.speech.tts.TextToSpeech(this, status -> {
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    ttsHolder[0].setLanguage(new java.util.Locale("bn", "BD"));
                    ttsHolder[0].setSpeechRate(0.85f);

                    // Synthesize to file
                    int result = ttsHolder[0].synthesizeToFile(text, null, new File(audioPath), "ai_tts_" + questionId);

                    if (result == android.speech.tts.TextToSpeech.SUCCESS) {
                        // Update the answer with audio path
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            com.agriminds.data.entity.ExpertAnswer answer = database.expertAnswerDao()
                                    .getAnswerById(answerId);
                            if (answer != null) {
                                answer.setAudioPath(audioPath);
                                database.expertAnswerDao().updateAnswer(answer);
                                android.util.Log.d("AskExpertActivity", "AI voice answer generated: " + audioPath);
                            }
                        });
                    }

                    ttsHolder[0].shutdown();
                }
            });
        } catch (Exception e) {
            android.util.Log.e("AskExpertActivity", "Failed to generate TTS audio: " + e.getMessage());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
