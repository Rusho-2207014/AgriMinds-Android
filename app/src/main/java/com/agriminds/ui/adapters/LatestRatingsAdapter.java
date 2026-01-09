package com.agriminds.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.R;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.ExpertAnswer;
import com.agriminds.data.entity.ExpertRating;
import com.agriminds.data.entity.Question;
import com.agriminds.data.entity.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LatestRatingsAdapter extends RecyclerView.Adapter<LatestRatingsAdapter.ViewHolder> {

    public static class RatingItem {
        public ExpertRating rating;
        public String farmerName;
        public String questionText;

        public RatingItem(ExpertRating rating, String farmerName, String questionText) {
            this.rating = rating;
            this.farmerName = farmerName;
            this.questionText = questionText;
        }
    }

    private List<RatingItem> ratings;
    private Context context;
    private SimpleDateFormat dateFormat;

    public LatestRatingsAdapter(List<RatingItem> ratings, Context context) {
        this.ratings = ratings != null ? ratings : new ArrayList<>();
        this.context = context;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_latest_rating, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RatingItem item = ratings.get(position);

        holder.tvFarmerName.setText(item.farmerName);
        holder.ratingBar.setRating(item.rating.getRating());
        holder.tvQuestionPreview.setText(item.questionText);
        holder.tvDate.setText(getTimeAgo(item.rating.getCreatedAt()));
    }

    @Override
    public int getItemCount() {
        return ratings.size();
    }

    public void updateRatings(List<RatingItem> newRatings) {
        this.ratings = newRatings != null ? newRatings : new ArrayList<>();
        notifyDataSetChanged();
    }

    private String getTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long days = diff / (1000 * 60 * 60 * 24);

        if (days == 0)
            return "Today";
        if (days == 1)
            return "Yesterday";
        if (days < 7)
            return days + " days ago";
        return dateFormat.format(new Date(timestamp));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFarmerName, tvQuestionPreview, tvDate;
        RatingBar ratingBar;

        ViewHolder(View itemView) {
            super(itemView);
            tvFarmerName = itemView.findViewById(R.id.tvFarmerName);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvQuestionPreview = itemView.findViewById(R.id.tvQuestionPreview);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}
