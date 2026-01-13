package com.agriminds.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.agriminds.MyQuestionsActivity;
import com.agriminds.R;
import com.agriminds.ui.askexpert.AskExpertActivity;
import com.agriminds.ui.myfarm.MyFarmActivity;
import com.agriminds.ui.scancrop.ScanCropActivity;
import com.agriminds.ui.soiltest.SoilTestActivity;

public class DashboardFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Set up quick action cards with direct click listeners
        CardView cardSoilTest = root.findViewById(R.id.card_soil_test);
        CardView cardScanCrop = root.findViewById(R.id.card_scan_crop);
        CardView cardAskExpert = root.findViewById(R.id.card_ask_expert);
        CardView cardMyFarm = root.findViewById(R.id.card_my_farm);
        CardView cardMyQuestions = root.findViewById(R.id.card_my_questions);

        cardSoilTest.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), SoilTestActivity.class));
        });

        cardScanCrop.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ScanCropActivity.class));
        });

        cardAskExpert.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AskExpertActivity.class));
        });

        cardMyFarm.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), MyFarmActivity.class));
        });

        cardMyQuestions.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), MyQuestionsActivity.class));
        });

        return root;
    }

}
