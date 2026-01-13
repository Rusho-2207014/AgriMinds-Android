package com.agriminds.ui.askexpert;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.agriminds.R;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.Question;
import com.agriminds.utils.AudioPlayerManager;
import com.agriminds.utils.AudioRecorder;
import com.agriminds.utils.VoiceRecorder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class AskExpertActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;

    // Multiple API keys for rotation (60+ requests/day instead of 20)
    private static final String[] GEMINI_API_KEYS = {
            "AIzaSyC90368I-saYiOPOd9DH5Ean3NNS1K8RQo",
            "AIzaSyAx-IxNj1EbbOAKIiD8PiTF6t6EhQml4zY",
            "AIzaSyDvTbDitI61UdRrMMkLO2GXc8VrNSUeK9M"
    };
    private static int currentKeyIndex = 0;

    // AssemblyAI API key for speech-to-text (free tier: 5 hours/month)
    private static final String ASSEMBLYAI_API_KEY = "8e4e7c3d0a8d4c4d9f3e2b1a5c6d7e8f";

    private TextInputEditText editName, editQuestion;
    private Spinner spinnerCategory;
    private Button btnSubmit;
    private MaterialButton btnRecordVoice, btnPlayVoice, btnCamera, btnGallery, btnVoiceToText;
    private TextView tvRecordingStatus, tvImageStatus, tvVoiceToTextStatus;
    private ImageView ivQuestionPreview;
    private AppDatabase database;
    private int currentUserId;
    private String currentUserName;
    private VoiceRecorder voiceRecorder; // For voice-to-text
    private AudioRecorder audioRecorder; // For audio recording
    private AudioPlayerManager audioPlayerManager;
    private String currentAudioPath = null;
    private String currentImagePath = null;
    private Uri cameraImageUri = null;
    private File cameraImageFile = null; // Store the actual file reference

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

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
        btnVoiceToText = findViewById(R.id.btn_voice_to_text);
        tvVoiceToTextStatus = findViewById(R.id.tv_voice_to_text_status);
        btnCamera = findViewById(R.id.btn_camera);
        btnGallery = findViewById(R.id.btn_gallery);
        tvImageStatus = findViewById(R.id.tv_image_status);
        ivQuestionPreview = findViewById(R.id.iv_question_preview);

        // Initialize image launchers
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Process image in background to avoid slowing down UI
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                if (cameraImageFile != null && cameraImageFile.exists()) {
                                    // Decode with smaller size to improve performance
                                    BitmapFactory.Options options = new BitmapFactory.Options();
                                    options.inSampleSize = 2; // Load at 50% size
                                    Bitmap bitmap = BitmapFactory.decodeFile(cameraImageFile.getAbsolutePath(),
                                            options);

                                    runOnUiThread(() -> {
                                        ivQuestionPreview.setImageBitmap(bitmap);
                                        ivQuestionPreview.setVisibility(ImageView.VISIBLE);
                                        currentImagePath = cameraImageFile.getAbsolutePath();
                                        tvImageStatus.setText("✓ Photo captured");
                                        tvImageStatus.setVisibility(TextView.VISIBLE);
                                        Log.d("AskExpertActivity", "Image saved at: " + currentImagePath);
                                    });
                                } else {
                                    runOnUiThread(() -> Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT)
                                            .show());
                                }
                            } catch (Exception e) {
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT)
                                            .show();
                                    Log.e("AskExpertActivity", "Camera result error", e);
                                });
                            }
                        });
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Process image in background to avoid slowing down UI
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                Uri imageUri = result.getData().getData();
                                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                                File imageFile = new File(getFilesDir(),
                                        "images/question_" + System.currentTimeMillis() + ".jpg");
                                imageFile.getParentFile().mkdirs();

                                FileOutputStream outputStream = new FileOutputStream(imageFile);
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, bytesRead);
                                }
                                outputStream.close();
                                inputStream.close();

                                // Decode with smaller size to improve performance
                                BitmapFactory.Options options = new BitmapFactory.Options();
                                options.inSampleSize = 2; // Load at 50% size
                                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

                                runOnUiThread(() -> {
                                    ivQuestionPreview.setImageBitmap(bitmap);
                                    ivQuestionPreview.setVisibility(ImageView.VISIBLE);
                                    currentImagePath = imageFile.getAbsolutePath();
                                    tvImageStatus.setText("✓ Image selected");
                                    tvImageStatus.setVisibility(TextView.VISIBLE);
                                    Log.d("AskExpertActivity", "Gallery image saved at: " + currentImagePath);
                                });
                            } catch (Exception e) {
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT)
                                            .show();
                                    Log.e("AskExpertActivity", "Gallery result error", e);
                                });
                            }
                        });
                    }
                });

        // Initialize audio player
        audioPlayerManager = new AudioPlayerManager();

        // Initialize audio recorder for voice messages
        audioRecorder = new AudioRecorder(new AudioRecorder.AudioRecorderListener() {
            @Override
            public void onRecordingStarted(String filePath) {
                runOnUiThread(() -> {
                    btnRecordVoice.setText("⏹️ Stop Recording");
                    btnPlayVoice.setVisibility(MaterialButton.GONE);
                    tvRecordingStatus.setText("🔴 Recording audio...");
                    tvRecordingStatus.setVisibility(TextView.VISIBLE);
                });
            }

            @Override
            public void onRecordingStopped(String filePath) {
                runOnUiThread(() -> {
                    currentAudioPath = filePath;
                    btnRecordVoice.setText("🎤 Record Voice");
                    btnPlayVoice.setVisibility(MaterialButton.VISIBLE);
                    tvRecordingStatus.setText("✅ Audio recorded");
                    tvRecordingStatus.setVisibility(TextView.VISIBLE);
                    Toast.makeText(AskExpertActivity.this, "Voice message saved", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnRecordVoice.setText("🎤 Record Voice");
                    tvRecordingStatus.setText("❌ Recording failed");
                    tvRecordingStatus.setVisibility(TextView.VISIBLE);
                    Toast.makeText(AskExpertActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });

        // Initialize voice-to-text converter
        voiceRecorder = new VoiceRecorder(this,
                new VoiceRecorder.VoiceRecorderListener() {
                    @Override
                    public void onRecordingStarted() {
                        runOnUiThread(() -> {
                            btnVoiceToText.setText("⏹️ Stop Listening");
                            tvVoiceToTextStatus.setText("🎤 Listening... Speak clearly");
                            tvVoiceToTextStatus.setVisibility(TextView.VISIBLE);
                            Toast.makeText(AskExpertActivity.this, "Speak now - text will appear below",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onRecordingStopped(String audioPath) {
                        runOnUiThread(() -> {
                            btnVoiceToText.setText("🗣️ Voice to Text");
                            tvVoiceToTextStatus.setText("✅ Voice converted to text");
                            tvVoiceToTextStatus.setVisibility(TextView.VISIBLE);
                        });
                    }

                    @Override
                    public void onTextRecognized(String text) {
                        runOnUiThread(() -> {
                            editQuestion.setText(text);
                            tvVoiceToTextStatus.setText("✅ Recognized: " + text);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Log.e("AskExpertActivity", "Voice error: " + error);
                            btnVoiceToText.setText("🗣️ Voice to Text");
                            tvVoiceToTextStatus.setText("❌ " + error);
                            tvVoiceToTextStatus.setVisibility(TextView.VISIBLE);
                            Toast.makeText(AskExpertActivity.this, "Error: " + error,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });

        // Pre-fill farmer name if logged in
        if (!currentUserName.isEmpty()) {
            editName.setText(currentUserName);
        }

        // Record Voice button - for audio messages
        btnRecordVoice.setOnClickListener(v -> {
            if (audioRecorder.isRecording()) {
                audioRecorder.stopRecording();
            } else {
                // Start new audio recording
                if (checkPermissions()) {
                    audioRecorder.startRecording(this);
                } else {
                    requestPermissions();
                }
            }
        });

        // Voice to Text button - for speech-to-text
        btnVoiceToText.setOnClickListener(v -> {
            if (voiceRecorder.isRecording()) {
                voiceRecorder.stopRecording();
            } else {
                // Start voice-to-text
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
                java.io.File audioFile = new java.io.File(
                        currentAudioPath);
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

        btnCamera.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                openCamera();
            } else {
                requestCameraPermission();
            }
        });

        btnGallery.setOnClickListener(v -> openGallery());

        btnSubmit.setOnClickListener(v -> submitQuestion());
    }

    private void openCamera() {
        try {
            cameraImageFile = new File(getFilesDir(), "images/question_" + System.currentTimeMillis() + ".jpg");
            cameraImageFile.getParentFile().mkdirs();
            cameraImageUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider", cameraImageFile);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(cameraIntent);
            Log.d("AskExpertActivity", "Opening camera, will save to: " + cameraImageFile.getAbsolutePath());
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("AskExpertActivity", "Camera error", e);
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    private boolean checkPermissions() {
        // Only check RECORD_AUDIO for voice recording
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        // Request ONLY audio permission for voice recording
        ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.RECORD_AUDIO },
                PERMISSION_REQUEST_CODE);
    }

    private void requestCameraPermission() {
        // Request camera permission separately
        ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.CAMERA },
                201);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Audio permission for voice recording
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Audio permission granted! Click record again.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Audio permission denied. Voice recording won't work.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 201) {
            // Camera permission
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceRecorder != null) {
            voiceRecorder.release();
        }
        if (audioRecorder != null) {
            audioRecorder.release();
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

        // If only voice, set placeholder text and inform user
        if (questionText.isEmpty() && currentAudioPath != null) {
            questionText = "[Voice Question]";
            Toast.makeText(this, "Voice question submitted. Please speak clearly next time for text recognition.",
                    Toast.LENGTH_LONG).show();
        } else if (!questionText.isEmpty() && currentAudioPath != null) {
            // Both text and voice - text was recognized from voice
            Toast.makeText(this, "Submitting your question: " + questionText, Toast.LENGTH_SHORT).show();
        }

        // Save question to database
        final String finalQuestionText = questionText;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Question question = new Question();
            question.setFarmerId(currentUserId);
            question.setFarmerName(name);
            question.setCategory(category);
            question.setQuestionText(finalQuestionText);
            question.setImageUrl(currentImagePath != null ? currentImagePath : ""); // Save image path
            question.setAudioPath(currentAudioPath); // Save voice recording path
            question.setStatus("Pending");
            question.setAnswerCount(0);
            question.setCreatedAt(System.currentTimeMillis());

            long questionId = database.questionDao().insertQuestion(question);

            // Increment persistent question count
            database.userDao().incrementQuestionsAsked(currentUserId);

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

                    // AI answer generation disabled due to quota limits
                    // generateAIAnswer((int) questionId, finalQuestionText);
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
                    android.util.Log.d("GeminiAI", "Voice question detected - using general agricultural prompt");
                    // Note: Transcription happens during recording via SpeechRecognizer
                    // If user saved it as [Voice Question], the speech recognition didn't capture
                    // text
                    // So we'll provide general agricultural advice
                }

                // Create JSON request
                JSONObject requestJson = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();

                JSONObject textPart = new JSONObject();
                String promptText;
                if ("[Voice Question]".equals(actualQuestionText)) {
                    // Transcription failed - provide general advice in Bengali
                    promptText = "You are an agricultural expert assistant. A farmer has submitted a voice question but the speech-to-text transcription is not available. "
                            +
                            "Provide helpful, general agricultural advice in BENGALI language only. Cover topics like: "
                            +
                            "1) Proper irrigation, 2) Pest control, 3) Soil health. " +
                            "Keep the answer concise (4-6 sentences), actionable, and suitable for farmers.";
                } else {
                    // Use the transcribed or original question text
                    promptText = "You are an agricultural expert assistant. A farmer has asked: \"" + actualQuestionText
                            + "\"\n\n" +
                            "Provide a helpful, practical answer about agriculture, crop management, pest control, or farming techniques. "
                            +
                            "IMPORTANT: You MUST answer in BENGALI language, regardless of the language of the question. "
                            +
                            "Keep the answer concise (3-5 sentences), actionable, and suitable for farmers.";
                }
                textPart.put("text", promptText);
                parts.put(textPart);

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
                                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent?key="
                                        + apiKey);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);
                        conn.setConnectTimeout(30000);
                        conn.setReadTimeout(30000);

                        android.util.Log.d("GeminiAI",
                                "Sending request to Gemini API with key #" + (currentKeyIndex + 1) + "...");

                        OutputStream os = conn.getOutputStream();
                        os.write(requestJson.toString().getBytes());
                        os.flush();
                        os.close();

                        int responseCode = 0;
                        try {
                            responseCode = conn.getResponseCode();
                            android.util.Log.d("GeminiAI",
                                    "Response code: " + responseCode + " (Key #" + (currentKeyIndex + 1) + ")");
                        } catch (Exception e) {
                            android.util.Log.e("GeminiAI", "Failed to get response code: " + e.getMessage());
                            // Assume quota exceeded if we can't even get the response
                            currentKeyIndex = (currentKeyIndex + 1) % GEMINI_API_KEYS.length;
                            continue;
                        }

                        // Read error response if not successful
                        if (responseCode != 200) {
                            try {
                                BufferedReader errorReader = new BufferedReader(
                                        new InputStreamReader(conn.getErrorStream()));
                                StringBuilder errorResponse = new StringBuilder();
                                String errorLine;
                                while ((errorLine = errorReader.readLine()) != null) {
                                    errorResponse.append(errorLine);
                                }
                                errorReader.close();
                                android.util.Log.e("GeminiAI", "Error response: " + errorResponse.toString());
                            } catch (Exception ex) {
                                android.util.Log.e("GeminiAI", "Could not read error response");
                            }
                        }

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
                        } else if (responseCode == 403) {
                            // Forbidden - API key issue, try next key
                            android.util.Log.d("GeminiAI",
                                    "Forbidden for key #" + (currentKeyIndex + 1) + ", switching to next key");
                            currentKeyIndex = (currentKeyIndex + 1) % GEMINI_API_KEYS.length;
                        } else {
                            // Other error, try next key
                            android.util.Log.e("GeminiAI",
                                    "Error code " + responseCode + " for key #" + (currentKeyIndex + 1));
                            currentKeyIndex = (currentKeyIndex + 1) % GEMINI_API_KEYS.length;
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
                    aiAnswerText = "⚠️ AI Service Temporarily Unavailable\n\n" +
                            "All 3 API keys have reached their daily quota limit (60 requests/day).\n\n" +
                            "Your question \"" + questionText + "\" has been saved successfully. " +
                            "Our agricultural experts will provide answers soon.\n\n" +
                            "✨ AI service resets daily at 2:00 PM Bangladesh time.\n" +
                            "Thank you for your patience!";

                    android.util.Log.w("GeminiAI", "All API keys exhausted - using fallback message");

                    // Show user-friendly toast
                    runOnUiThread(() -> {
                        Toast.makeText(AskExpertActivity.this,
                                "AI quota limit reached. Your question is saved - experts will answer soon!",
                                Toast.LENGTH_LONG).show();
                    });
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

    private String transcribeAudioWithGemini(File audioFile) {
        try {
            android.util.Log.d("GeminiTranscription", "Starting audio transcription with Gemini...");

            // Read file to Base64
            byte[] fileBytes = new byte[(int) audioFile.length()];
            java.io.FileInputStream fis = new java.io.FileInputStream(audioFile);
            fis.read(fileBytes);
            fis.close();
            String base64Audio = android.util.Base64.encodeToString(fileBytes, android.util.Base64.NO_WRAP);

            // Construct JSON Request
            JSONObject requestJson = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();

            // Part 1: Text Prompt
            JSONObject textPart = new JSONObject();
            textPart.put("text",
                    "Please transcribe this audio file accurately. If it is in Bengali, transcribe in Bengali script. If English, in English. Output ONLY the transcription text, no other words.");
            parts.put(textPart);

            // Part 2: Audio Data
            JSONObject audioPart = new JSONObject();
            JSONObject inlineData = new JSONObject();
            inlineData.put("mime_type", "audio/mp4");
            inlineData.put("data", base64Audio);
            audioPart.put("inline_data", inlineData);
            parts.put(audioPart);

            content.put("parts", parts);
            contents.put(content);
            requestJson.put("contents", contents);

            String lastError = "Unknown Error";

            // Try with rotating keys
            for (int i = 0; i < GEMINI_API_KEYS.length; i++) {
                String apiKey = GEMINI_API_KEYS[currentKeyIndex];
                try {
                    URL url = new URL(
                            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent?key="
                                    + apiKey);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    OutputStream os = conn.getOutputStream();
                    os.write(requestJson.toString().getBytes());
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null)
                            response.append(line);
                        br.close();

                        JSONObject responseJson = new JSONObject(response.toString());
                        String text = responseJson.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        return text.trim();
                    } else {
                        // Capture detailed error response
                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        StringBuilder errorResponse = new StringBuilder();
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                        errorReader.close();

                        lastError = "Key " + i + " Failed (" + responseCode + "): " + errorResponse.toString();
                        android.util.Log.e("GeminiTranscription", lastError);

                        // Rotate key
                        currentKeyIndex = (currentKeyIndex + 1) % GEMINI_API_KEYS.length;
                    }
                } catch (Exception e) {
                    lastError = "Key " + i + " Error: " + e.getMessage();
                    android.util.Log.e("GeminiTranscription", lastError);
                    currentKeyIndex = (currentKeyIndex + 1) % GEMINI_API_KEYS.length;
                }
            }
            return "Error: All keys failed. Last: " + lastError;
        } catch (Exception e) {
            android.util.Log.e("GeminiTranscription", "Fatal Error: " + e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
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
