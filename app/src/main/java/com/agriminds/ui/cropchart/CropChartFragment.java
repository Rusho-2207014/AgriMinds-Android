package com.agriminds.ui.cropchart;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.R;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.CropChart;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class CropChartFragment extends Fragment {

    private RecyclerView recyclerViewCropCharts;
    private TextView emptyView;
    private FloatingActionButton fabAddCrop;
    private CropChartAdapter adapter;
    private AppDatabase database;
    private String farmerId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_crop_chart, container, false);

        // Get farmer ID from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("AgriMindsPrefs", Context.MODE_PRIVATE);
        farmerId = String.valueOf(prefs.getInt("userId", -1));

        database = AppDatabase.getInstance(requireContext());

        recyclerViewCropCharts = root.findViewById(R.id.recyclerViewCropCharts);
        emptyView = root.findViewById(R.id.emptyView);
        fabAddCrop = root.findViewById(R.id.fabAddCrop);

        recyclerViewCropCharts.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CropChartAdapter(new ArrayList<>(), requireContext(), false);
        recyclerViewCropCharts.setAdapter(adapter);

        // Load crop charts
        loadCropCharts();

        // Add new crop chart
        fabAddCrop.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditCropChartActivity.class);
            startActivity(intent);
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCropCharts();
    }

    private void loadCropCharts() {
        LiveData<List<CropChart>> cropChartsLiveData = database.cropChartDao().getCropChartsByFarmer(farmerId);
        cropChartsLiveData.observe(getViewLifecycleOwner(), cropCharts -> {
            if (cropCharts != null && !cropCharts.isEmpty()) {
                adapter.updateData(cropCharts);
                recyclerViewCropCharts.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            } else {
                recyclerViewCropCharts.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        });
    }
}
