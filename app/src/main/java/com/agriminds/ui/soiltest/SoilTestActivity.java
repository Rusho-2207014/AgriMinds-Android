package com.agriminds.ui.soiltest;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.agriminds.R;
import com.google.android.material.textfield.TextInputEditText;

public class SoilTestActivity extends AppCompatActivity {

    private TextInputEditText editFieldName, editPhLevel, editNitrogen, editPhosphorus, editPotassium;
    private Button btnAnalyze;
    private TextView textResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soil_test);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Soil Test");
        }

        editFieldName = findViewById(R.id.edit_field_name);
        editPhLevel = findViewById(R.id.edit_ph_level);
        editNitrogen = findViewById(R.id.edit_nitrogen);
        editPhosphorus = findViewById(R.id.edit_phosphorus);
        editPotassium = findViewById(R.id.edit_potassium);
        btnAnalyze = findViewById(R.id.btn_analyze_soil);
        textResult = findViewById(R.id.text_soil_result);

        btnAnalyze.setOnClickListener(v -> analyzeSoil());
    }

    private void analyzeSoil() {
        String fieldName = editFieldName.getText().toString().trim();
        String phStr = editPhLevel.getText().toString().trim();
        String nStr = editNitrogen.getText().toString().trim();
        String pStr = editPhosphorus.getText().toString().trim();
        String kStr = editPotassium.getText().toString().trim();

        if (fieldName.isEmpty() || phStr.isEmpty() || nStr.isEmpty() || pStr.isEmpty() || kStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double ph = Double.parseDouble(phStr);
            double n = Double.parseDouble(nStr);
            double p = Double.parseDouble(pStr);
            double k = Double.parseDouble(kStr);

            StringBuilder result = new StringBuilder();
            result.append("Soil Analysis for ").append(fieldName).append("\n\n");

            // pH Analysis
            result.append("pH Level: ").append(ph).append(" - ");
            if (ph < 6.0) {
                result.append("Acidic. Add lime to raise pH.\n");
            } else if (ph > 7.5) {
                result.append("Alkaline. Add sulfur to lower pH.\n");
            } else {
                result.append("Optimal range.\n");
            }

            // NPK Analysis
            result.append("\nNPK Levels:\n");
            result.append("Nitrogen: ").append(n).append("% - ");
            result.append(n < 1.5 ? "Low. Add urea or compost.\n" : "Adequate.\n");

            result.append("Phosphorus: ").append(p).append("% - ");
            result.append(p < 0.5 ? "Low. Add bone meal or phosphate.\n" : "Adequate.\n");

            result.append("Potassium: ").append(k).append("% - ");
            result.append(k < 1.0 ? "Low. Add potash fertilizer.\n" : "Adequate.\n");

            // Crop recommendations
            result.append("\nRecommended Crops:\n");
            boolean hasCrops = false;

            // Rice, Wheat, Corn (pH 6.0-7.0, good nitrogen)
            if (ph >= 6.0 && ph <= 7.0 && n >= 1.5) {
                result.append("• Rice, Wheat, Corn\n");
                hasCrops = true;
            }

            // Potato, Tomato (pH 5.5-6.5)
            if (ph >= 5.5 && ph <= 6.5) {
                result.append("• Potato, Tomato, Eggplant\n");
                hasCrops = true;
            }

            // Cabbage, Cauliflower (pH 6.5-7.5)
            if (ph >= 6.5 && ph <= 7.5) {
                result.append("• Cabbage, Cauliflower, Broccoli\n");
                hasCrops = true;
            }

            // Legumes (pH 6.0-7.0)
            if (ph >= 6.0 && ph <= 7.0) {
                result.append("• Beans, Peas, Lentils\n");
                hasCrops = true;
            }

            // General vegetables (pH 6.0-7.0)
            if (ph >= 6.0 && ph <= 7.0) {
                result.append("• Cucumber, Pumpkin, Squash\n");
                hasCrops = true;
            }

            // Acidic soil crops
            if (ph < 6.0) {
                result.append("• Blueberry, Cranberry, Radish\n");
                hasCrops = true;
            }

            // Alkaline tolerant crops
            if (ph > 7.0) {
                result.append("• Spinach, Beet, Asparagus\n");
                hasCrops = true;
            }

            if (!hasCrops) {
                result.append("• Adjust soil pH first for better crop options\n");
            }

            textResult.setText(result.toString());
            textResult.setVisibility(View.VISIBLE);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
