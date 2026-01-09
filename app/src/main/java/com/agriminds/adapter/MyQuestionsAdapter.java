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
            "AIzaSyCsP16njQFn5rXz02jHsM1_QJBH4dDWq6M",
            "AIzaSyDdqGQy-FneDjZ4IaWBIELpRrWdPNnFW5A",
            "AIzaSyD9UdEZSlZR5Q8jl8n4DfxoLTj5jSz4t_E"
    };
    private static int currentKeyIndex = 0;

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
            Glide.with(context)
                    .load(question.getImageUrl())
                    .into(holder.ivQuestionImage);
        } else {
            holder.ivQuestionImage.setVisibility(View.GONE);
        }

        // Show voice play button if question has audio
        if (question.getAudioPath() != null && !question.getAudioPath().isEmpty()) {
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

        // View Answers button
        holder.btnViewAnswers.setOnClickListener(v -> {
            Intent intent = new Intent(context, AnswersActivity.class);
            intent.putExtra("questionId", question.getId());
            intent.putExtra("questionText", question.getQuestionText());
            context.startActivity(intent);
        });

        // AI Answer button
        holder.btnGetAIAnswer.setOnClickListener(v -> {
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

                // Check if this is a voice question
                if ("[Voice Question]".equals(question.getQuestionText())) {
                    // Voice question without transcription - provide general advice
                    android.util.Log.d("GeminiAI", "Voice question detected (no transcription available)");

                    JSONObject textPart = new JSONObject();
                    textPart.put("text",
                            "You are an agricultural expert assistant. A farmer has submitted a voice question but the speech-to-text transcription is not available. "
                                    +
                                    "Provide helpful, general agricultural advice covering common topics like: " +
                                    "1) Proper irrigation and water management, " +
                                    "2) Common pest identification and control, " +
                                    "3) Soil health and fertilization basics, " +
                                    "4) Seasonal crop care tips. " +
                                    "Keep the answer concise (4-6 sentences), actionable, and suitable for farmers. " +
                                    "Encourage them to consult with local experts for specific issues.");
                    parts.put(textPart);
                } else {
                    // Text-based question
                    JSONObject textPart = new JSONObject();
                    String promptText = "You are an agricultural expert assistant. A farmer has asked: \""
                            + question.getQuestionText()
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
                    aiAnswerText = "Thank you for your question about '" + question.getQuestionText() + "'. " +
                            "While I'm currently unable to provide a detailed response, " +
                            "our expert team will review your question and provide comprehensive guidance soon. " +
                            "In the meantime, ensure proper care and monitoring of your crops.";
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
