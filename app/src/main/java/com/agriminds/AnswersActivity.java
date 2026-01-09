package com.agriminds;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.AnswerView;
import com.agriminds.data.entity.ExpertAnswer;
import com.agriminds.ui.adapters.ExpertAnswersAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.List;

public class AnswersActivity extends AppCompatActivity {

    private RecyclerView recyclerViewExpertAnswers;
    private RecyclerView recyclerViewOthersAnswers;
    private ExpertAnswersAdapter expertAnswersAdapter;
    private ExpertAnswersAdapter othersAnswersAdapter;
    private View emptyState;
    private TextView tvQuestionText;
    private TextView tvFarmerName;
    private TextView tvQuestionLabel;
    private TextView tvAnswersLabel;
    private TextView tvOthersAnswersLabel;
    private MaterialButton btnPlayQuestionVoice;
    private AppDatabase database;
    private int questionId;
    private int currentUserId;
    private String currentUserName;
    private String currentUserType;
    private String questionAudioPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answers);

        database = AppDatabase.getInstance(this);

        SharedPreferences prefs = getSharedPreferences("AgriMindsPrefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("userId", -1);
        currentUserName = prefs.getString("userName", "User");
        currentUserType = prefs.getString("userType", "FARMER");

        questionId = getIntent().getIntExtra("questionId", -1);
        String questionText = getIntent().getStringExtra("questionText");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvQuestionText = findViewById(R.id.tvQuestionText);
        tvFarmerName = findViewById(R.id.tvFarmerName);
        tvQuestionLabel = findViewById(R.id.tvQuestionLabel);
        tvAnswersLabel = findViewById(R.id.tvAnswersLabel);
        tvOthersAnswersLabel = findViewById(R.id.tvOthersAnswersLabel);
        btnPlayQuestionVoice = findViewById(R.id.btnPlayQuestionVoice);
        tvQuestionText.setText(questionText);

        recyclerViewExpertAnswers = findViewById(R.id.recyclerViewExpertAnswers);
        recyclerViewOthersAnswers = findViewById(R.id.recyclerViewOthersAnswers);
        emptyState = findViewById(R.id.emptyState);

        // Load and display the question with farmer name
        loadQuestionInfo();

        // Setup Expert Answers RecyclerView
        recyclerViewExpertAnswers.setLayoutManager(new LinearLayoutManager(this));
        boolean isFarmer = "FARMER".equals(currentUserType);
        expertAnswersAdapter = new ExpertAnswersAdapter(database, currentUserId, currentUserName, isFarmer);
        recyclerViewExpertAnswers.setAdapter(expertAnswersAdapter);

        // Setup Others Answers RecyclerView
        recyclerViewOthersAnswers.setLayoutManager(new LinearLayoutManager(this));
        othersAnswersAdapter = new ExpertAnswersAdapter(database, currentUserId, currentUserName, isFarmer);
        recyclerViewOthersAnswers.setAdapter(othersAnswersAdapter);

        loadAnswers();
        markAnswersAsViewed();
    }

    private void loadQuestionInfo() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            com.agriminds.data.entity.Question question = database.questionDao().getQuestionById(questionId);
            List<ExpertAnswer> expertAnswers = database.expertAnswerDao().getAnswersByQuestion(questionId);

            // Store audio path
            questionAudioPath = question != null ? question.getAudioPath() : null;

            // Check if current expert has answered this question
            boolean hasOwnAnswer = false;
            if ("EXPERT".equals(currentUserType)) {
                for (ExpertAnswer answer : expertAnswers) {
                    if (answer.getExpertId() == currentUserId) {
                        hasOwnAnswer = true;
                        break;
                    }
                }
            }

            boolean finalHasOwnAnswer = hasOwnAnswer;
            runOnUiThread(() -> {
                // Show play button if question has audio
                if (questionAudioPath != null && !questionAudioPath.isEmpty()) {
                    btnPlayQuestionVoice.setVisibility(View.VISIBLE);
                    btnPlayQuestionVoice.setOnClickListener(v -> playQuestionAudio());
                } else {
                    btnPlayQuestionVoice.setVisibility(View.GONE);
                }

                // Update labels based on user type and whether expert has answered
                if ("EXPERT".equals(currentUserType) && finalHasOwnAnswer) {
                    tvQuestionLabel.setText("Question");
                    tvAnswersLabel.setText("Your Answer");
                } else {
                    tvQuestionLabel.setText("Your Question");
                    tvAnswersLabel.setText("Expert Answers");
                }

                if (question != null && question.getFarmerName() != null) {
                    tvFarmerName.setText("Asked by: " + question.getFarmerName());
                    tvFarmerName.setVisibility(View.VISIBLE);
                } else {
                    tvFarmerName.setVisibility(View.GONE);
                }
            });
        });
    }

    private void loadAnswers() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Get expert answers
            List<ExpertAnswer> expertAnswers = database.expertAnswerDao().getAnswersByQuestion(questionId);

            runOnUiThread(() -> {
                if (!expertAnswers.isEmpty()) {
                    if ("EXPERT".equals(currentUserType)) {
                        // Filter answers for experts: separate own answers from others
                        java.util.List<ExpertAnswer> ownAnswers = new java.util.ArrayList<>();
                        java.util.List<ExpertAnswer> othersAnswers = new java.util.ArrayList<>();

                        for (ExpertAnswer answer : expertAnswers) {
                            if (answer.getExpertId() == currentUserId) {
                                ownAnswers.add(answer);
                            } else {
                                othersAnswers.add(answer);
                            }
                        }

                        // Show own answers section
                        if (!ownAnswers.isEmpty()) {
                            expertAnswersAdapter.setAnswers(ownAnswers);
                            recyclerViewExpertAnswers.setVisibility(View.VISIBLE);
                            tvAnswersLabel.setVisibility(View.VISIBLE);
                        } else {
                            recyclerViewExpertAnswers.setVisibility(View.GONE);
                            tvAnswersLabel.setVisibility(View.GONE);
                        }

                        // Show others answers section
                        if (!othersAnswers.isEmpty()) {
                            othersAnswersAdapter.setAnswers(othersAnswers);
                            recyclerViewOthersAnswers.setVisibility(View.VISIBLE);
                            tvOthersAnswersLabel.setVisibility(View.VISIBLE);
                        } else {
                            recyclerViewOthersAnswers.setVisibility(View.GONE);
                            tvOthersAnswersLabel.setVisibility(View.GONE);
                        }

                        // Hide empty state if any answers exist
                        emptyState.setVisibility(View.GONE);
                    } else {
                        // For farmers, show all answers in one section
                        expertAnswersAdapter.setAnswers(expertAnswers);
                        recyclerViewExpertAnswers.setVisibility(View.VISIBLE);
                        tvAnswersLabel.setVisibility(View.VISIBLE);
                        recyclerViewOthersAnswers.setVisibility(View.GONE);
                        tvOthersAnswersLabel.setVisibility(View.GONE);
                        emptyState.setVisibility(View.GONE);
                    }
                } else {
                    recyclerViewExpertAnswers.setVisibility(View.GONE);
                    recyclerViewOthersAnswers.setVisibility(View.GONE);
                    tvAnswersLabel.setVisibility(View.GONE);
                    tvOthersAnswersLabel.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void markAnswersAsViewed() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ExpertAnswer> answers = database.expertAnswerDao()
                    .getAnswersByQuestion(questionId);

            for (ExpertAnswer answer : answers) {
                // Check if view record exists
                AnswerView existingView = database.answerViewDao().getAnswerView(answer.getId(), currentUserId);

                if (existingView != null) {
                    // Update existing record to mark as viewed
                    if (!existingView.isViewed()) {
                        database.answerViewDao().markAsViewed(answer.getId(), currentUserId,
                                System.currentTimeMillis());
                    }
                } else {
                    // Create new view record (already viewed)
                    AnswerView answerView = new AnswerView();
                    answerView.setAnswerId(answer.getId());
                    answerView.setFarmerId(currentUserId);
                    answerView.setViewed(true);
                    answerView.setViewedAt(System.currentTimeMillis());
                    database.answerViewDao().insertAnswerView(answerView);
                }
            }
        });
    }

    private void playQuestionAudio() {
        if (questionAudioPath == null || questionAudioPath.isEmpty()) {
            Toast.makeText(this, "No voice recording available", Toast.LENGTH_SHORT).show();
            return;
        }

        File audioFile = new File(questionAudioPath);
        if (!audioFile.exists()) {
            Toast.makeText(this, "Audio file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (audioPlayerManager != null) {
            audioPlayerManager.play(questionAudioPath,
                    () -> runOnUiThread(() -> btnPlayQuestionVoice.setText("▶️ Play Voice")));
            btnPlayQuestionVoice.setText("⏸️ Playing...");
        }
    }

    // Managers for Audio/Translation
    private com.agriminds.utils.TranslationManager translationManager;
    private com.agriminds.utils.TTSManager ttsManager;
    private com.agriminds.utils.AudioPlayerManager audioPlayerManager;

    @Override
    protected void onStart() {
        super.onStart();
        translationManager = new com.agriminds.utils.TranslationManager();
        ttsManager = new com.agriminds.utils.TTSManager(this);
        audioPlayerManager = new com.agriminds.utils.AudioPlayerManager();

        if (expertAnswersAdapter != null) {
            expertAnswersAdapter.setManagers(translationManager, ttsManager, audioPlayerManager);
        }
        if (othersAnswersAdapter != null) {
            othersAnswersAdapter.setManagers(translationManager, ttsManager, audioPlayerManager);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (translationManager != null)
            translationManager.close();
        if (ttsManager != null)
            ttsManager.shutdown();
        if (audioPlayerManager != null)
            audioPlayerManager.release();
    }
}