package com.agriminds.ui.cropchart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.R;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.CropChart;
import com.agriminds.data.entity.CropChartComment;
import com.agriminds.data.entity.CropChartStar;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class CropChartDetailActivity extends AppCompatActivity {

    private TextView tvCropName, tvSeason, tvFarmerName, tvDate, tvFertilizers;
    private TextView tvSeedCost, tvFertilizerCost, tvLaborCost, tvOtherCosts;
    private TextView tvTotalCost, tvTotalYield, tvSellPrice, tvTotalRevenue, tvProfit;
    private TextView tvNoComments;
    private Button btnEdit, btnToggleShare, btnAddComment, btnSubmitStar;
    private LinearLayout layoutFarmerActions, layoutExpertCommentInput;
    private RecyclerView recyclerViewComments;
    private TextInputEditText etComment;
    private RatingBar ratingBarExpert;

    private AppDatabase database;
    private int chartId;
    private CropChart cropChart;
    private String userId, userName, userType;
    private CropChartCommentAdapter commentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_chart_detail);

        database = AppDatabase.getInstance(this);
        chartId = getIntent().getIntExtra("chartId", -1);

        if (chartId == -1) {
            Toast.makeText(this, "Invalid crop chart", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get user info
        SharedPreferences prefs = getSharedPreferences("AgriMindsPrefs", MODE_PRIVATE);
        userId = String.valueOf(prefs.getInt("userId", -1));
        userName = prefs.getString("userName", "User");
        userType = prefs.getString("userType", "FARMER");

        initializeViews();
        setupToolbar();
        loadCropChartDetails();
        loadComments();
    }

    private void initializeViews() {
        tvCropName = findViewById(R.id.tvCropName);
        tvSeason = findViewById(R.id.tvSeason);
        tvFarmerName = findViewById(R.id.tvFarmerName);
        tvDate = findViewById(R.id.tvDate);
        tvFertilizers = findViewById(R.id.tvFertilizers);

        tvSeedCost = findViewById(R.id.tvSeedCost);
        tvFertilizerCost = findViewById(R.id.tvFertilizerCost);
        tvLaborCost = findViewById(R.id.tvLaborCost);
        tvOtherCosts = findViewById(R.id.tvOtherCosts);
        tvTotalCost = findViewById(R.id.tvTotalCost);

        tvTotalYield = findViewById(R.id.tvTotalYield);
        tvSellPrice = findViewById(R.id.tvSellPrice);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvProfit = findViewById(R.id.tvProfit);

        btnEdit = findViewById(R.id.btnEdit);
        btnToggleShare = findViewById(R.id.btnToggleShare);
        layoutFarmerActions = findViewById(R.id.layoutFarmerActions);

        layoutExpertCommentInput = findViewById(R.id.layoutExpertCommentInput);
        etComment = findViewById(R.id.etComment);
        btnAddComment = findViewById(R.id.btnAddComment);
        ratingBarExpert = findViewById(R.id.ratingBarExpert);
        btnSubmitStar = findViewById(R.id.btnSubmitStar);

        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        tvNoComments = findViewById(R.id.tvNoComments);

        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CropChartCommentAdapter(new ArrayList<>(), this, userId, userName, userType);
        recyclerViewComments.setAdapter(commentAdapter);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadCropChartDetails() {
        database.cropChartDao().getCropChartById(chartId).observe(this, chart -> {
            if (chart != null) {
                cropChart = chart;
                displayCropChartDetails(chart);
                setupActions();
            }
        });
    }

    private void displayCropChartDetails(CropChart chart) {
        String taka = "\u09F3";

        tvCropName.setText(chart.getCropName());
        tvSeason.setText(chart.getSeason());
        tvFarmerName.setText("By: " + chart.getFarmerName());

        String dateRange = "📅 " + chart.getCultivationStartDate() + " - " + chart.getCultivationEndDate();
        tvDate.setText(dateRange);

        tvFertilizers.setText("🌱 Fertilizers: " + chart.getFertilizersUsed());

        tvSeedCost.setText(taka + String.format("%,.2f", chart.getSeedCost()));
        tvFertilizerCost.setText(taka + String.format("%,.2f", chart.getFertilizerCost()));
        tvLaborCost.setText(taka + String.format("%,.2f", chart.getLaborCost()));
        tvOtherCosts.setText(taka + String.format("%,.2f", chart.getOtherCosts()));
        tvTotalCost.setText(taka + String.format("%,.2f", chart.getTotalCost()));

        tvTotalYield.setText(chart.getTotalYield() + " quintals");
        tvSellPrice.setText(taka + String.format("%,.2f", chart.getSellPrice()) + " / quintal");
        tvTotalRevenue.setText(taka + String.format("%,.2f", chart.getTotalRevenue()));
        tvProfit.setText(taka + String.format("%,.2f", chart.getProfit()));

        if (chart.getProfit() >= 0) {
            tvProfit.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvProfit.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void setupActions() {
        // Check if current user is the owner
        boolean isOwner = cropChart.getFarmerId().equals(userId);

        if (userType.equals("FARMER") && isOwner) {
            // Show farmer actions
            layoutFarmerActions.setVisibility(View.VISIBLE);
            layoutExpertCommentInput.setVisibility(View.GONE);

            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddEditCropChartActivity.class);
                intent.putExtra("chartId", chartId);
                startActivity(intent);
            });

            updateShareButton();
            btnToggleShare.setOnClickListener(v -> toggleShareStatus());
        } else if (userType.equals("EXPERT") && cropChart.isShared()) {
            // Show expert comment input
            layoutFarmerActions.setVisibility(View.GONE);
            layoutExpertCommentInput.setVisibility(View.VISIBLE);

            btnAddComment.setOnClickListener(v -> addComment());

            // Load and show expert's previous star rating if exists
            new Thread(() -> {
                CropChartStar existingStar = database.cropChartStarDao().getStarByExpert(chartId, userId);
                runOnUiThread(() -> {
                    if (existingStar != null) {
                        ratingBarExpert.setRating(existingStar.getStars());
                    } else {
                        ratingBarExpert.setRating(0);
                    }
                });
            }).start();

            btnSubmitStar.setOnClickListener(v -> submitStarRating());
        } else {
            // Hide all action buttons
            layoutFarmerActions.setVisibility(View.GONE);
            layoutExpertCommentInput.setVisibility(View.GONE);
        }
    }

    private void updateShareButton() {
        if (cropChart.isShared()) {
            btnToggleShare.setText("Unshare");
        } else {
            btnToggleShare.setText("Share with Experts");
        }
    }

    private void toggleShareStatus() {
        new Thread(() -> {
            // 1. Fetch the latest state synchronously to avoid race conditions
            CropChart currentChart = database.cropChartDao().getCropChartByIdSync(chartId);
            if (currentChart == null)
                return;

            boolean isSharing = !currentChart.isShared(); // Toggle status

            // 2. Update share status in DB
            database.cropChartDao().updateShareStatus(chartId, isSharing);

            // 3. Update 'Persistent' Stats if sharing for the first time
            if (isSharing && !currentChart.isHasEverBeenShared()) {
                try {
                    // Re-enable persistent user stats as requested
                    database.userDao().incrementChartsShared(Integer.parseInt(userId));
                    database.cropChartDao().updateHasEverBeenShared(chartId, true);

                    // Update local object to reflect this immediately for this session
                    currentChart.setHasEverBeenShared(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // 4. Update UI
            runOnUiThread(() -> {
                // Update the memory object used by UI
                cropChart.setShared(isSharing);
                if (currentChart.isHasEverBeenShared()) {
                    cropChart.setHasEverBeenShared(true);
                }

                updateShareButton();
                String message = isSharing ? "Shared with experts successfully" : "Unshared from experts";
                // DEBUG TOAST REMOVED
                // android.widget.Toast.makeText(this, "Debug: Sharing? " + isSharing + "
                // FirstTime? " + (!currentChart.isHasEverBeenShared()),
                // android.widget.Toast.LENGTH_LONG).show();
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void loadComments() {
        database.cropChartCommentDao().getCommentsByCropChart(chartId).observe(this, comments -> {
            if (comments != null && !comments.isEmpty()) {
                commentAdapter.updateData(comments);
                recyclerViewComments.setVisibility(View.VISIBLE);
                tvNoComments.setVisibility(View.GONE);
            } else {
                recyclerViewComments.setVisibility(View.GONE);
                tvNoComments.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addComment() {
        String commentText = etComment.getText().toString().trim();
        if (commentText.isEmpty()) {
            etComment.setError("Enter your comment");
            return;
        }

        CropChartComment comment = new CropChartComment();
        comment.setCropChartId(chartId);
        comment.setExpertId(userId);
        comment.setExpertName(userName);
        comment.setComment(commentText);

        new Thread(() -> {
            database.cropChartCommentDao().insert(comment);
            runOnUiThread(() -> {
                etComment.setText("");
                Toast.makeText(this, "Comment added successfully", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void submitStarRating() {
        float stars = ratingBarExpert.getRating();
        if (stars < 1) {
            Toast.makeText(this, "Please select at least 1 star", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            CropChartStar existingStar = database.cropChartStarDao().getStarByExpert(chartId, userId);
            if (existingStar == null) {
                CropChartStar star = new CropChartStar();
                star.setCropChartId(chartId);
                star.setExpertId(userId);
                star.setStars(stars);
                database.cropChartStarDao().insert(star);
            } else {
                existingStar.setStars(stars);
                database.cropChartStarDao().update(existingStar);
            }
            runOnUiThread(() -> Toast.makeText(this, "Star rating submitted!", Toast.LENGTH_SHORT).show());
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCropChartDetails();
    }
}
