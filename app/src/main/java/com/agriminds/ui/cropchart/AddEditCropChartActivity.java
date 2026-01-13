package com.agriminds.ui.cropchart;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.agriminds.R;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.CropChart;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddEditCropChartActivity extends AppCompatActivity {

    private TextInputEditText etCropName, etSeason, etStartDate, etEndDate, etFertilizers;
    private TextInputEditText etSeedCost, etFertilizerCost, etLaborCost, etOtherCosts;
    private TextInputEditText etTotalYield, etSellPrice;
    private TextView tvTotalCost, tvTotalRevenue, tvProfit;
    private Button btnSave;

    private AppDatabase database;
    private int chartId = -1;
    private CropChart cropChart;
    private String farmerId, farmerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_crop_chart);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Crop Chart");
        }

        database = AppDatabase.getInstance(this);

        // Get farmer info
        SharedPreferences prefs = getSharedPreferences("AgriMindsPrefs", MODE_PRIVATE);
        farmerId = String.valueOf(prefs.getInt("userId", -1));
        farmerName = prefs.getString("userName", "Farmer");

        // Check if editing existing chart
        chartId = getIntent().getIntExtra("chartId", -1);
        if (chartId != -1) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Crop Chart");
            }
        }

        initializeViews();
        setupListeners();

        if (chartId != -1) {
            loadChartData();
        }
    }

    private void initializeViews() {
        etCropName = findViewById(R.id.etCropName);
        etSeason = findViewById(R.id.etSeason);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etFertilizers = findViewById(R.id.etFertilizers);

        etSeedCost = findViewById(R.id.etSeedCost);
        etFertilizerCost = findViewById(R.id.etFertilizerCost);
        etLaborCost = findViewById(R.id.etLaborCost);
        etOtherCosts = findViewById(R.id.etOtherCosts);

        etTotalYield = findViewById(R.id.etTotalYield);
        etSellPrice = findViewById(R.id.etSellPrice);

        tvTotalCost = findViewById(R.id.tvTotalCost);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvProfit = findViewById(R.id.tvProfit);

        btnSave = findViewById(R.id.btnSave);
    }

    private void setupListeners() {
        // Date pickers
        etStartDate.setOnClickListener(v -> showDatePicker(true));
        etEndDate.setOnClickListener(v -> showDatePicker(false));

        // Auto-calculate on text change
        TextWatcher calculationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                calculateFinancials();
            }
        };

        etSeedCost.addTextChangedListener(calculationWatcher);
        etFertilizerCost.addTextChangedListener(calculationWatcher);
        etLaborCost.addTextChangedListener(calculationWatcher);
        etOtherCosts.addTextChangedListener(calculationWatcher);
        etTotalYield.addTextChangedListener(calculationWatcher);
        etSellPrice.addTextChangedListener(calculationWatcher);

        btnSave.setOnClickListener(v -> saveCropChart());
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                    if (isStartDate) {
                        etStartDate.setText(date);
                    } else {
                        etEndDate.setText(date);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void calculateFinancials() {
        try {
            double seedCost = getDoubleValue(etSeedCost);
            double fertilizerCost = getDoubleValue(etFertilizerCost);
            double laborCost = getDoubleValue(etLaborCost);
            double otherCosts = getDoubleValue(etOtherCosts);
            double totalYield = getDoubleValue(etTotalYield);
            double sellPrice = getDoubleValue(etSellPrice);

            double totalCost = seedCost + fertilizerCost + laborCost + otherCosts;
            double totalRevenue = totalYield * sellPrice;
            double profit = totalRevenue - totalCost;

            // Use Taka symbol (৳) for all currency display
            String taka = "\u09F3";
            tvTotalCost.setText("Total Cost: " + taka + String.format("%,.2f", totalCost));
            tvTotalRevenue.setText("Total Revenue: " + taka + String.format("%,.2f", totalRevenue));
            tvProfit.setText("Profit: " + taka + String.format("%,.2f", profit));

            if (profit >= 0) {
                tvProfit.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvProfit.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        } catch (Exception e) {
            // Ignore parsing errors during input
        }
    }

    private double getDoubleValue(TextInputEditText editText) {
        String text = editText.getText().toString().trim();
        if (text.isEmpty())
            return 0;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void loadChartData() {
        database.cropChartDao().getCropChartById(chartId).observe(this, chart -> {
            if (chart != null) {
                cropChart = chart;
                etCropName.setText(chart.getCropName());
                etSeason.setText(chart.getSeason());
                etStartDate.setText(chart.getCultivationStartDate());
                etEndDate.setText(chart.getCultivationEndDate());
                etFertilizers.setText(chart.getFertilizersUsed());

                etSeedCost.setText(String.valueOf(chart.getSeedCost()));
                etFertilizerCost.setText(String.valueOf(chart.getFertilizerCost()));
                etLaborCost.setText(String.valueOf(chart.getLaborCost()));
                etOtherCosts.setText(String.valueOf(chart.getOtherCosts()));

                etTotalYield.setText(String.valueOf(chart.getTotalYield()));
                etSellPrice.setText(String.valueOf(chart.getSellPrice()));

                calculateFinancials();
            }
        });
    }

    private void saveCropChart() {
        // Validate inputs
        if (etCropName.getText().toString().trim().isEmpty()) {
            etCropName.setError("Enter crop name");
            return;
        }
        if (etSeason.getText().toString().trim().isEmpty()) {
            etSeason.setError("Enter season");
            return;
        }

        if (cropChart == null) {
            cropChart = new CropChart();
            cropChart.setFarmerId(farmerId);
            cropChart.setFarmerName(farmerName);
        }

        cropChart.setCropName(etCropName.getText().toString().trim());
        cropChart.setSeason(etSeason.getText().toString().trim());
        cropChart.setCultivationStartDate(etStartDate.getText().toString().trim());
        cropChart.setCultivationEndDate(etEndDate.getText().toString().trim());
        cropChart.setFertilizersUsed(etFertilizers.getText().toString().trim());

        cropChart.setSeedCost(getDoubleValue(etSeedCost));
        cropChart.setFertilizerCost(getDoubleValue(etFertilizerCost));
        cropChart.setLaborCost(getDoubleValue(etLaborCost));
        cropChart.setOtherCosts(getDoubleValue(etOtherCosts));

        cropChart.setTotalYield(getDoubleValue(etTotalYield));
        cropChart.setSellPrice(getDoubleValue(etSellPrice));

        cropChart.recalculate();

        new Thread(() -> {
            if (chartId == -1) {
                database.cropChartDao().insert(cropChart);
            } else {
                database.cropChartDao().update(cropChart);
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Crop chart saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
