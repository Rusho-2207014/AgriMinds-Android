package com.agriminds.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.agriminds.R;
import com.agriminds.data.AgrimindsRepository;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.User;
import com.agriminds.databinding.ActivityLoginBinding;
import com.agriminds.ui.MainActivity;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AgrimindsRepository repository;
    private SharedPreferences sharedPreferences;
    private String selectedUserType = "FARMER"; // Default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        sharedPreferences = getSharedPreferences("AgriMindsPrefs", MODE_PRIVATE);

        // Clean up excess saved accounts immediately (keep only 5)
        cleanupExcessAccounts();

        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            navigateToMainActivity();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        repository = new AgrimindsRepository(db);

        // Ensure test users exist synchronously
        ensureTestUsersExist(db);

        binding.btnLoginFarmer.setOnClickListener(v -> {
            selectedUserType = "FARMER";
            binding.tvUserType.setText("Logging in as: Farmer");
            performLogin();
        });

        binding.btnLoginExpert.setOnClickListener(v -> {
            selectedUserType = "EXPERT";
            binding.tvUserType.setText("Logging in as: Expert");
            performLogin();
        });

        binding.tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void cleanupExcessAccounts() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Get existing accounts
        java.util.List<String> existingEmails = new java.util.ArrayList<>();
        java.util.List<String> existingTypes = new java.util.ArrayList<>();
        java.util.List<Integer> existingUserIds = new java.util.ArrayList<>();
        java.util.List<String> existingUserNames = new java.util.ArrayList<>();

        // Read all existing accounts (check up to 20)
        for (int i = 0; i < 20; i++) {
            String email = sharedPreferences.getString("recent_email_" + i, null);
            if (email != null) {
                existingEmails.add(email);
                existingTypes.add(sharedPreferences.getString("recent_type_" + i, ""));
                existingUserIds.add(sharedPreferences.getInt("recent_userId_" + i, -1));
                existingUserNames.add(sharedPreferences.getString("recent_userName_" + i, ""));
            }
        }

        // Clear all entries
        for (int i = 0; i < 20; i++) {
            editor.remove("recent_email_" + i);
            editor.remove("recent_type_" + i);
            editor.remove("recent_userId_" + i);
            editor.remove("recent_userName_" + i);
        }

        // Save only the first 5
        int limit = Math.min(existingEmails.size(), 5);
        for (int i = 0; i < limit; i++) {
            editor.putString("recent_email_" + i, existingEmails.get(i));
            editor.putString("recent_type_" + i, existingTypes.get(i));
            editor.putInt("recent_userId_" + i, existingUserIds.get(i));
            editor.putString("recent_userName_" + i, existingUserNames.get(i));
        }

        editor.apply();
    }

    private void ensureTestUsersExist(AppDatabase db) {
        // Run synchronously on main thread since we allow main thread queries
        try {
            // Check and create test farmer
            com.agriminds.data.entity.User existingFarmer = db.userDao().getUserByEmail("farmer@test.com");
            if (existingFarmer == null) {
                com.agriminds.data.entity.User farmer = new com.agriminds.data.entity.User();
                farmer.fullName = "Test Farmer";
                farmer.email = "farmer@test.com";
                farmer.password = "123456";
                farmer.phone = "01712345678";
                farmer.userType = "FARMER";
                farmer.district = "Dhaka";
                farmer.upazila = "Savar";
                farmer.farmSize = "5 acres";
                farmer.farmingType = "Rice & Vegetables";
                db.userDao().insert(farmer);
                android.util.Log.d("LoginActivity", "Created test farmer: farmer@test.com");
            } else {
                android.util.Log.d("LoginActivity", "Test farmer already exists - Email: " + existingFarmer.email
                        + ", Password: " + existingFarmer.password + ", Type: " + existingFarmer.userType);
            }

            // Check and create test expert
            com.agriminds.data.entity.User existingExpert = db.userDao().getUserByEmail("expert@test.com");
            if (existingExpert == null) {
                com.agriminds.data.entity.User expert = new com.agriminds.data.entity.User();
                expert.fullName = "Dr. Expert";
                expert.email = "expert@test.com";
                expert.password = "123456";
                expert.phone = "01798765432";
                expert.userType = "EXPERT";
                expert.specialization = "Crop Diseases";
                expert.experience = "10 years";
                expert.organization = "Agricultural Institute";
                db.userDao().insert(expert);
                android.util.Log.d("LoginActivity", "Created test expert: expert@test.com");
            } else {
                android.util.Log.d("LoginActivity", "Test expert already exists - Email: " + existingExpert.email
                        + ", Password: " + existingExpert.password + ", Type: " + existingExpert.userType);
            }

            // Show toast to inform users
            Toast.makeText(this, "Test accounts ready: farmer@test.com / expert@test.com (password: 123456)",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "Error creating test users: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void performLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading message
        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();

        // Perform Login Logic in background
        Executors.newSingleThreadExecutor().execute(() -> {
            User user = repository.getUserByEmailAndPassword(email, password);

            runOnUiThread(() -> {
                if (user != null) {
                    if (user.userType.equals(selectedUserType)) {
                        // Save login state
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("isLoggedIn", true);
                        editor.putString("userEmail", email);
                        editor.putString("userType", user.userType);
                        editor.putInt("userId", user.id);
                        editor.putString("userName", user.fullName);

                        // Manage recent accounts - keep only latest 5
                        manageRecentAccounts(email, user.userType, user.id, user.fullName);

                        editor.apply();

                        Toast.makeText(this, "Login Successful! Welcome " + user.fullName, Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        Toast.makeText(this, "Wrong user type. You are registered as " + user.userType
                                + ". Please select correct login button.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "Invalid credentials. Please register if you don't have an account.",
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void manageRecentAccounts(String email, String userType, int userId, String userName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Get existing recent accounts (stored as JSON-like string)
        java.util.List<String> recentAccounts = new java.util.ArrayList<>();

        // Load up to 5 recent accounts
        for (int i = 0; i < 5; i++) {
            String savedEmail = sharedPreferences.getString("recent_email_" + i, null);
            if (savedEmail != null && !savedEmail.equals(email)) {
                recentAccounts.add(savedEmail);
            }
        }

        // Add current email at the beginning (most recent)
        recentAccounts.add(0, email);

        // Keep only latest 5
        if (recentAccounts.size() > 5) {
            recentAccounts = recentAccounts.subList(0, 5);
        }

        // Clear ALL old entries (up to 20 to clean any legacy data)
        for (int i = 0; i < 20; i++) {
            editor.remove("recent_email_" + i);
            editor.remove("recent_type_" + i);
            editor.remove("recent_userId_" + i);
            editor.remove("recent_userName_" + i);
        }

        // Save new recent accounts (only up to 5)
        for (int i = 0; i < recentAccounts.size(); i++) {
            if (recentAccounts.get(i).equals(email)) {
                editor.putString("recent_email_" + i, email);
                editor.putString("recent_type_" + i, userType);
                editor.putInt("recent_userId_" + i, userId);
                editor.putString("recent_userName_" + i, userName);
            } else {
                // Keep the old data for this account
                String oldEmail = recentAccounts.get(i);
                for (int j = 0; j < 5; j++) {
                    String checkEmail = sharedPreferences.getString("recent_email_" + j, null);
                    if (oldEmail.equals(checkEmail)) {
                        editor.putString("recent_email_" + i, oldEmail);
                        editor.putString("recent_type_" + i, sharedPreferences.getString("recent_type_" + j, ""));
                        editor.putInt("recent_userId_" + i, sharedPreferences.getInt("recent_userId_" + j, -1));
                        editor.putString("recent_userName_" + i,
                                sharedPreferences.getString("recent_userName_" + j, ""));
                        break;
                    }
                }
            }
        }

        editor.apply();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
