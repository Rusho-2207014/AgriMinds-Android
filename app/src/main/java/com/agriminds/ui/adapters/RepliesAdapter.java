package com.agriminds.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.R;
import com.agriminds.data.entity.Reply;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RepliesAdapter extends RecyclerView.Adapter<RepliesAdapter.ReplyViewHolder> {

    private List<Reply> replies;
    private SimpleDateFormat dateFormat;
    private com.agriminds.utils.TranslationManager translationManager;
    private com.agriminds.utils.TTSManager ttsManager;

    public RepliesAdapter() {
        this.replies = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
    }

    public void setManagers(com.agriminds.utils.TranslationManager translationManager,
            com.agriminds.utils.TTSManager ttsManager) {
        this.translationManager = translationManager;
        this.ttsManager = ttsManager;
    }

    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reply, parent, false);
        return new ReplyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        Reply reply = replies.get(position);
        holder.bind(reply);
    }

    @Override
    public int getItemCount() {
        return replies.size();
    }

    public void setReplies(List<Reply> replies) {
        this.replies = replies != null ? replies : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addReply(Reply reply) {
        this.replies.add(reply);
        notifyItemInserted(replies.size() - 1);
    }

    class ReplyViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivUserAvatar;
        private TextView tvUserName;
        private TextView tvUserTypeBadge;
        private TextView tvReplyDate;
        private TextView tvReplyText;
        private TextView tvBengaliTranslation;
        private MaterialButton btnListenReply;
        private MaterialButton btnHideTranslationReply;
        private boolean isSpeaking = false;
        private String lastTranslatedText = null;
        private String originalReplyText = null;
        private android.os.Handler speechHandler = new android.os.Handler();
        private Runnable resetButtonRunnable;

        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserTypeBadge = itemView.findViewById(R.id.tvUserTypeBadge);
            tvReplyDate = itemView.findViewById(R.id.tvReplyDate);
            tvReplyText = itemView.findViewById(R.id.tvReplyText);
            tvBengaliTranslation = itemView.findViewById(R.id.tvBengaliTranslation);
            btnListenReply = itemView.findViewById(R.id.btnListenReply);
            btnHideTranslationReply = itemView.findViewById(R.id.btnHideTranslationReply);

            // Hide/Show translation button toggle handler
            btnHideTranslationReply.setOnClickListener(v -> {
                if (tvBengaliTranslation.getVisibility() == View.VISIBLE) {
                    // Hide translation
                    tvBengaliTranslation.setVisibility(View.GONE);
                    btnHideTranslationReply.setText("👀 Show Translation");
                } else {
                    // Show translation
                    tvBengaliTranslation.setVisibility(View.VISIBLE);
                    btnHideTranslationReply.setText("🙈 Hide Translation");
                }
            });
        }

        public void bind(Reply reply) {
            tvUserName.setText(reply.getUserName());
            tvReplyText.setText(reply.getReplyText());
            originalReplyText = reply.getReplyText();
            tvReplyDate.setText(getTimeAgo(reply.getCreatedAt()));

            // Reset Bengali translation and hide button (only show when Listen button is
            // clicked to save API quota)
            tvBengaliTranslation.setVisibility(View.GONE);
            tvBengaliTranslation.setText("");
            btnHideTranslationReply.setVisibility(View.GONE);

            // Reset state for recycled views
            isSpeaking = false;
            lastTranslatedText = null;
            btnListenReply.setText("🔊 Listen in Bangla");
            btnListenReply.setEnabled(true);
            if (resetButtonRunnable != null) {
                speechHandler.removeCallbacks(resetButtonRunnable);
            }

            // Always show Listen button for both Bengali and English
            // Clear existing click listener to prevent duplicates on recycled views
            btnListenReply.setOnClickListener(null);
            btnListenReply.setVisibility(View.VISIBLE);

            // Set user type badge
            if ("EXPERT".equals(reply.getUserType())) {
                tvUserTypeBadge.setVisibility(View.VISIBLE);
                tvUserTypeBadge.setText("EXPERT");
                ivUserAvatar.setImageResource(R.drawable.ic_expert);
            } else if ("AI".equals(reply.getUserType())) {
                tvUserTypeBadge.setVisibility(View.VISIBLE);
                tvUserTypeBadge.setText("AI");
                ivUserAvatar.setImageResource(R.drawable.ic_expert);
            } else {
                tvUserTypeBadge.setVisibility(View.VISIBLE);
                tvUserTypeBadge.setText("FARMER");
                ivUserAvatar.setImageResource(R.drawable.ic_person);
            }

            // Listen Button Click with toggle functionality
            btnListenReply.setOnClickListener(v -> {
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
                        btnListenReply.setText("🔊 Listen in Bangla");
                        btnListenReply.setEnabled(true);

                        // Cancel pending reset
                        if (resetButtonRunnable != null) {
                            speechHandler.removeCallbacks(resetButtonRunnable);
                        }
                        return;
                    }

                    // If text is already in Bengali, speak it directly without translation
                    if (isBengaliText(reply.getReplyText())) {
                        btnListenReply.setText("⏹️ Stop Speaking");
                        btnListenReply.setEnabled(true);
                        isSpeaking = true;

                        // Speak Bengali text directly without showing translation
                        ttsManager.speak(reply.getReplyText(), new com.agriminds.utils.TTSManager.OnSpeakListener() {
                            @Override
                            public void onStart() {
                                // Speech started successfully
                            }

                            @Override
                            public void onError(String error) {
                                android.widget.Toast.makeText(itemView.getContext(),
                                        error, android.widget.Toast.LENGTH_LONG).show();
                                btnListenReply.setText("🔊 Listen in Bangla");
                                btnListenReply.setEnabled(true);
                                isSpeaking = false;
                            }
                        });
                        return;
                    }

                    // If we already have translation, just speak it again
                    if (lastTranslatedText != null && !lastTranslatedText.isEmpty()) {
                        startSpeaking(btnListenReply, lastTranslatedText);
                        return;
                    }

                    // First time - need to translate English text
                    btnListenReply.setText("Translating...");
                    btnListenReply.setEnabled(false);

                    translationManager.translate(reply.getReplyText(),
                            new com.agriminds.utils.TranslationManager.OnTranslationListener() {
                                @Override
                                public void onTranslationSuccess(String translatedText) {
                                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                                        // Extra safety: if translation returned Bengali for Bengali input, don't
                                        // proceed
                                        if (isBengaliText(reply.getReplyText())) {
                                            btnListenReply.setText("🔊 Listen in Bangla");
                                            btnListenReply.setEnabled(true);
                                            return;
                                        }
                                        lastTranslatedText = translatedText;
                                        startSpeaking(btnListenReply, translatedText);
                                    });
                                }

                                @Override
                                public void onTranslationError(String error) {
                                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                                        android.widget.Toast.makeText(itemView.getContext(),
                                                error, android.widget.Toast.LENGTH_LONG).show();
                                        btnListenReply.setText("🔊 Listen in Bangla");
                                        btnListenReply.setEnabled(true);
                                        isSpeaking = false;
                                    });
                                }
                            });
                } else {
                    android.widget.Toast.makeText(itemView.getContext(),
                            "Translation service not available", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void startSpeaking(MaterialButton btnListen, String translatedText) {
            btnListen.setText("⏹️ Stop Speaking");
            btnListen.setEnabled(true);
            isSpeaking = true;

            // Show Bengali translation below the reply and show hide button
            // Only show if original text is NOT already in Bengali (avoid duplicate Bengali
            // text)
            if (tvBengaliTranslation != null && translatedText != null
                    && originalReplyText != null && !isBengaliText(originalReplyText)) {
                tvBengaliTranslation.setText(translatedText);
                tvBengaliTranslation.setVisibility(View.VISIBLE);
                btnHideTranslationReply.setVisibility(View.VISIBLE);
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

        private boolean isBengaliText(String text) {
            if (text == null || text.trim().isEmpty()) {
                return false;
            }
            // Check if text contains Bengali Unicode characters (ঀ-৿)
            return text.matches(".*[\\u0980-\\u09FF]+.*");
        }
    }
}
