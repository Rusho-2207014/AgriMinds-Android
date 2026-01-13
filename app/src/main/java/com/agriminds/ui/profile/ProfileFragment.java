package com.agriminds.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.agriminds.R;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.CropChart;
import com.agriminds.data.entity.Question;
import com.agriminds.data.entity.User;
import com.agriminds.ui.login.LoginActivity;

import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class ProfileFragment extends Fragment {

    private TextView textUserName, textUserType;
    private TextView tvEmail, tvPhone, tvLocation, tvFarmDetails;
    private TextView tvQuestionsAsked, tvAnswersReceived, tvChartsShared, tvAvgRating;
    private AppDatabase database;
    private String userIdStr;
    private int userId;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        database = AppDatabase.getInstance(requireContext());
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("AgriMindsPrefs", MODE_PRIVATE);
        userIdStr = String.valueOf(sharedPreferences.getInt("userId", -1));
        userId = sharedPreferences.getInt("userId", -1);
        String userType = sharedPreferences.getString("userType", "FARMER");

        initializeViews(root);

        textUserType.setText(userType);

        loadUserProfile();
        loadUserStats();

        Button btnLogout = root.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            logout();
        });

        return root;
    }

    private void initializeViews(View root) {
        textUserName = root.findViewById(R.id.text_user_name);
        textUserType = root.findViewById(R.id.text_user_type);

        tvEmail = root.findViewById(R.id.tvEmail);
        tvPhone = root.findViewById(R.id.tvPhone);
        tvLocation = root.findViewById(R.id.tvLocation);
        tvFarmDetails = root.findViewById(R.id.tvFarmDetails);

        tvQuestionsAsked = root.findViewById(R.id.tvQuestionsAsked);
        tvAnswersReceived = root.findViewById(R.id.tvAnswersReceived);
        tvChartsShared = root.findViewById(R.id.tvChartsShared);
        tvAvgRating = root.findViewById(R.id.tvAvgRating);
    }

    private void loadUserProfile() {
        if (userId == -1)
            return;

        new Thread(() -> {
            com.agriminds.data.entity.User user = database.userDao().getUserById(userId);
            if (user != null) {
                getActivity().runOnUiThread(() -> {
                    textUserName.setText(user.fullName);
                    tvEmail.setText(user.email != null ? user.email : "Not provided");
                    tvPhone.setText(user.phone != null ? user.phone : "Not provided");

                    String location = "";
                    if (user.district != null)
                        location += user.district;
                    if (user.upazila != null)
                        location += (location.isEmpty() ? "" : ", ") + user.upazila;
                    tvLocation.setText(location.isEmpty() ? "Location not set" : location);

                    String farmInfo = "";
                    if (user.farmSize != null)
                        farmInfo += user.farmSize + " Acres";
                    if (user.farmingType != null)
                        farmInfo += (farmInfo.isEmpty() ? "" : " • ") + user.farmingType;
                    tvFarmDetails.setText(farmInfo.isEmpty() ? "Farm details not set" : farmInfo);
                });
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
        refreshOneShotStats();
    }

    private void loadUserStats() {
        // Stats are now loaded via refreshOneShotStats() in onResume or onCreate
    }

    private void refreshOneShotStats() {
        // 1. Questions & Answers (Background Thread) - Now fetched directly from User
        // entity
        new Thread(() -> {
            com.agriminds.data.entity.User user = database.userDao().getUserById(userId);
            if (user == null)
                return;

            int questionCount = user.totalQuestionsAsked;
            int answerCount = user.totalAnswersReceived;
            // Use persistent count from users table as requested
            int sharedCount = user.totalChartsShared;

            // 2. Average Rating (Background Thread)
            Float avgRating = database.cropChartStarDao().getAverageStarsByFarmer(userIdStr);
            float finalAvgRating = avgRating != null ? avgRating : 0.0f;

            getActivity().runOnUiThread(() -> {
                tvQuestionsAsked.setText(String.valueOf(questionCount));
                tvAnswersReceived.setText(String.valueOf(answerCount));
                tvChartsShared.setText(String.valueOf(sharedCount));
                tvAvgRating.setText(String.format("%.1f", finalAvgRating));
            });
        }).start();
    }

    private void logout() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("AgriMindsPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}
