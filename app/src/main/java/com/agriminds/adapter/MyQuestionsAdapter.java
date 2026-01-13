package com.agriminds.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.AnswersActivity;
import com.agriminds.R;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.Question;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MyQuestionsAdapter extends RecyclerView.Adapter<MyQuestionsAdapter.ViewHolder> {

    private static final String[] GEMINI_API_KEYS = {
            "AIzaSyC90368I-saYiOPOd9DH5Ean3NNS1K8RQo",
            "AIzaSyAx-IxNj1EbbOAKIiD8PiTF6t6EhQml4zY",
            "AIzaSyDvTbDitI61UdRrMMkLO2GXc8VrNSUeK9M"
    };
    private static int currentKeyIndex = 0;
    private static final int DAILY_QUOTA_LIMIT = 60; // gemini-2.5-flash: 20/day per key × 3 keys
    private static final String QUOTA_PREFS = "gemini_quota";
    private static final String QUOTA_COUNT_KEY = "request_count";
    private static final String QUOTA_DATE_KEY = "last_reset_date";

    private List<Question> questions;
    private Context context;
    private int currentUserId;
    private AppDatabase database;
    private com.agriminds.utils.AudioPlayerManager audioPlayerManager;

    public MyQuestionsAdapter(List<Question> questions, Context context, int currentUserId) {
        this.questions = questions;
        this.context = context;
        this.currentUserId = currentUserId;
        this.database = AppDatabase.getInstance(context);
        this.audioPlayerManager = new com.agriminds.utils.AudioPlayerManager();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_question, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Question question = questions.get(position);

        holder.tvCategory.setText(question.getCategory());
        holder.tvQuestionText.setText(question.getQuestionText());
        holder.tvDate.setText(getRelativeTime(question.getCreatedAt()));

        // Show answer count
        holder.tvAnswerCount.setText(question.getAnswerCount() + " answers");

        // Check for unviewed answers
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int unviewedCount = database.answerViewDao()
                    .getUnviewedAnswerCountForQuestion(question.getId(), currentUserId);
            ((android.app.Activity) context).runOnUiThread(() -> {
                if (unviewedCount > 0) {
                    holder.tvNewAnswerBadge.setVisibility(View.VISIBLE);
                } else {
                    holder.tvNewAnswerBadge.setVisibility(View.GONE);
                }
            });
        });

        // Load image if exists
        if (question.getImageUrl() != null && !question.getImageUrl().isEmpty()) {
            holder.ivQuestionImage.setVisibility(View.VISIBLE);
            // Check if it's a local file path or URL
            if (question.getImageUrl().startsWith("http")) {
                Glide.with(context)
                        .load(question.getImageUrl())
                        .into(holder.ivQuestionImage);
            } else {
                // Local file path
                Glide.with(context)
                        .load(new java.io.File(question.getImageUrl()))
                        .into(holder.ivQuestionImage);
            }
        } else {
            holder.ivQuestionImage.setVisibility(View.GONE);
        }

        // Show voice play button if question has audio
        if (question.getAudioPath() != null && !question.getAudioPath().isEmpty()) {
            // Check if audio file actually exists
            File audioFile = new File(question.getAudioPath());
            if (audioFile.exists()) {
                holder.btnPlayMyVoice.setVisibility(View.VISIBLE);
                holder.btnPlayMyVoice.setOnClickListener(v -> {
                    holder.btnPlayMyVoice.setText("⏸️ Playing...");
                    audioPlayerManager.play(question.getAudioPath(), () -> {
                        ((android.app.Activity) context)
                                .runOnUiThread(() -> holder.btnPlayMyVoice.setText("▶️ Play My Voice"));
                    });
                });
            } else {
                holder.btnPlayMyVoice.setVisibility(View.GONE);
            }
        } else {
            holder.btnPlayMyVoice.setVisibility(View.GONE);
        }

        // View Answers button
        holder.btnViewAnswers.setOnClickListener(v -> {
            Intent intent = new Intent(context, AnswersActivity.class);
            intent.putExtra("questionId", question.getId());
            intent.putExtra("questionText", question.getQuestionText());
            context.startActivity(intent);
        });

        // AI Answer button
        holder.btnGetAIAnswer.setOnClickListener(v -> {
            int used = getTodayRequestCount();
            int remaining = getRemainingQuota();
            Toast.makeText(context,
                    "AI Quota: " + used + "/" + DAILY_QUOTA_LIMIT + " used | " + remaining + " remaining",
                    Toast.LENGTH_SHORT).show();
            generateAIAnswer(question);
        });

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            deleteQuestion(question, position);
        });
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    // Quota tracking methods
    private int getTodayRequestCount() {
        android.content.SharedPreferences prefs = context.getSharedPreferences(QUOTA_PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String lastDate = prefs.getString(QUOTA_DATE_KEY, "");

        if (!today.equals(lastDate)) {
            // New day, reset counter
            prefs.edit()
                    .putInt(QUOTA_COUNT_KEY, 0)
                    .putString(QUOTA_DATE_KEY, today)
                    .apply();
            return 0;
        }

        return prefs.getInt(QUOTA_COUNT_KEY, 0);
    }

    private void incrementRequestCount() {
        android.content.SharedPreferences prefs = context.getSharedPreferences(QUOTA_PREFS, Context.MODE_PRIVATE);
        int currentCount = getTodayRequestCount();
        prefs.edit().putInt(QUOTA_COUNT_KEY, currentCount + 1).apply();
        android.util.Log.d("GeminiQuota", "API requests today: " + (currentCount + 1) + "/" + DAILY_QUOTA_LIMIT);
    }

    private int getRemainingQuota() {
        return DAILY_QUOTA_LIMIT - getTodayRequestCount();
    }

    public void updateQuestions(List<Question> newQuestions) {
        this.questions = newQuestions;
        notifyDataSetChanged();
    }

    private void deleteQuestion(Question question, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Question")
                .setMessage(
                        "Delete this question? Expert answers and ratings will be preserved for their statistics.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        // Delete replies only
                        List<com.agriminds.data.entity.Reply> replies = database.replyDao()
                                .getRepliesByQuestion(question.getId());
                        for (com.agriminds.data.entity.Reply reply : replies) {
                            database.replyDao().deleteReply(reply);
                        }

                        // Keep expert answers and ratings intact - this preserves expert statistics
                        // Only delete the question itself
                        database.questionDao().deleteQuestion(question);

                        ((android.app.Activity) context).runOnUiThread(() -> {
                            questions.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, questions.size());
                            Toast.makeText(context, "Question deleted. Expert statistics preserved.",
                                    Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getRelativeTime(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0)
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        if (hours > 0)
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        if (minutes > 0)
            return minutes + " min" + (minutes > 1 ? "s" : "") + " ago";
        return "Just now";
    }

    private void generateAIAnswer(Question question) {
        Toast.makeText(context, "Generating AI answer...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                android.util.Log.d("GeminiAI", "Generating AI answer for question: " + question.getQuestionText());

                // Check if AI answer already exists
                List<com.agriminds.data.entity.ExpertAnswer> existingAnswers = database.expertAnswerDao()
                        .getAnswersByQuestion(question.getId());

                boolean hasAIAnswer = false;
                for (com.agriminds.data.entity.ExpertAnswer answer : existingAnswers) {
                    if ("AI".equals(answer.getAnswerType())) {
                        hasAIAnswer = true;
                        break;
                    }
                }

                if (hasAIAnswer) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "AI answer already exists", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Create JSON request
                JSONObject requestJson = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();

                // Always use the actual question text (from typing or voice-to-text)
                String actualQuestionText = question.getQuestionText();

                // Create the prompt based on the actual question text
                JSONObject textPart = new JSONObject();
                String promptText = "You are a friendly agricultural expert assistant. A farmer has asked: \""
                        + actualQuestionText
                        + "\"\n\n" +
                        "Instructions:\n" +
                        "1. If this is a greeting (like 'hello', 'hi', 'how are you', 'kemon acho', etc.), respond warmly and ask how you can help them today.\n"
                        +
                        "2. If this is a casual question about yourself, respond politely and redirect to agricultural topics.\n"
                        +
                        "3. If this is an agricultural question, provide helpful, practical advice about farming, crops, pest control, or agricultural techniques.\n"
                        +
                        "4. IMPORTANT LANGUAGE RULES:\n" +
                        "   - If question is in English → Respond in English\n" +
                        "   - If question is in Bengali script (বাংলা) → Respond in Bengali script\n" +
                        "   - If question is in Banglish (Bengali words written in English letters like 'kivabe', 'kemon', 'dhan chash') → Respond in Bengali script (বাংলা)\n"
                        +
                        "   - NEVER respond in Banglish, only understand it\n" +
                        "5. Keep answers concise (3-5 sentences), friendly, and actionable for farmers.\n" +
                        "6. For greetings and casual chat, keep it brief (1-2 sentences) before asking how to help.";
                textPart.put("text", promptText);
                parts.put(textPart);

                android.util.Log.d("GeminiAI", "Using question text: " + actualQuestionText);

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
                                "Sending request to Gemini API with key #" + (currentKeyIndex + 1) + "...");

                        OutputStream os = conn.getOutputStream();
                        os.write(requestJson.toString().getBytes());
                        os.flush();
                        os.close();

                        int responseCode = conn.getResponseCode();
                        android.util.Log.d("GeminiAI",
                                "Response code: " + responseCode + " (Key #" + (currentKeyIndex + 1) + ")");

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

                            android.util.Log.d("GeminiAI", "AI answer generated successfully: " + aiAnswerText);

                            // Increment quota counter on success
                            incrementRequestCount();

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
                    int requestsToday = getTodayRequestCount();
                    int remaining = getRemainingQuota();

                    aiAnswerText = "⚠️ AI Service Temporarily Unavailable\n\n" +
                            "Daily quota reached (" + requestsToday + "/" + DAILY_QUOTA_LIMIT + " requests used).\n" +
                            "Your question about '" + question.getQuestionText() + "' has been saved and " +
                            "our agricultural experts will provide a detailed answer soon.\n\n" +
                            "✨ AI service resets at 2:00 PM Bangladesh time daily.\n" +
                            "Thank you for your patience!";
                    android.util.Log.w("GeminiAI", "All API keys exhausted (" + requestsToday + "/" + DAILY_QUOTA_LIMIT
                            + ") - using fallback message");
                }

                // Save AI answer to database
                com.agriminds.data.entity.ExpertAnswer aiAnswer = new com.agriminds.data.entity.ExpertAnswer();
                aiAnswer.setQuestionId(question.getId());
                aiAnswer.setExpertId(0); // AI has ID 0
                aiAnswer.setExpertName("AI Assistant");
                aiAnswer.setAnswerText(aiAnswerText);
                aiAnswer.setAnswerType("AI");
                aiAnswer.setCreatedAt(System.currentTimeMillis());

                // Insert answer first
                long answerId = database.expertAnswerDao().insertAnswer(aiAnswer);

                // Generate voice for AI answer if question has voice
                if (question.getAudioPath() != null && !question.getAudioPath().isEmpty()) {
                    // Generate TTS audio for AI answer
                    String finalAiAnswerText = aiAnswerText;
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        generateTTSAudioAsync(finalAiAnswerText, question.getId(), (int) answerId);
                    });
                }

                database.questionDao().incrementAnswerCount(question.getId());

                android.util.Log.d("GeminiAI", "AI answer saved to database");

                ((android.app.Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "AI answer generated!", Toast.LENGTH_SHORT).show();
                    // Refresh the list
                    notifyDataSetChanged();
                });

            } catch (Exception e) {
                android.util.Log.e("GeminiAI", "Error generating AI answer: " + e.getMessage(), e);
                ((android.app.Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Failed to generate AI answer. Please try again.", Toast.LENGTH_SHORT)
                            .show();
                });
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

    private void generateTTSAudioAsync(String answerText, int questionId, int answerId) {
        // Initialize TTS
        final android.speech.tts.TextToSpeech[] ttsHolder = new android.speech.tts.TextToSpeech[1];

        ttsHolder[0] = new android.speech.tts.TextToSpeech(context, status -> {
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                // Set Bengali language
                int result = ttsHolder[0].setLanguage(new Locale("bn", "BD"));

                if (result == android.speech.tts.TextToSpeech.LANG_MISSING_DATA ||
                        result == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                    android.util.Log.e("TTS", "Bengali language not supported, using default");
                } else {
                    // Set speech rate
                    ttsHolder[0].setSpeechRate(0.85f);

                    // Generate audio file
                    File audioDir = new File(context.getFilesDir(), "audio");
                    if (!audioDir.exists()) {
                        audioDir.mkdirs();
                    }

                    String audioFileName = "ai_answer_" + answerId + "_" + System.currentTimeMillis() + ".wav";
                    File audioFile = new File(audioDir, audioFileName);

                    android.speech.tts.UtteranceProgressListener progressListener = new android.speech.tts.UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            android.util.Log.d("TTS", "Started generating TTS audio");
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            android.util.Log.d("TTS", "TTS audio generated: " + audioFile.getAbsolutePath());

                            // Update answer with audio path
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                database.expertAnswerDao().updateAnswerAudioPath(answerId, audioFile.getAbsolutePath());
                            });

                            // Shutdown TTS
                            ttsHolder[0].shutdown();
                        }

                        @Override
                        public void onError(String utteranceId) {
                            android.util.Log.e("TTS", "Error generating TTS audio");
                            ttsHolder[0].shutdown();
                        }
                    };

                    ttsHolder[0].setOnUtteranceProgressListener(progressListener);

                    // Synthesize to file
                    ttsHolder[0].synthesizeToFile(answerText, null, audioFile, "tts_" + answerId);
                }
            } else {
                android.util.Log.e("TTS", "TTS initialization failed");
            }
        });
    }

    private String transcribeAudio(File audioFile) {
        try {
            android.util.Log.d("Transcription", "Starting audio transcription...");

            // Read audio file
            byte[] audioBytes = readAudioFile(audioFile);
            String base64Audio = android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP);

            // Use Google Cloud Speech-to-Text API
            String apiKey = GEMINI_API_KEYS[0]; // Reuse Gemini API key for Google services
            URL url = new URL("https://speech.googleapis.com/v1/speech:recognize?key=" + apiKey);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            // Create request JSON
            JSONObject requestJson = new JSONObject();
            JSONObject config = new JSONObject();
            config.put("encoding", "LINEAR16");
            config.put("sampleRateHertz", 16000);
            config.put("languageCode", "en-US"); // Change to "bn-BD" for Bengali if needed
            config.put("enableAutomaticPunctuation", true);

            JSONObject audio = new JSONObject();
            audio.put("content", base64Audio);

            requestJson.put("config", config);
            requestJson.put("audio", audio);

            android.util.Log.d("Transcription", "Sending request to Google Speech API...");

            OutputStream os = conn.getOutputStream();
            os.write(requestJson.toString().getBytes());
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            android.util.Log.d("Transcription", "Response code: " + responseCode);

            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                android.util.Log.d("Transcription", "Response: " + response.toString());

                JSONObject responseJson = new JSONObject(response.toString());
                if (responseJson.has("results") && responseJson.getJSONArray("results").length() > 0) {
                    String transcript = responseJson.getJSONArray("results")
                            .getJSONObject(0)
                            .getJSONArray("alternatives")
                            .getJSONObject(0)
                            .getString("transcript");

                    android.util.Log.d("Transcription", "Successfully transcribed: " + transcript);
                    return transcript;
                }
            } else {
                android.util.Log.e("Transcription", "API error: " + responseCode);
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    errorResponse.append(line);
                }
                br.close();
                android.util.Log.e("Transcription", "Error response: " + errorResponse.toString());
            }

        } catch (Exception e) {
            android.util.Log.e("Transcription", "Error transcribing audio: " + e.getMessage(), e);
        }

        return null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvQuestionText, tvDate, tvAnswerCount, tvNewAnswerBadge;
        ImageView ivQuestionImage;
        Button btnViewAnswers, btnGetAIAnswer, btnDelete;
        com.google.android.material.button.MaterialButton btnPlayMyVoice;

        ViewHolder(View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvQuestionText = itemView.findViewById(R.id.tvQuestionText);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAnswerCount = itemView.findViewById(R.id.tvAnswerCount);
            tvNewAnswerBadge = itemView.findViewById(R.id.tvNewAnswerBadge);
            ivQuestionImage = itemView.findViewById(R.id.ivQuestionImage);
            btnViewAnswers = itemView.findViewById(R.id.btnViewAnswers);
            btnGetAIAnswer = itemView.findViewById(R.id.btnGetAIAnswer);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnPlayMyVoice = itemView.findViewById(R.id.btnPlayMyVoice);
        }
    }
}
