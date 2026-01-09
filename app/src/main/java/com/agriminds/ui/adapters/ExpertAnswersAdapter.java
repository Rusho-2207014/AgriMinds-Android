package com.agriminds.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.R;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.ExpertAnswer;
import com.agriminds.data.entity.ExpertRating;
import com.agriminds.data.entity.Reply;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ExpertAnswersAdapter extends RecyclerView.Adapter<ExpertAnswersAdapter.AnswerViewHolder> {

    // Multiple API keys for rotation (60+ requests/day instead of 20)
    private static final String[] GEMINI_API_KEYS = {
            "AIzaSyBgcPsiq9SfdmUuOb6KgQiwawOTSp1tXH0",
            "AIzaSyCBbcNjEG-7dlNWuHuPCgXMCjeoZZrrE8U",
            "AIzaSyD7scjKl6K1m0khEb5OSRlPN4NoBzwcFCs"
    };
    private static int currentKeyIndex = 0;
    private List<ExpertAnswer> answers;
    private SimpleDateFormat dateFormat;
    private AppDatabase database;
    private int currentUserId;
    private String currentUserName;
    private boolean isFarmer;
    private com.agriminds.utils.TranslationManager translationManager;
    private com.agriminds.utils.TTSManager ttsManager;
    private com.agriminds.utils.AudioPlayerManager audioPlayerManager;

    public ExpertAnswersAdapter(AppDatabase database, int currentUserId, String currentUserName, boolean isFarmer) {
        this.answers = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        this.database = database;
        this.currentUserId = currentUserId;
        this.currentUserName = currentUserName;
        this.isFarmer = isFarmer;
    }

    public void setManagers(com.agriminds.utils.TranslationManager translationManager,
            com.agriminds.utils.TTSManager ttsManager,
            com.agriminds.utils.AudioPlayerManager audioPlayerManager) {
        this.translationManager = translationManager;
        this.ttsManager = ttsManager;
        this.audioPlayerManager = audioPlayerManager;
    }

    @NonNull
    @Override
    public AnswerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_answer, parent, false);
        return new AnswerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnswerViewHolder holder, int position) {
        ExpertAnswer answer = answers.get(position);
        holder.bind(answer);
    }

    @Override
    public int getItemCount() {
        return answers.size();
    }

    public void setAnswers(List<ExpertAnswer> answers) {
        this.answers = answers != null ? answers : new ArrayList<>();
        notifyDataSetChanged();
    }

    class AnswerViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivExpertAvatar;
        private TextView tvExpertName;
        private TextView tvAIBadge;
        private TextView tvAnswerDate;
        private TextView tvAnswerText;
        private TextView tvBengaliTranslation;
        private RatingBar ratingBar;
        private TextView tvAverageRating;
        private TextView tvTapToChange;
        private LinearLayout layoutRatingSection;
        private LinearLayout layoutStarsContainer;
        private MaterialButton btnGiveRating;
        private LinearLayout layoutReplies;
        private TextView tvRepliesCount;
        private RecyclerView recyclerViewReplies;
        private TextInputEditText etReply;
        private MaterialButton btnSendReply;
        private MaterialButton btnDoneRating;
        private MaterialButton btnUndoRating;
        private MaterialButton btnHideTranslation;
        private RepliesAdapter repliesAdapter;
        private float pendingRating = 0;
        private boolean isSpeaking = false;
        private String lastTranslatedText = null;
        private String originalAnswerText = null;
        private android.os.Handler speechHandler = new android.os.Handler();
        private Runnable resetButtonRunnable;

        public AnswerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivExpertAvatar = itemView.findViewById(R.id.ivExpertAvatar);
            tvExpertName = itemView.findViewById(R.id.tvExpertName);
            tvAIBadge = itemView.findViewById(R.id.tvAIBadge);
            tvAnswerDate = itemView.findViewById(R.id.tvAnswerDate);
            tvAnswerText = itemView.findViewById(R.id.tvAnswerText);
            tvBengaliTranslation = itemView.findViewById(R.id.tvBengaliTranslation);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvAverageRating = itemView.findViewById(R.id.tvAverageRating);
            tvTapToChange = itemView.findViewById(R.id.tvTapToChange);
            layoutRatingSection = itemView.findViewById(R.id.layoutRatingSection);
            layoutStarsContainer = itemView.findViewById(R.id.layoutStarsContainer);
            btnGiveRating = itemView.findViewById(R.id.btnGiveRating);
            layoutReplies = itemView.findViewById(R.id.layoutReplies);
            tvRepliesCount = itemView.findViewById(R.id.tvRepliesCount);
            recyclerViewReplies = itemView.findViewById(R.id.recyclerViewReplies);
            etReply = itemView.findViewById(R.id.etReply);
            btnSendReply = itemView.findViewById(R.id.btnSendReply);
            btnDoneRating = itemView.findViewById(R.id.btnDoneRating);
            btnUndoRating = itemView.findViewById(R.id.btnUndoRating);
            btnHideTranslation = itemView.findViewById(R.id.btnHideTranslation);

            recyclerViewReplies.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            repliesAdapter = new RepliesAdapter();
            repliesAdapter.setManagers(translationManager, ttsManager);
            recyclerViewReplies.setAdapter(repliesAdapter);

            // Hide/Show translation button toggle handler
            btnHideTranslation.setOnClickListener(v -> {
                if (tvBengaliTranslation.getVisibility() == View.VISIBLE) {
                    // Hide translation
                    tvBengaliTranslation.setVisibility(View.GONE);
                    btnHideTranslation.setText("👀 Show Translation");
                } else {
                    // Show translation
                    tvBengaliTranslation.setVisibility(View.VISIBLE);
                    btnHideTranslation.setText("🙈 Hide Translation");
                }
            });
        }

        public void bind(ExpertAnswer answer) {
            tvExpertName.setText(answer.getExpertName());
            tvAnswerText.setText(answer.getAnswerText());
            originalAnswerText = answer.getAnswerText();
            tvAnswerDate.setText(getTimeAgo(answer.getCreatedAt()));

            // Reset Bengali translation and hide button (only show when Listen button is
            // clicked to save API quota)
            tvBengaliTranslation.setVisibility(View.GONE);
            tvBengaliTranslation.setText("");
            btnHideTranslation.setVisibility(View.GONE);

            // Reset state for recycled views
            isSpeaking = false;
            lastTranslatedText = null;

            // Show AI badge if it's an AI answer
            if ("AI".equals(answer.getAnswerType())) {
                tvAIBadge.setVisibility(View.VISIBLE);
            } else {
                tvAIBadge.setVisibility(View.GONE);
            }

            // Play Audio Button
            MaterialButton btnPlayAudio = itemView.findViewById(R.id.btnPlayAudio);
            if (answer.getAudioPath() != null && !answer.getAudioPath().isEmpty()) {
                btnPlayAudio.setVisibility(View.VISIBLE);
                btnPlayAudio.setOnClickListener(v -> {
                    if (audioPlayerManager != null) {
                        btnPlayAudio.setText("Playing...");
                        btnPlayAudio.setEnabled(false);

                        audioPlayerManager.play(answer.getAudioPath(), () -> {
                            ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                                btnPlayAudio.setText("▶ Play Voice");
                                btnPlayAudio.setEnabled(true);
                            });
                        });
                    }
                });
            } else {
                btnPlayAudio.setVisibility(View.GONE);
            }

            // Always show Listen button for both Bengali and English
            MaterialButton btnListen = itemView.findViewById(R.id.btnListen);
            // Clear existing click listener to prevent duplicates on recycled views
            btnListen.setOnClickListener(null);
            btnListen.setVisibility(View.VISIBLE);

            // Listen Button Click with toggle functionality
            btnListen.setOnClickListener(v -> {
                if (translationManager != null && ttsManager != null) {
                    // Check if TTS is ready
                    if (!ttsManager.isReady()) {
                        android.widget.Toast.makeText(itemView.getContext(),
                                "Text-to-speech not ready. Please install Bengali language pack in your device settings.",
                                android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }

                    // If currently speaking, stop it
                    if (isSpeaking) {
                        ttsManager.stop();
                        isSpeaking = false;
                        btnListen.setText("🔊 Listen in Bangla");
                        btnListen.setEnabled(true);

                        // Cancel pending reset
                        if (resetButtonRunnable != null) {
                            speechHandler.removeCallbacks(resetButtonRunnable);
                        }
                        return;
                    }

                    // If text is already in Bengali, speak it directly without translation
                    if (isBengaliText(answer.getAnswerText())) {
                        btnListen.setText("⏹️ Stop Speaking");
                        btnListen.setEnabled(true);
                        isSpeaking = true;

                        // Speak Bengali text directly without showing translation
                        ttsManager.speak(answer.getAnswerText(), new com.agriminds.utils.TTSManager.OnSpeakListener() {
                            @Override
                            public void onStart() {
                                // Speech started successfully
                            }

                            @Override
                            public void onError(String error) {
                                android.widget.Toast.makeText(itemView.getContext(),
                                        error, android.widget.Toast.LENGTH_LONG).show();
                                btnListen.setText("🔊 Listen in Bangla");
                                btnListen.setEnabled(true);
                                isSpeaking = false;
                            }
                        });
                        return;
                    }

                    // If we already have translation, just speak it again
                    if (lastTranslatedText != null && !lastTranslatedText.isEmpty()) {
                        startSpeaking(btnListen, lastTranslatedText);
                        return;
                    }

                    // First time - need to translate English text
                    btnListen.setText("Translating...");
                    btnListen.setEnabled(false);

                    translationManager.translate(answer.getAnswerText(),
                            new com.agriminds.utils.TranslationManager.OnTranslationListener() {
                                @Override
                                public void onTranslationSuccess(String translatedText) {
                                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                                        // Extra safety: if translation returned Bengali for Bengali input, don't
                                        // proceed
                                        if (isBengaliText(answer.getAnswerText())) {
                                            btnListen.setText("🔊 Listen in Bangla");
                                            btnListen.setEnabled(true);
                                            return;
                                        }
                                        lastTranslatedText = translatedText;
                                        startSpeaking(btnListen, translatedText);
                                    });
                                }

                                @Override
                                public void onTranslationError(String error) {
                                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                                        android.widget.Toast.makeText(itemView.getContext(),
                                                error, android.widget.Toast.LENGTH_LONG).show();
                                        btnListen.setText("🔊 Listen in Bangla");
                                        btnListen.setEnabled(true);
                                        isSpeaking = false;
                                    });
                                }
                            });
                } else {
                    android.widget.Toast.makeText(itemView.getContext(),
                            "Translation service not available", android.widget.Toast.LENGTH_SHORT).show();
                }
            });

            // Load rating for this answer by current user
            AppDatabase.databaseWriteExecutor.execute(() -> {
                ExpertRating userRating = database.expertRatingDao()
                        .getRatingByFarmerAndAnswer(currentUserId, answer.getId());

                // Get expert's overall average rating
                Float avgRating = database.expertRatingDao()
                        .getAverageRating(answer.getExpertId());

                ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                    // Check if this is an AI answer
                    boolean isAIAnswer = "AI".equals(answer.getAnswerType());

                    if (isAIAnswer) {
                        // AI answers cannot be rated - hide rating section
                        layoutRatingSection.setVisibility(View.GONE);
                    } else if (isFarmer) {
                        // Farmers can rate all expert answers
                        layoutRatingSection.setVisibility(View.VISIBLE);

                        if (userRating != null) {
                            // Already rated - show yellow button with current rating
                            btnGiveRating.setVisibility(View.VISIBLE);
                            btnGiveRating.setText("Rating: " + (int) userRating.getRating() + "★");
                            tvTapToChange.setVisibility(View.VISIBLE);
                            layoutStarsContainer.setVisibility(View.GONE);
                            ratingBar.setRating(userRating.getRating());
                        } else {
                            // Not rated yet - show yellow button
                            btnGiveRating.setVisibility(View.VISIBLE);
                            btnGiveRating.setText("Give Rating");
                            tvTapToChange.setVisibility(View.GONE);
                            layoutStarsContainer.setVisibility(View.GONE);
                            ratingBar.setRating(0);
                        }

                        // Yellow button click - expand stars
                        btnGiveRating.setOnClickListener(v -> {
                            btnGiveRating.setVisibility(View.GONE);
                            tvTapToChange.setVisibility(View.GONE);
                            layoutStarsContainer.setVisibility(View.VISIBLE);
                            float currentRating = userRating != null ? userRating.getRating() : 0;
                            ratingBar.setRating(currentRating);

                            // Always start with white when opening rating, user will see green as they
                            // click
                            setRatingBarColor(ratingBar, android.graphics.Color.WHITE);
                        });

                        // Rating bar interaction
                        ratingBar.setIsIndicator(false);
                        ratingBar.setOnRatingBarChangeListener((rb, rating, fromUser) -> {
                            if (fromUser) {
                                if (rating > 0) {
                                    pendingRating = rating;
                                    // Turn stars green when clicked
                                    setRatingBarColor(ratingBar, android.graphics.Color.GREEN);
                                } else {
                                    // Turn white when cleared
                                    pendingRating = 0;
                                    setRatingBarColor(ratingBar, android.graphics.Color.WHITE);
                                }
                            }
                        });

                        // Done button click - save and collapse
                        btnDoneRating.setOnClickListener(v -> {
                            if (pendingRating > 0 || ratingBar.getRating() > 0) {
                                float finalRating = pendingRating > 0 ? pendingRating : ratingBar.getRating();
                                saveRating(answer.getId(), answer.getExpertId(), finalRating);

                                // Collapse back to yellow button
                                layoutStarsContainer.setVisibility(View.GONE);
                                btnGiveRating.setVisibility(View.VISIBLE);
                                btnGiveRating.setText("Rating: " + (int) finalRating + "★");
                                tvTapToChange.setVisibility(View.VISIBLE);
                                pendingRating = 0;
                            }
                        });

                        // Undo button click - clear rating and go back
                        btnUndoRating.setOnClickListener(v -> {
                            // Clear rating from database
                            deleteRating(answer.getId());

                            // Reset UI
                            ratingBar.setRating(0);
                            pendingRating = 0;

                            // Collapse back to yellow button
                            layoutStarsContainer.setVisibility(View.GONE);
                            btnGiveRating.setVisibility(View.VISIBLE);
                            btnGiveRating.setText("Give Rating");
                            tvTapToChange.setVisibility(View.GONE);
                        });
                    } else {
                        // Experts cannot rate any answers - hide rating section
                        layoutRatingSection.setVisibility(View.GONE);
                    }

                    // Show average rating beside expert name
                    if (avgRating != null && avgRating > 0) {
                        tvAverageRating.setVisibility(View.VISIBLE);
                        tvAverageRating.setText(String.format(Locale.getDefault(),
                                "(%.1f★)", avgRating));
                    } else {
                        tvAverageRating.setVisibility(View.GONE);
                    }
                });
            });

            // Load and show replies for this answer
            loadReplies(answer.getId());

            // Show reply input only for:
            // 1. Farmers (can reply to any expert answer)
            // 2. Experts replying to their own answer only
            if (isFarmer) {
                // Farmers can reply to all expert answers
                etReply.setVisibility(View.VISIBLE);
                btnSendReply.setVisibility(View.VISIBLE);
                btnSendReply.setOnClickListener(v -> sendReply(answer));
            } else {
                // Experts can only reply to their own answers
                if (answer.getExpertId() == currentUserId) {
                    etReply.setVisibility(View.VISIBLE);
                    btnSendReply.setVisibility(View.VISIBLE);
                    btnSendReply.setOnClickListener(v -> sendReply(answer));
                } else {
                    // Hide reply section for other experts' answers
                    etReply.setVisibility(View.GONE);
                    btnSendReply.setVisibility(View.GONE);
                }
            }
        }

        private void loadReplies(int answerId) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                List<Reply> replies = database.replyDao().getRepliesByAnswer(answerId);

                ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                    if (!replies.isEmpty()) {
                        layoutReplies.setVisibility(View.VISIBLE);
                        tvRepliesCount.setText(replies.size() + " " + (replies.size() == 1 ? "reply" : "replies"));
                        repliesAdapter.setReplies(replies);
                    } else {
                        layoutReplies.setVisibility(View.VISIBLE); // Show to allow first reply
                        tvRepliesCount.setVisibility(View.GONE);
                    }
                });
            });
        }

        private void sendReply(ExpertAnswer answer) {
            String replyText = etReply.getText().toString().trim();

            if (replyText.isEmpty()) {
                android.widget.Toast.makeText(itemView.getContext(),
                        "Please enter your reply", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            Reply reply = new Reply();
            reply.setQuestionId(answer.getQuestionId());
            reply.setAnswerId(answer.getId());
            reply.setUserId(currentUserId);
            reply.setUserName(currentUserName);
            reply.setUserType(isFarmer ? "FARMER" : "EXPERT");
            reply.setReplyText(replyText);

            AppDatabase.databaseWriteExecutor.execute(() -> {
                database.replyDao().insertReply(reply);

                // Check if this is a reply to AI answer - if so, trigger AI auto-response
                if ("AI".equals(answer.getAnswerType())) {
                    generateAIReply(answer, replyText);
                }

                ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                    etReply.setText("");
                    android.widget.Toast.makeText(itemView.getContext(),
                            "Reply sent", android.widget.Toast.LENGTH_SHORT).show();
                    loadReplies(answer.getId());
                });
            });
        }

        private void saveRating(int answerId, int expertId, float rating) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                // Check if rating already exists
                ExpertRating existingRating = database.expertRatingDao()
                        .getRatingByFarmerAndAnswer(currentUserId, answerId);

                if (existingRating != null) {
                    // Update existing rating
                    existingRating.setRating((int) rating);
                    database.expertRatingDao().updateRating(existingRating);
                } else {
                    // Create new rating
                    ExpertRating newRating = new ExpertRating();
                    newRating.setAnswerId(answerId);
                    newRating.setExpertId(expertId);
                    newRating.setFarmerId(currentUserId);
                    newRating.setRating((int) rating);
                    database.expertRatingDao().insertRating(newRating);
                }

                ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                    android.widget.Toast.makeText(itemView.getContext(),
                            "Rating saved: " + (int) rating + " stars",
                            android.widget.Toast.LENGTH_SHORT).show();
                    notifyItemChanged(getAdapterPosition());
                });
            });
        }

        private void deleteRating(int answerId) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                ExpertRating existingRating = database.expertRatingDao()
                        .getRatingByFarmerAndAnswer(currentUserId, answerId);

                if (existingRating != null) {
                    database.expertRatingDao().deleteRating(existingRating);

                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                        android.widget.Toast.makeText(itemView.getContext(),
                                "Rating removed",
                                android.widget.Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }

        private void setRatingBarColor(RatingBar ratingBar, int color) {
            android.graphics.drawable.LayerDrawable stars = (android.graphics.drawable.LayerDrawable) ratingBar
                    .getProgressDrawable();
            if (stars != null) {
                // Set filled stars color (the stars that are selected)
                stars.getDrawable(2).setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_ATOP);
                // Set empty stars to white (the stars that are not selected)
                stars.getDrawable(0).setColorFilter(android.graphics.Color.WHITE,
                        android.graphics.PorterDuff.Mode.SRC_ATOP);
            }
        }

        private String getTimeAgo(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (seconds < 60) {
                return "Just now";
            } else if (minutes < 60) {
                return minutes + " min ago";
            } else if (hours < 24) {
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else if (days < 7) {
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            } else {
                return dateFormat.format(new Date(timestamp));
            }
        }

        private void generateAIReply(ExpertAnswer answer, String userReplyText) {
            // Generate AI response to user's reply
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    android.util.Log.d("GeminiAI", "Generating AI reply for: " + userReplyText);

                    // Get original question context
                    com.agriminds.data.entity.Question question = database.questionDao()
                            .getQuestionById(answer.getQuestionId());
                    String questionText = question != null ? question.getQuestionText() : "your farming question";

                    // Create JSON request
                    JSONObject requestJson = new JSONObject();
                    JSONArray contents = new JSONArray();
                    JSONObject content = new JSONObject();
                    JSONArray parts = new JSONArray();

                    JSONObject textPart = new JSONObject();
                    textPart.put("text",
                            "You are an agricultural expert AI assistant. " +
                                    "Original question: \"" + questionText + "\"\n" +
                                    "Your previous answer: \"" + answer.getAnswerText() + "\"\n\n" +
                                    "User's follow-up question/reply: \"" + userReplyText + "\"\n\n" +
                                    "Provide a helpful, concise response (2-4 sentences) addressing their follow-up question. "
                                    +
                                    "Be conversational and helpful.");

                    parts.put(textPart);
                    content.put("parts", parts);
                    contents.put(content);
                    requestJson.put("contents", contents);

                    // Make API call with key rotation
                    String aiReplyText = null;
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

                            OutputStream os = conn.getOutputStream();
                            os.write(requestJson.toString().getBytes());
                            os.flush();
                            os.close();

                            int responseCode = conn.getResponseCode();
                            android.util.Log.d("GeminiAI", "AI reply response code: " + responseCode + " (Key #"
                                    + (currentKeyIndex + 1) + ")");

                            if (responseCode == 200) {
                                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                                StringBuilder response = new StringBuilder();
                                String line;
                                while ((line = br.readLine()) != null) {
                                    response.append(line);
                                }
                                br.close();

                                JSONObject responseJson = new JSONObject(response.toString());
                                aiReplyText = responseJson.getJSONArray("candidates")
                                        .getJSONObject(0)
                                        .getJSONObject("content")
                                        .getJSONArray("parts")
                                        .getJSONObject(0)
                                        .getString("text");

                                android.util.Log.d("GeminiAI", "AI reply generated successfully");
                                success = true;
                            } else if (responseCode == 429) {
                                // Quota exceeded, try next key
                                android.util.Log.d("GeminiAI",
                                        "Quota exceeded for key #" + (currentKeyIndex + 1) + ", switching to next key");
                                currentKeyIndex = (currentKeyIndex + 1) % GEMINI_API_KEYS.length;
                            } else {
                                // Other error, don't retry
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
                    if (aiReplyText == null) {
                        aiReplyText = "Thank you for your follow-up question. Let me provide more details: " +
                                "Please ensure you follow the recommendations from my previous answer. " +
                                "If you need specific guidance, our expert team can help further.";
                    }

                    // Save new AI reply to database
                    Reply aiReply = new Reply();
                    aiReply.setQuestionId(answer.getQuestionId());
                    aiReply.setAnswerId(answer.getId());
                    aiReply.setUserId(0); // AI user ID
                    aiReply.setUserName("AI Assistant");
                    aiReply.setUserType("AI");
                    aiReply.setReplyText(aiReplyText);

                    database.replyDao().insertReply(aiReply);

                    android.util.Log.d("GeminiAI", "AI reply saved to database");

                    // Refresh replies on UI thread
                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                        loadReplies(answer.getId());
                    });

                } catch (Exception e) {
                    android.util.Log.e("GeminiAI", "Error generating AI reply: " + e.getMessage(), e);

                    // Save fallback reply on error
                    try {
                        Reply aiReply = new Reply();
                        aiReply.setQuestionId(answer.getQuestionId());
                        aiReply.setAnswerId(answer.getId());
                        aiReply.setUserId(0);
                        aiReply.setUserName("AI Assistant");
                        aiReply.setUserType("AI");
                        aiReply.setReplyText(
                                "I understand your question. Our expert team will review this and provide detailed guidance.");

                        database.replyDao().insertReply(aiReply);

                        ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                            loadReplies(answer.getId());
                        });
                    } catch (Exception ex) {
                        android.util.Log.e("GeminiAI", "Failed to save fallback AI reply");
                    }
                }
            });
        }

        private void startSpeaking(MaterialButton btnListen, String translatedText) {
            btnListen.setText("⏹️ Stop Speaking");
            btnListen.setEnabled(true);
            isSpeaking = true;

            // Debug logging
            android.util.Log.d("TranslationDebug", "startSpeaking called");
            android.util.Log.d("TranslationDebug", "originalAnswerText: " + originalAnswerText);
            android.util.Log.d("TranslationDebug", "translatedText: " + translatedText);
            android.util.Log.d("TranslationDebug", "isBengali(original): "
                    + (originalAnswerText != null ? isBengaliText(originalAnswerText) : "null"));

            // Show Bengali translation below the answer and show hide button
            // Only show if original text is NOT already in Bengali (avoid duplicate Bengali
            // text)
            if (tvBengaliTranslation != null && translatedText != null
                    && originalAnswerText != null && !isBengaliText(originalAnswerText)) {
                android.util.Log.d("TranslationDebug", "Showing translation TextView");
                tvBengaliTranslation.setText(translatedText);
                tvBengaliTranslation.setVisibility(View.VISIBLE);
                btnHideTranslation.setVisibility(View.VISIBLE);
            } else {
                android.util.Log.d("TranslationDebug", "NOT showing translation TextView - check failed");
            }

            // Use new speak method with listener
            ttsManager.speak(translatedText, new com.agriminds.utils.TTSManager.OnSpeakListener() {
                @Override
                public void onStart() {
                    // Speech started successfully
                }

                @Override
                public void onError(String error) {
                    android.widget.Toast.makeText(itemView.getContext(),
                            error, android.widget.Toast.LENGTH_LONG).show();
                    btnListen.setText("🔊 Listen in Bangla");
                    btnListen.setEnabled(true);
                    isSpeaking = false;
                }
            });

            // Reset button after speech completes (approximate duration)
            int duration = Math.max(3000, translatedText.length() * 50);
            resetButtonRunnable = () -> {
                btnListen.setText("🔊 Listen in Bangla");
                btnListen.setEnabled(true);
                isSpeaking = false;
            };
            speechHandler.postDelayed(resetButtonRunnable, duration);
        }

        private boolean isBengaliText(String text) {
            if (text == null || text.trim().isEmpty()) {
                return false;
            }
            // Check if text contains Bengali Unicode characters (ঀ-৿)
            return text.matches(".*[\\u0980-\\u09FF]+.*");
        }
    }
}
