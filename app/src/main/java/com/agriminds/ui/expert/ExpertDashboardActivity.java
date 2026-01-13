package com.agriminds.ui.expert;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.R;
import com.agriminds.adapter.ExpertQuestionsAdapter;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.ExpertAnswer;
import com.agriminds.data.entity.ExpertRating;
import com.agriminds.data.entity.HiddenQuestion;
import com.agriminds.data.entity.Question;
import com.agriminds.data.entity.User;
import com.agriminds.ui.adapters.ExpertMyAnswersAdapter;
import com.agriminds.ui.adapters.ExpertCommunityAdapter;
import com.agriminds.ui.adapters.LatestRatingsAdapter;
import com.agriminds.ui.cropchart.CropChartAdapter;
import com.agriminds.data.entity.CropChart;
import com.agriminds.ui.login.LoginActivity;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExpertDashboardActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private AppDatabase database;
    private int expertId;
    private String expertName;

    // Views
    private TextView tvExpertName, tvPendingCount;
    private LinearLayout btnTabQuestions, btnTabMyAnswers, btnTabCommunity, btnTabRating;
    private View viewQuestions, viewMyAnswers, viewCommunity, viewRating;

    // Questions Tab
    private RecyclerView recyclerViewQuestions;
    private TextView emptyQuestions;
    private ExpertQuestionsAdapter questionsAdapter;

    // My Answers Tab
    private RecyclerView recyclerViewMyAnswers;
    private TextView emptyMyAnswers;
    private ExpertMyAnswersAdapter myAnswersAdapter;
    private MaterialButton btnAllAnswers, btnLatest5;
    private boolean showAllAnswers = true;

    // Community Tab
    private RecyclerView recyclerViewCommunity;
    private TextView emptyCommunity;
    private CropChartAdapter cropChartAdapter;

    // Rating Tab
    private TextView tvAverageRatingValue, tvTotalAnswers, tvRatedAnswers, emptyRatings;
    private RatingBar ratingBarAverage;
    private RecyclerView recyclerViewLatestRatings;
    private LatestRatingsAdapter latestRatingsAdapter;
    private MaterialButton btnGetCertificate, btnToggleLatestRatings;
    private LinearLayout containerLatestRatings;
    private boolean latestRatingsVisible = false;
    private float currentAverageRating = 0f;

    private int currentTab = 0; // 0=Questions, 1=MyAnswers, 2=Community, 3=Rating

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expert_dashboard);

        sharedPreferences = getSharedPreferences("AgriMindsPrefs", MODE_PRIVATE);
        database = AppDatabase.getInstance(this);

        expertId = sharedPreferences.getInt("userId", -1);
        expertName = sharedPreferences.getString("userName", "Expert");

        initViews();
        setupTabs();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Expert Panel");
        }

        loadData();
    }

    private void initViews() {
        tvExpertName = findViewById(R.id.tv_expert_name);
        tvPendingCount = findViewById(R.id.tvPendingCount);

        tvExpertName.setText("Welcome, " + expertName);

        // Tab buttons
        btnTabQuestions = findViewById(R.id.btnTabQuestions);
        btnTabMyAnswers = findViewById(R.id.btnTabMyAnswers);
        btnTabCommunity = findViewById(R.id.btnTabCommunity);
        btnTabRating = findViewById(R.id.btnTabRating);

        // Views
        viewQuestions = findViewById(R.id.viewQuestions);
        viewMyAnswers = findViewById(R.id.viewMyAnswers);
        viewCommunity = findViewById(R.id.viewCommunity);
        viewRating = findViewById(R.id.viewRating);

        // Questions Tab
        recyclerViewQuestions = findViewById(R.id.recyclerViewQuestions);
        emptyQuestions = findViewById(R.id.emptyQuestions);
        recyclerViewQuestions.setLayoutManager(new LinearLayoutManager(this));
        questionsAdapter = new ExpertQuestionsAdapter(new ArrayList<>(), this, expertId, expertName);
        questionsAdapter.setOnAnswerSubmittedListener(() -> {
            // Refresh all tabs when an answer is submitted
            loadPendingQuestions();
            loadMyAnswers(showAllAnswers);
            loadCommunity();
        });
        recyclerViewQuestions.setAdapter(questionsAdapter);

        // Delete All Questions Button
        MaterialButton btnDeleteAllQuestions = findViewById(R.id.btnDeleteAllQuestions);
        btnDeleteAllQuestions.setOnClickListener(v -> deleteAllQuestions());

        // My Answers Tab
        recyclerViewMyAnswers = findViewById(R.id.recyclerViewMyAnswers);
        emptyMyAnswers = findViewById(R.id.emptyMyAnswers);
        btnAllAnswers = findViewById(R.id.btnAllAnswers);
        btnLatest5 = findViewById(R.id.btnLatest5);
        recyclerViewMyAnswers.setLayoutManager(new LinearLayoutManager(this));
        myAnswersAdapter = new ExpertMyAnswersAdapter(new ArrayList<>(), this, expertId, expertName);
        recyclerViewMyAnswers.setAdapter(myAnswersAdapter);

        // Setup filter buttons
        btnAllAnswers.setOnClickListener(v -> {
            showAllAnswers = true;
            btnAllAnswers.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_green_dark));
            btnLatest5.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
            loadMyAnswers(true);
        });

        btnLatest5.setOnClickListener(v -> {
            showAllAnswers = false;
            btnLatest5.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_green_dark));
            btnAllAnswers.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
            loadMyAnswers(false);
        });

        // Community Tab
        recyclerViewCommunity = findViewById(R.id.recyclerViewCommunity);
        emptyCommunity = findViewById(R.id.emptyCommunity);
        recyclerViewCommunity.setLayoutManager(new LinearLayoutManager(this));
        cropChartAdapter = new CropChartAdapter(new ArrayList<>(), this, true);
        recyclerViewCommunity.setAdapter(cropChartAdapter);

        // Rating Tab
        tvAverageRatingValue = findViewById(R.id.tvAverageRatingValue);
        tvTotalAnswers = findViewById(R.id.tvTotalAnswers);
        tvRatedAnswers = findViewById(R.id.tvRatedAnswers);
        ratingBarAverage = findViewById(R.id.ratingBarAverage);
        btnGetCertificate = findViewById(R.id.btnGetCertificate);
        btnToggleLatestRatings = findViewById(R.id.btnToggleLatestRatings);
        containerLatestRatings = findViewById(R.id.containerLatestRatings);
        recyclerViewLatestRatings = findViewById(R.id.recyclerViewLatestRatings);
        emptyRatings = findViewById(R.id.emptyRatings);

        recyclerViewLatestRatings.setLayoutManager(new LinearLayoutManager(this));
        latestRatingsAdapter = new LatestRatingsAdapter(new ArrayList<>(), this);
        recyclerViewLatestRatings.setAdapter(latestRatingsAdapter);

        btnGetCertificate.setOnClickListener(v -> {
            if (currentAverageRating >= 4.5f) {
                generateCertificate();
            }
        });

        btnToggleLatestRatings.setOnClickListener(v -> toggleLatestRatings());
        ratingBarAverage = findViewById(R.id.ratingBarAverage);

        findViewById(R.id.btn_logout).setOnClickListener(v -> logout());
    }

    private void setupTabs() {
        btnTabQuestions.setOnClickListener(v -> switchTab(0));
        btnTabMyAnswers.setOnClickListener(v -> switchTab(1));
        btnTabCommunity.setOnClickListener(v -> switchTab(2));
        btnTabRating.setOnClickListener(v -> switchTab(3));
    }

    private void switchTab(int tab) {
        currentTab = tab;

        // Reset all tabs
        btnTabQuestions.setBackgroundResource(R.drawable.bg_tab_unselected);
        btnTabMyAnswers.setBackgroundResource(R.drawable.bg_tab_unselected);
        btnTabCommunity.setBackgroundResource(R.drawable.bg_tab_unselected);
        btnTabRating.setBackgroundResource(R.drawable.bg_tab_unselected);

        viewQuestions.setVisibility(View.GONE);
        viewMyAnswers.setVisibility(View.GONE);
        viewCommunity.setVisibility(View.GONE);
        viewRating.setVisibility(View.GONE);

        // Show selected tab
        switch (tab) {
            case 0:
                btnTabQuestions.setBackgroundResource(R.drawable.bg_tab_selected);
                viewQuestions.setVisibility(View.VISIBLE);
                loadPendingQuestions();
                break;
            case 1:
                btnTabMyAnswers.setBackgroundResource(R.drawable.bg_tab_selected);
                viewMyAnswers.setVisibility(View.VISIBLE);
                loadMyAnswers(showAllAnswers);
                break;
            case 2:
                btnTabCommunity.setBackgroundResource(R.drawable.bg_tab_selected);
                viewCommunity.setVisibility(View.VISIBLE);
                loadCommunity();
                break;
            case 3:
                btnTabRating.setBackgroundResource(R.drawable.bg_tab_selected);
                viewRating.setVisibility(View.VISIBLE);
                loadRatings();
                break;
        }
    }

    private void loadData() {
        switchTab(0); // Start with Questions tab
    }

    private void deleteAllQuestions() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Hide All Questions")
                .setMessage("Are you sure you want to hide ALL pending questions from your view?")
                .setPositiveButton("Hide All", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        // Get all questions
                        List<Question> allQuestions = database.questionDao().getAllQuestions();
                        List<Integer> hiddenQuestionIds = database.hiddenQuestionDao()
                                .getHiddenQuestionIdsByExpert(expertId);

                        int hiddenCount = 0;
                        for (Question q : allQuestions) {
                            // Skip if already hidden
                            if (hiddenQuestionIds.contains(q.getId())) {
                                continue;
                            }

                            // Check if this expert has answered
                            List<ExpertAnswer> answers = database.expertAnswerDao()
                                    .getAnswersByQuestion(q.getId());
                            boolean thisExpertAnswered = false;
                            for (ExpertAnswer answer : answers) {
                                if (answer.getExpertId() == expertId) {
                                    thisExpertAnswered = true;
                                    break;
                                }
                            }

                            // Hide if not answered
                            if (!thisExpertAnswered) {
                                HiddenQuestion hiddenQuestion = new HiddenQuestion();
                                hiddenQuestion.setExpertId(expertId);
                                hiddenQuestion.setQuestionId(q.getId());
                                hiddenQuestion.setHiddenAt(System.currentTimeMillis());
                                database.hiddenQuestionDao().insertHiddenQuestion(hiddenQuestion);
                                hiddenCount++;
                            }
                        }

                        final int finalCount = hiddenCount;
                        runOnUiThread(() -> {
                            Toast.makeText(this, finalCount + " question(s) hidden successfully",
                                    Toast.LENGTH_SHORT).show();
                            loadPendingQuestions();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadPendingQuestions() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Get all questions
            List<Question> allQuestions = database.questionDao().getAllQuestions();
            // Get hidden question IDs for this expert
            List<Integer> hiddenQuestionIds = database.hiddenQuestionDao().getHiddenQuestionIdsByExpert(expertId);
            // Get questions that THIS expert hasn't answered yet (pending)
            List<Question> pendingQuestions = new ArrayList<>();

            for (Question q : allQuestions) {
                // Skip hidden questions
                if (hiddenQuestionIds.contains(q.getId())) {
                    continue;
                }

                List<ExpertAnswer> answers = database.expertAnswerDao().getAnswersByQuestion(q.getId());

                // Check if THIS expert has already answered this question
                boolean thisExpertAnswered = false;
                for (ExpertAnswer answer : answers) {
                    if (answer.getExpertId() == expertId) {
                        thisExpertAnswered = true;
                        break;
                    }
                }

                // If this expert hasn't answered yet, it's still pending for them
                if (!thisExpertAnswered) {
                    pendingQuestions.add(q);
                }
            }

            runOnUiThread(() -> {
                tvPendingCount.setText("Pending: " + pendingQuestions.size());

                if (pendingQuestions.isEmpty()) {
                    recyclerViewQuestions.setVisibility(View.GONE);
                    emptyQuestions.setVisibility(View.VISIBLE);
                } else {
                    recyclerViewQuestions.setVisibility(View.VISIBLE);
                    emptyQuestions.setVisibility(View.GONE);
                    questionsAdapter.updateQuestions(pendingQuestions);
                }
            });
        });
    }

    private void loadMyAnswers() {
        loadMyAnswers(showAllAnswers);
    }

    private void loadMyAnswers(boolean showAll) {
        android.util.Log.d("ExpertDashboard", "loadMyAnswers called with showAll: " + showAll);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Get all answers by this expert
            List<ExpertAnswer> dbAnswers = database.expertAnswerDao().getAnswersByExpert(expertId);

            // Create a mutable copy to avoid UnsupportedOperationException if Room returns
            // immutable list
            List<ExpertAnswer> myAnswers = new ArrayList<>(dbAnswers != null ? dbAnswers : new ArrayList<>());

            // Sort by date (latest first) to ensure correct order
            try {
                myAnswers.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
            } catch (Exception e) {
                e.printStackTrace();
                // Fallback: rely on DB order if sort fails
            }

            // For each answer, check if there are farmer replies
            List<QuestionWithReplies> answersWithReplies = new ArrayList<>();
            int count = 0;
            int targetCount = showAll ? myAnswers.size() : 5;

            android.util.Log.d("ExpertDashboard",
                    "Processing answers. Total: " + myAnswers.size() + ", Target: " + targetCount);

            for (ExpertAnswer answer : myAnswers) {
                // Stop if we found enough answers for the "Latest 5" view
                if (!showAll && count >= targetCount)
                    break;

                Question question = database.questionDao().getQuestionById(answer.getQuestionId());

                // CRITICAL FIX: Skip orphaned answers where question is null to prevent crash
                if (question == null) {
                    android.util.Log.w("ExpertDashboard", "Skipping orphan answer ID: " + answer.getId());
                    continue;
                }

                int replyCount = database.replyDao().getReplyCountByAnswer(answer.getId());
                answersWithReplies.add(new QuestionWithReplies(question, answer, replyCount));
                count++;
            }

            runOnUiThread(() -> {
                if (answersWithReplies.isEmpty()) {
                    recyclerViewMyAnswers.setVisibility(View.GONE);
                    emptyMyAnswers.setVisibility(View.VISIBLE);
                } else {
                    recyclerViewMyAnswers.setVisibility(View.VISIBLE);
                    emptyMyAnswers.setVisibility(View.GONE);
                    myAnswersAdapter.updateAnswers(answersWithReplies);
                }
            });
        });
    }

    private void loadCommunity() {
        database.cropChartDao().getAllSharedCropCharts().observe(this, cropCharts -> {
            if (cropCharts != null && !cropCharts.isEmpty()) {
                recyclerViewCommunity.setVisibility(View.VISIBLE);
                emptyCommunity.setVisibility(View.GONE);
                cropChartAdapter.updateData(cropCharts);
            } else {
                recyclerViewCommunity.setVisibility(View.GONE);
                emptyCommunity.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadRatings() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Get all answers by this expert
            List<ExpertAnswer> myAnswers = database.expertAnswerDao().getAnswersByExpert(expertId);
            int totalAnswers = myAnswers.size();

            // Get all ratings with farmer names
            List<LatestRatingsAdapter.RatingItem> ratingItems = new ArrayList<>();
            int ratedAnswers = 0;
            float totalRating = 0;
            int ratingCount = 0;

            for (ExpertAnswer answer : myAnswers) {
                List<ExpertRating> ratings = database.expertRatingDao().getRatingsByAnswer(answer.getId());

                for (ExpertRating rating : ratings) {
                    User farmer = database.userDao().getUserById(rating.getFarmerId());
                    Question question = database.questionDao().getQuestionById(answer.getQuestionId());

                    if (farmer != null && question != null) {
                        ratingItems.add(new LatestRatingsAdapter.RatingItem(
                                rating,
                                farmer.fullName,
                                question.getQuestionText()));
                    }

                    totalRating += rating.getRating();
                    ratingCount++;
                }

                if (!ratings.isEmpty()) {
                    ratedAnswers++;
                }
            }

            float averageRating = ratingCount > 0 ? totalRating / ratingCount : 0;

            // Sort by date (latest first) and limit to 10
            ratingItems.sort((a, b) -> Long.compare(b.rating.getCreatedAt(), a.rating.getCreatedAt()));
            if (ratingItems.size() > 10) {
                ratingItems = ratingItems.subList(0, 10);
            }

            int finalRatedAnswers = ratedAnswers;
            List<LatestRatingsAdapter.RatingItem> finalRatingItems = ratingItems;
            float finalAverageRating = averageRating;

            runOnUiThread(() -> {
                currentAverageRating = finalAverageRating;
                tvTotalAnswers.setText(String.valueOf(totalAnswers));
                tvRatedAnswers.setText(String.valueOf(finalRatedAnswers));
                tvAverageRatingValue.setText(String.format("%.1f", finalAverageRating));
                ratingBarAverage.setRating(finalAverageRating);

                // Update certificate button state - requires 10+ answers AND 4.5+ rating
                if (totalAnswers >= 10 && finalAverageRating >= 4.5f) {
                    btnGetCertificate.setText("🎓 Download Certificate");
                    btnGetCertificate
                            .setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_green_dark));
                    btnGetCertificate.setEnabled(true);
                } else {
                    int questionsNeeded = Math.max(0, 10 - totalAnswers);
                    btnGetCertificate
                            .setText("Answer " + questionsNeeded + " questions, reach 4.5 rating to get certificate");
                    btnGetCertificate
                            .setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
                    btnGetCertificate.setEnabled(false);
                }

                // Update latest ratings list
                if (finalRatingItems.isEmpty()) {
                    recyclerViewLatestRatings.setVisibility(View.GONE);
                    emptyRatings.setVisibility(View.VISIBLE);
                } else {
                    recyclerViewLatestRatings.setVisibility(View.VISIBLE);
                    emptyRatings.setVisibility(View.GONE);
                    latestRatingsAdapter.updateRatings(finalRatingItems);
                }
            });
        });
    }

    private void toggleLatestRatings() {
        latestRatingsVisible = !latestRatingsVisible;
        if (latestRatingsVisible) {
            containerLatestRatings.setVisibility(View.VISIBLE);
            btnToggleLatestRatings.setText("Hide Latest Ratings");
        } else {
            containerLatestRatings.setVisibility(View.GONE);
            btnToggleLatestRatings.setText("Show Latest Ratings");
        }
    }

    private void generateCertificate() {
        Toast.makeText(this, "Generating Certificate...", Toast.LENGTH_SHORT).show();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Get expert details
                List<ExpertAnswer> myAnswers = database.expertAnswerDao().getAnswersByExpert(expertId);
                int totalAnswers = myAnswers.size();

                // Calculate ratings
                int ratingCount = 0;
                float totalRating = 0;
                for (ExpertAnswer answer : myAnswers) {
                    List<ExpertRating> ratings = database.expertRatingDao().getRatingsByAnswer(answer.getId());
                    for (ExpertRating rating : ratings) {
                        totalRating += rating.getRating();
                        ratingCount++;
                    }
                }

                float avgRating = ratingCount > 0 ? totalRating / ratingCount : 0;

                // Create certificate bitmap
                Bitmap certificate = createCertificateBitmap(expertName, avgRating, totalAnswers, ratingCount);

                // Save to Downloads folder using MediaStore (works on all Android versions)
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String fileName = "AgriMinds_Certificate_" + expertName.replace(" ", "_") + "_" + timestamp + ".png";

                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS);

                android.net.Uri uri = getContentResolver()
                        .insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    java.io.OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    certificate.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.flush();
                    outputStream.close();

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Certificate saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();
                    });
                } else {
                    throw new Exception("Failed to create file in Downloads");
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error generating certificate: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private Bitmap createCertificateBitmap(String expertName, float rating, int totalAnswers, int ratingCount) {
        int width = 1200;
        int height = 850;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Background gradient (white to light blue)
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#F5F5F5"));
        canvas.drawRect(0, 0, width, height, bgPaint);

        // Border
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#FF9800"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(15);
        canvas.drawRect(30, 30, width - 30, height - 30, borderPaint);

        // Inner border
        borderPaint.setColor(Color.parseColor("#4CAF50"));
        borderPaint.setStrokeWidth(5);
        canvas.drawRect(50, 50, width - 50, height - 50, borderPaint);

        // Title
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#FF9800"));
        titlePaint.setTextSize(70);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("CERTIFICATE OF EXCELLENCE", width / 2, 150, titlePaint);

        // AgriMinds branding
        Paint brandPaint = new Paint();
        brandPaint.setColor(Color.parseColor("#4CAF50"));
        brandPaint.setTextSize(40);
        brandPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
        brandPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("AgriMinds Expert Platform", width / 2, 210, brandPaint);

        // Subtitle
        Paint subtitlePaint = new Paint();
        subtitlePaint.setColor(Color.parseColor("#333333"));
        subtitlePaint.setTextSize(35);
        subtitlePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("This certifies that", width / 2, 290, subtitlePaint);

        // Expert name
        Paint namePaint = new Paint();
        namePaint.setColor(Color.parseColor("#1976D2"));
        namePaint.setTextSize(60);
        namePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        namePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(expertName, width / 2, 370, namePaint);

        // Achievement text
        Paint achievementPaint = new Paint();
        achievementPaint.setColor(Color.parseColor("#333333"));
        achievementPaint.setTextSize(32);
        achievementPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("has achieved outstanding performance as an agricultural expert", width / 2, 440,
                achievementPaint);
        canvas.drawText("with a stellar rating of", width / 2, 480, achievementPaint);

        // Rating
        Paint ratingPaint = new Paint();
        ratingPaint.setColor(Color.parseColor("#FFD700"));
        ratingPaint.setTextSize(80);
        ratingPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        ratingPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.format("%.1f ★", rating), width / 2, 580, ratingPaint);

        // Statistics
        Paint statsPaint = new Paint();
        statsPaint.setColor(Color.parseColor("#555555"));
        statsPaint.setTextSize(28);
        statsPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Total Answers Provided: " + totalAnswers, width / 2, 650, statsPaint);
        canvas.drawText("Ratings Received: " + ratingCount, width / 2, 690, statsPaint);

        // Date
        String dateStr = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date());
        Paint datePaint = new Paint();
        datePaint.setColor(Color.parseColor("#777777"));
        datePaint.setTextSize(25);
        datePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Issued on " + dateStr, width / 2, 770, datePaint);

        return bitmap;
    }

    // Helper class for My Answers tab
    public static class QuestionWithReplies {
        public Question question;
        public ExpertAnswer answer;
        public int replyCount;

        public QuestionWithReplies(Question question, ExpertAnswer answer, int replyCount) {
            this.question = question;
            this.answer = answer;
            this.replyCount = replyCount;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload current tab data
        switchTab(currentTab);
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
