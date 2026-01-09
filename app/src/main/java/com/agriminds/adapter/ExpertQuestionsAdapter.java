package com.agriminds.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.AnswersActivity;
import com.agriminds.R;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.AnswerView;
import com.agriminds.data.entity.ExpertAnswer;
import com.agriminds.data.entity.HiddenQuestion;
import com.agriminds.data.entity.Question;
import com.bumptech.glide.Glide;

import java.util.List;

public class ExpertQuestionsAdapter extends RecyclerView.Adapter<ExpertQuestionsAdapter.ViewHolder> {

    public interface OnAnswerSubmittedListener {
        void onAnswerSubmitted();
    }

    private List<Question> questions;
    private Context context;
    private int expertId;
    private String expertName;
    private AppDatabase database;
    private OnAnswerSubmittedListener listener;
    private com.agriminds.utils.AudioPlayerManager audioPlayerManager;

    public ExpertQuestionsAdapter(List<Question> questions, Context context, int expertId, String expertName) {
        this.questions = questions;
        this.context = context;
        this.expertId = expertId;
        this.expertName = expertName;
        this.database = AppDatabase.getInstance(context);
        this.audioPlayerManager = new com.agriminds.utils.AudioPlayerManager();
    }

    public void setOnAnswerSubmittedListener(OnAnswerSubmittedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expert_question, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Question question = questions.get(position);

        holder.tvFarmerName.setText(question.getFarmerName());
        holder.tvCategory.setText(question.getCategory());
        holder.tvQuestionText.setText(question.getQuestionText());
        holder.tvDate.setText(getRelativeTime(question.getCreatedAt()));
        holder.tvAnswerCount.setText(question.getAnswerCount() + " answers");

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
            holder.btnPlayVoice.setVisibility(View.VISIBLE);
            holder.btnPlayVoice.setOnClickListener(v -> {
                holder.btnPlayVoice.setText("⏸️ Playing...");
                audioPlayerManager.play(question.getAudioPath(), () -> {
                    ((android.app.Activity) context)
                            .runOnUiThread(() -> holder.btnPlayVoice.setText("▶️ Play Farmer's Voice"));
                });
            });
        } else {
            holder.btnPlayVoice.setVisibility(View.GONE);
        }

        // Answer button click
        holder.btnAnswer.setOnClickListener(v -> {
            showAnswerDialog(question);
        });

        // View Conversation button click
        holder.btnViewConversation.setOnClickListener(v -> {
            Intent intent = new Intent(context, AnswersActivity.class);
            intent.putExtra("questionId", question.getId());
            intent.putExtra("questionText", question.getQuestionText());
            context.startActivity(intent);
        });

