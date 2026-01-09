package com.agriminds.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.agriminds.R;
import com.agriminds.data.AgrimindsRepository;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.User;
import com.agriminds.ui.MainActivity;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etEmail, etPhone, etPassword;
    private TextInputEditText etDistrict, etUpazila, etFarmSize, etFarmingType;
    private TextInputEditText etSpecialization, etExperience, etOrganization;
    private Button btnSelectFarmer, btnSelectExpert, btnRegister;
    private TextView tvRegisterType, tvLogin;
    private LinearLayout layoutFarmerFields, layoutExpertFields;

    private String selectedUserType = "FARMER";
    private AgrimindsRepository repository;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        repository = new AgrimindsRepository(db);
        sharedPreferences = getSharedPreferences("AgriMindsPrefs", MODE_PRIVATE);

        // Initialize views
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);

        // Farmer fields
        etDistrict = findViewById(R.id.et_district);
        etUpazila = findViewById(R.id.et_upazila);
        etFarmSize = findViewById(R.id.et_farm_size);
        etFarmingType = findViewById(R.id.et_farming_type);

        // Expert fields
        etSpecialization = findViewById(R.id.et_specialization);
        etExperience = findViewById(R.id.et_experience);
        etOrganization = findViewById(R.id.et_organization);

        btnSelectFarmer = findViewById(R.id.btn_select_farmer);
        btnSelectExpert = findViewById(R.id.btn_select_expert);
        btnRegister = findViewById(R.id.btn_register);
        tvRegisterType = findViewById(R.id.tv_register_type);
        tvLogin = findViewById(R.id.tv_login);

        layoutFarmerFields = findViewById(R.id.layout_farmer_fields);
        layoutExpertFields = findViewById(R.id.layout_expert_fields);

        btnSelectFarmer.setOnClickListener(v -> selectUserType("FARMER"));
        btnSelectExpert.setOnClickListener(v -> selectUserType("EXPERT"));
        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void selectUserType(String userType) {
        selectedUserType = userType;

        if (userType.equals("FARMER")) {
            tvRegisterType.setText("Register as Farmer");
            btnSelectFarmer.setBackgroundTintList(getColorStateList(android.R.color.holo_green_dark));
            btnSelectExpert.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
            layoutFarmerFields.setVisibility(View.VISIBLE);
            layoutExpertFields.setVisibility(View.GONE);
        } else {
            tvRegisterType.setText("Register as Expert");
            btnSelectFarmer.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
            btnSelectExpert.setBackgroundTintList(getColorStateList(android.R.color.holo_orange_dark));
            layoutFarmerFields.setVisibility(View.GONE);
            layoutExpertFields.setVisibility(View.VISIBLE);
        }
    }

    private void registerUser() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = new User();
        user.fullName = fullName;
        user.email = email;
        user.phone = phone;
        user.password = password;
        user.userType = selectedUserType;

        if (selectedUserType.equals("FARMER")) {
            user.district = etDistrict.getText().toString().trim();
            user.upazila = etUpazila.getText().toString().trim();
            user.farmSize = etFarmSize.getText().toString().trim();
            user.farmingType = etFarmingType.getText().toString().trim();

            if (user.district.isEmpty() || user.upazila.isEmpty()) {
                Toast.makeText(this, "Please fill farmer details", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            user.specialization = etSpecialization.getText().toString().trim();
            user.experience = etExperience.getText().toString().trim();
            user.organization = etOrganization.getText().toString().trim();

            if (user.specialization.isEmpty() || user.experience.isEmpty()) {
                Toast.makeText(this, "Please fill expert details", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Register user in background
        Executors.newSingleThreadExecutor().execute(() -> {
            android.util.Log.d("RegisterActivity", "Registering user: " + email + ", Type: " + selectedUserType);

            // Check if email already exists
            User existingUserByEmail = repository.getUserByEmail(email);
            if (existingUserByEmail != null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Email already registered. Please use a different email.", Toast.LENGTH_LONG)
                            .show();
                });
                return;
            }

            // Check if phone number already exists
            User existingUserByPhone = repository.getUserByPhone(phone);
            if (existingUserByPhone != null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Phone number already registered. Please use a different number.",
                            Toast.LENGTH_LONG).show();
                });
                return;
            }

            // Check if name already exists
            User existingUserByName = repository.getUserByName(fullName);
            if (existingUserByName != null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Name already registered. Please use a different name.", Toast.LENGTH_LONG)
                            .show();
                });
                return;
            }

            long userId = repository.insertUser(user);
            android.util.Log.d("RegisterActivity", "User registered with ID: " + userId);

            runOnUiThread(() -> {
                if (userId > 0) {
                    // Auto-login after registration
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.putString("userEmail", email);
                    editor.putString("userType", selectedUserType);
                    editor.putInt("userId", (int) userId);
                    editor.putString("userName", fullName);
                    editor.apply();

                    Toast.makeText(this, "Registration Successful! Welcome " + fullName, Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Registration failed. Email might already exist.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
