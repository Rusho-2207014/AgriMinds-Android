package com.agriminds.ui.dashboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.agriminds.MyQuestionsActivity;
import com.agriminds.R;
import com.agriminds.ui.askexpert.AskExpertActivity;
import com.agriminds.ui.login.LoginActivity;
import com.agriminds.ui.myfarm.MyFarmActivity;
import com.agriminds.ui.scancrop.ScanCropActivity;
import com.agriminds.ui.soiltest.SoilTestActivity;

import static android.content.Context.MODE_PRIVATE;

public class DashboardFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Set up quick action buttons
        Button btnSoilTest = root.findViewById(R.id.btn_soil_test);
        Button btnScanCrop = root.findViewById(R.id.btn_scan_crop);
        Button btnAskExpert = root.findViewById(R.id.btn_ask_expert);
        Button btnMyFarm = root.findViewById(R.id.btn_my_farm);
        Button btnMyQuestions = root.findViewById(R.id.btn_my_questions);
        Button btnLogout = root.findViewById(R.id.btn_logout);

        btnSoilTest.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SoilTestActivity.class);
            startActivity(intent);
        });

        btnScanCrop.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ScanCropActivity.class);
            startActivity(intent);
        });

        btnAskExpert.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AskExpertActivity.class);
            startActivity(intent);
        });

        btnMyFarm.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyFarmActivity.class);
            startActivity(intent);
        });

        btnMyQuestions.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyQuestionsActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            logout();
        });

        return root;
    }

    private void logout() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("AgriMindsPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
        getActivity().finish();
    }
}
