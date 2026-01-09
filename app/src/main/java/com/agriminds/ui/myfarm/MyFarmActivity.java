package com.agriminds.ui.myfarm;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.agriminds.R;

public class MyFarmActivity extends AppCompatActivity {

    private Button btnAddCrop, btnViewHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_farm);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Farm");
        }

        btnAddCrop = findViewById(R.id.btn_add_crop);
        btnViewHistory = findViewById(R.id.btn_view_history);

        btnAddCrop.setOnClickListener(
                v -> Toast.makeText(this, "Add crop functionality coming soon!", Toast.LENGTH_SHORT).show());

        btnViewHistory.setOnClickListener(
                v -> Toast.makeText(this, "Harvest history feature coming soon!", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