        // Delete button click
        holder.btnDelete.setOnClickListener(v -> {
            showDeleteConfirmDialog(question, position);
        });
    }

    private void showDeleteConfirmDialog(Question question, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Hide Question")
                .setMessage("Hide this question from your view? You can't see it again.")
                .setPositiveButton("Hide", (dialog, which) -> {
                    deleteQuestion(question, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteQuestion(Question question, int position) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Hide the question for this expert
            HiddenQuestion hiddenQuestion = new HiddenQuestion();
            hiddenQuestion.setExpertId(expertId);
            hiddenQuestion.setQuestionId(question.getId());
            hiddenQuestion.setHiddenAt(System.currentTimeMillis());
            database.hiddenQuestionDao().insertHiddenQuestion(hiddenQuestion);

            ((android.app.Activity) context).runOnUiThread(() -> {
                questions.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, questions.size());
                Toast.makeText(context, "Question hidden successfully", Toast.LENGTH_SHORT).show();

                if (listener != null) {
                    listener.onAnswerSubmitted();
                }
            });
        });
    }

    private void showAnswerDialog(Question question) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Answer Question");

        // Custom Layout for Dialog
        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final TextView tvQuestion = new TextView(context);
        tvQuestion.setText("Question: " + question.getQuestionText());
        tvQuestion.setPadding(0, 0, 0, 20);
        layout.addView(tvQuestion);

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setHint("Type your answer here...");
        input.setMinLines(4);
        layout.addView(input);

        // Voice Recording Section
        final Button btnRecord = new Button(context);
        btnRecord.setText("🎤 Record Voice Answer");
        layout.addView(btnRecord);

        final Button btnPlay = new Button(context);
        btnPlay.setText("▶️ Play Recording");
        btnPlay.setVisibility(View.GONE);
        layout.addView(btnPlay);

        final TextView tvStatus = new TextView(context);
        tvStatus.setText("No recording");
        tvStatus.setPadding(0, 10, 0, 0);
        layout.addView(tvStatus);

        final String[] audioPath = { null }; // Wrapper for checking in listener
        final com.agriminds.utils.VoiceRecorderManager voiceRecorderManager = new com.agriminds.utils.VoiceRecorderManager(
                context);
        final com.agriminds.utils.AudioPlayerManager audioPlayerManager = new com.agriminds.utils.AudioPlayerManager();
        final boolean[] isRecording = { false };

        btnRecord.setOnClickListener(v -> {
            if (!isRecording[0]) {
                // Start Recording
                String fileName = "expert_answer_" + System.currentTimeMillis();
                voiceRecorderManager.startRecording(fileName);
                isRecording[0] = true;
                btnRecord.setText("⏹️ Stop Recording");
                btnRecord.setBackgroundColor(android.graphics.Color.RED);
                tvStatus.setText("Recording...");
            } else {
                // Stop Recording
                audioPath[0] = voiceRecorderManager.stopRecording();
                isRecording[0] = false;
                btnRecord.setText("🎤 Re-record");
                btnRecord.setBackgroundColor(android.graphics.Color.LTGRAY); // Default-ish
                btnPlay.setVisibility(View.VISIBLE);
                tvStatus.setText("Audio recorded!");
            }
        });

        btnPlay.setOnClickListener(v -> {
            if (audioPath[0] != null) {
                btnPlay.setText("⏸️ Playing...");
                audioPlayerManager.play(audioPath[0], () -> {
                    ((android.app.Activity) context).runOnUiThread(() -> btnPlay.setText("▶️ Play Recording"));
                });
            }
        });

        builder.setView(layout);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String answerText = input.getText().toString().trim();

            if (isRecording[0]) {
                // Stop if still recording
                audioPath[0] = voiceRecorderManager.stopRecording();
            }

            if (!answerText.isEmpty() || audioPath[0] != null) {
                if (answerText.isEmpty())
                    answerText = "[Voice Answer]";
                submitAnswer(question, answerText, audioPath[0]);
            } else {
                Toast.makeText(context, "Please enter text or record audio", Toast.LENGTH_SHORT).show();
            }
            voiceRecorderManager.release();
            audioPlayerManager.release();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            if (isRecording[0])
                voiceRecorderManager.stopRecording();
            voiceRecorderManager.release();
            audioPlayerManager.release();
            dialog.cancel();
        });

        builder.setOnDismissListener(dialog -> {
            if (isRecording[0])
                voiceRecorderManager.stopRecording();
            voiceRecorderManager.release();
            audioPlayerManager.release();
        });

        builder.show();
    }

    private void submitAnswer(Question question, String answerText, String audioPath) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Create answer
            ExpertAnswer answer = new ExpertAnswer();
            answer.setQuestionId(question.getId());
            answer.setExpertId(expertId);
            answer.setExpertName(expertName);
            answer.setAnswerText(answerText);
            answer.setAudioPath(audioPath);
            answer.setAnswerType("EXPERT");
            answer.setCreatedAt(System.currentTimeMillis());

            long answerId = database.expertAnswerDao().insertAnswer(answer);

            // Increment answer count
            database.questionDao().incrementAnswerCount(question.getId());

            // Create unviewed answer record for farmer
            AnswerView answerView = new AnswerView();
            answerView.setAnswerId((int) answerId);
            answerView.setFarmerId(question.getFarmerId());
            answerView.setViewed(false);
            answerView.setViewedAt(0);
            database.answerViewDao().insertAnswerView(answerView);

            ((android.app.Activity) context).runOnUiThread(() -> {
                Toast.makeText(context, "Answer submitted successfully!", Toast.LENGTH_SHORT).show();

                // Remove the question from pending list
                int position = questions.indexOf(question);
                if (position >= 0) {
                    questions.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, questions.size());
                }

                // Notify parent activity to refresh all data
                if (listener != null) {
                    listener.onAnswerSubmitted();
                }
            });
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFarmerName, tvCategory, tvQuestionText, tvDate, tvAnswerCount;
        ImageView ivQuestionImage;
        Button btnAnswer, btnViewConversation, btnDelete;
        com.google.android.material.button.MaterialButton btnPlayVoice;

        ViewHolder(View itemView) {
            super(itemView);
            tvFarmerName = itemView.findViewById(R.id.tvFarmerName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvQuestionText = itemView.findViewById(R.id.tvQuestionText);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAnswerCount = itemView.findViewById(R.id.tvAnswerCount);
            ivQuestionImage = itemView.findViewById(R.id.ivQuestionImage);
            btnAnswer = itemView.findViewById(R.id.btnAnswer);
            btnViewConversation = itemView.findViewById(R.id.btnViewConversation);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnPlayVoice = itemView.findViewById(R.id.btnPlayVoice);
        }
    }
}
