package com.agriminds.ui.cropchart;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.R;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.CropChart;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CropChartAdapter extends RecyclerView.Adapter<CropChartAdapter.ViewHolder> {

    private List<CropChart> cropCharts;
    private Context context;
    private boolean isExpertView;
    private AppDatabase database;

    public CropChartAdapter(List<CropChart> cropCharts, Context context, boolean isExpertView) {
        this.cropCharts = cropCharts;
        this.context = context;
        this.isExpertView = isExpertView;
        this.database = AppDatabase.getInstance(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_crop_chart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CropChart chart = cropCharts.get(position);
        String taka = "\u09F3";

        holder.tvCropName.setText(chart.getCropName());
        holder.tvSeason.setText(chart.getSeason());

        String dateRange = formatDate(chart.getCultivationStartDate()) + " - "
                + formatDate(chart.getCultivationEndDate());
        holder.tvDate.setText(dateRange);

        holder.tvProfit.setText(taka + String.format("%,.2f", chart.getProfit()));
        if (chart.getProfit() >= 0) {
            holder.tvProfit.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.tvProfit.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        }

        holder.tvYield.setText("Yield: " + chart.getTotalYield() + " quintals");

        // Show share status if shared
        if (chart.isShared()) {
            holder.ivShareStatus.setVisibility(View.VISIBLE);
            holder.btnShareChart.setVisibility(View.GONE);
        } else {
            holder.ivShareStatus.setVisibility(View.GONE);
            holder.btnShareChart.setVisibility(View.VISIBLE);
            holder.btnShareChart.setOnClickListener(v -> {
                // Update DB to set shared
                new Thread(() -> {
                    // 1. Fetch latest to ensure thread safety
                    CropChart latestChart = database.cropChartDao().getCropChartByIdSync(chart.getId());
                    if (latestChart == null)
                        return;

                    // 2. Update shared status
                    database.cropChartDao().updateShareStatus(chart.getId(), true);

                    // 3. Update 'Persistent' Stats if sharing for the first time
                    if (!latestChart.isHasEverBeenShared()) {
                        try {
                            // Re-enable persistent user stats as requested
                            database.userDao().incrementChartsShared(Integer.parseInt(chart.getFarmerId()));
                            database.cropChartDao().updateHasEverBeenShared(chart.getId(), true);
                            latestChart.setHasEverBeenShared(true);
                            chart.setHasEverBeenShared(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    chart.setShared(true);
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        holder.ivShareStatus.setVisibility(View.VISIBLE);
                        holder.btnShareChart.setVisibility(View.GONE);
                        notifyItemChanged(position);
                        android.widget.Toast.makeText(context, "Shared with experts successfully",
                                android.widget.Toast.LENGTH_SHORT).show();
                    });
                }).start();
            });
        }

        // Delete button logic
        if (holder.btnDeleteChart != null) {
            holder.btnDeleteChart.setOnClickListener(v -> {
                new Thread(() -> {
                    database.cropChartDao().delete(chart);
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        cropCharts.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, cropCharts.size());
                        android.widget.Toast
                                .makeText(context, "Chart deleted successfully", android.widget.Toast.LENGTH_SHORT)
                                .show();
                    });
                }).start();
            });
        }

        // Get comment count
        if (context instanceof LifecycleOwner) {
            LiveData<Integer> commentCountLiveData = database.cropChartCommentDao().getCommentCount(chart.getId());
            // Show average stars for all users (Farmers and Experts)
            holder.layoutAverageStars.setVisibility(View.VISIBLE);
            new Thread(() -> {
                Float avgStars = database.cropChartStarDao().getAverageStars(chart.getId());
                ((android.app.Activity) context).runOnUiThread(() -> {
                    if (avgStars != null && avgStars > 0) {
                        holder.tvAverageStars.setText("★ " + String.format("%.1f", avgStars));
                        holder.layoutAverageStars.setVisibility(View.VISIBLE);
                    } else {
                        holder.tvAverageStars.setText("★ 0.0");
                        holder.layoutAverageStars.setVisibility(View.VISIBLE);
                    }
                });
            }).start();

            commentCountLiveData.observe((LifecycleOwner) context, count -> {
                if (count != null && count > 0) {
                    holder.tvCommentCount.setText(count + " Comment" + (count > 1 ? "s" : ""));
                    holder.tvCommentCount.setVisibility(View.VISIBLE);
                } else {
                    holder.tvCommentCount.setVisibility(View.GONE);
                }
            });
        }

        // For expert view, show farmer name
        if (isExpertView && chart.getFarmerName() != null) {
            holder.tvSeason.setText(holder.tvSeason.getText() + " • " + chart.getFarmerName());
            // Experts cannot delete charts
            if (holder.btnDeleteChart != null) {
                holder.btnDeleteChart.setVisibility(View.GONE);
            }
        }

        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CropChartDetailActivity.class);
            intent.putExtra("chartId", chart.getId());
            context.startActivity(intent);
        });
        holder.btnSeeDetails.setOnClickListener(v -> {
            Intent intent = new Intent(context, CropChartDetailActivity.class);
            intent.putExtra("chartId", chart.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return cropCharts.size();
    }

    public void updateData(List<CropChart> newData) {
        this.cropCharts = newData;
        notifyDataSetChanged();
    }

    LinearLayout layoutAverageStars;
    TextView tvAverageStars, tvStarLabel;

    private String formatDate(String date) {
        if (date == null || date.isEmpty()) {
            return "";
        }
        // Simple date formatting - you can enhance this
        return date;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvCropName, tvSeason, tvDate, tvProfit, tvYield, tvCommentCount;
        LinearLayout layoutAverageStars;
        TextView tvAverageStars, tvStarLabel;
        ImageView ivShareStatus;
        Button btnShareChart;
        Button btnSeeDetails;
        Button btnDeleteChart;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            tvCropName = itemView.findViewById(R.id.tvCropName);
            tvSeason = itemView.findViewById(R.id.tvSeason);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvProfit = itemView.findViewById(R.id.tvProfit);
            tvYield = itemView.findViewById(R.id.tvYield);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            layoutAverageStars = itemView.findViewById(R.id.layoutAverageStars);
            tvAverageStars = itemView.findViewById(R.id.tvAverageStars);
            tvStarLabel = itemView.findViewById(R.id.tvStarLabel);
            ivShareStatus = itemView.findViewById(R.id.ivShareStatus);
            btnShareChart = itemView.findViewById(R.id.btnShareChart);
            btnSeeDetails = itemView.findViewById(R.id.btnSeeDetails);
            btnDeleteChart = itemView.findViewById(R.id.btnDeleteChart);
        }
    }
}
