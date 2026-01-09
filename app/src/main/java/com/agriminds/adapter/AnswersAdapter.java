package com.agriminds.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.R;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.ExpertAnswer;
import com.agriminds.data.entity.ExpertRating;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class AnswersAdapter extends RecyclerView.Adapter<AnswersAdapter.ViewHolder> {

    private List<ExpertAnswer> answers;
    private Context context;
    private int currentUserId;
    private AppDatabase database;

    public AnswersAdapter(List<ExpertAnswer> answers, Context context, int currentUserId) {
        this.answers = answers;
        this.context = context;
        this.currentUserId = currentUserId;
        this.database = AppDatabase.getInstance(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_answer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExpertAnswer answer = answers.get(position);

        holder.tvExpertName.setText(answer.getExpertName());
        holder.tvAnswerText.setText(answer.getAnswerText());
        holder.tvAnswerDate.setText(getRelativeTime(answer.getCreatedAt()));

        // Show AI badge if it's an AI answer
        if ("AI".equals(answer.getAnswerType())) {
            holder.tvAIBadge.setVisibility(View.VISIBLE);
            holder.ratingBar.setVisibility(View.GONE); // Can't rate AI
            holder.tvAverageRating.setVisibility(View.GONE);
            holder.btnDoneRating.setVisibility(View.GONE);
        } else {
            holder.tvAIBadge.setVisibility(View.GONE);
            holder.ratingBar.setVisibility(View.VISIBLE);

            // Load existing rating
            loadRating(answer, holder);
        }

        // Rating change listener - show Done button when rating changes
        holder.ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser && rating > 0) {
                holder.pendingRating = rating;
                holder.btnDoneRating.setVisibility(View.VISIBLE);
                // Turn clicked stars green immediately
                setRatingBarColor(holder.ratingBar, android.graphics.Color.GREEN);
            } else if (fromUser && rating == 0) {
                holder.btnDoneRating.setVisibility(View.GONE);
                holder.pendingRating = 0;
                // Reset to white when rating is removed
                setRatingBarColor(holder.ratingBar, android.graphics.Color.WHITE);
            }
        });

        // Done button click - submit the rating
        holder.btnDoneRating.setOnClickListener(v -> {
            if (holder.pendingRating > 0) {
                submitRating(answer, (int) holder.pendingRating, holder);
                holder.btnDoneRating.setVisibility(View.GONE);
            }
        });

        // Undo button click - clear rating and go back
        holder.btnUndoRating.setOnClickListener(v -> {
            // Clear rating from database
            AppDatabase.databaseWriteExecutor.execute(() -> {
                ExpertRating existingRating = database.expertRatingDao()
                        .getRatingByFarmerAndAnswer(currentUserId, answer.getId());

                if (existingRating != null) {
                    database.expertRatingDao().deleteRating(existingRating);
                }

                ((android.app.Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Rating removed", Toast.LENGTH_SHORT).show();
                    // Reload to reset the button
                    loadRating(answer, holder);
                });
            });
        });
    }

    private void loadRating(ExpertAnswer answer, ViewHolder holder) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Check if user already rated this answer
            ExpertRating existingRating = database.expertRatingDao()
                    .getRatingByFarmerAndAnswer(currentUserId, answer.getId());

            // Get average rating for this specific answer
            // Get expert's overall average rating (not just this answer)
            Float expertAvgRating = database.expertRatingDao()
                    .getAverageRating(answer.getExpertId());

            ((android.app.Activity) context).runOnUiThread(() -> {
                if (existingRating != null) {
                    holder.ratingBar.setRating(existingRating.getRating());
                    // Set green color for already submitted rating
                    setRatingBarColor(holder.ratingBar, android.graphics.Color.GREEN);
                } else {
                    holder.ratingBar.setRating(0);
                    // Set white color for unrated
                    setRatingBarColor(holder.ratingBar, android.graphics.Color.WHITE);
                }

                // Show expert's average rating beside expert name
                if (expertAvgRating != null && expertAvgRating > 0) {
                    holder.tvAverageRating.setVisibility(View.VISIBLE);
                    holder.tvAverageRating.setText(String.format("(%.1f★)", expertAvgRating));
                } else {
                    holder.tvAverageRating.setVisibility(View.GONE);
                }
            });
        });
    }

    private void submitRating(ExpertAnswer answer, int rating, ViewHolder holder) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Check if already rated
            ExpertRating existingRating = database.expertRatingDao()
                    .getRatingByFarmerAndAnswer(currentUserId, answer.getId());

            if (existingRating != null) {
                // Update existing rating
                existingRating.setRating(rating);
                database.expertRatingDao().updateRating(existingRating);
            } else {
                // Create new rating
                ExpertRating newRating = new ExpertRating();
                newRating.setExpertId(answer.getExpertId());
                newRating.setFarmerId(currentUserId);
                newRating.setAnswerId(answer.getId());
                newRating.setRating(rating);
                newRating.setCreatedAt(System.currentTimeMillis());
                database.expertRatingDao().insertRating(newRating);
            }

            ((android.app.Activity) context).runOnUiThread(() -> {
                Toast.makeText(context, "Rating submitted: " + rating + " stars",
                        Toast.LENGTH_SHORT).show();
                // Change to green after submitting
                setRatingBarColor(holder.ratingBar, android.graphics.Color.GREEN);
                loadRating(answer, holder); // Reload to show average rating
            });
        });
    }

    private void setRatingBarColor(RatingBar ratingBar, int color) {
        android.graphics.drawable.LayerDrawable stars = (android.graphics.drawable.LayerDrawable) ratingBar
                .getProgressDrawable();
        if (stars != null) {
            // Set filled stars color (the stars that are selected)
            stars.getDrawable(2).setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_ATOP);
            // Set empty stars to white (the stars that are not selected)
            stars.getDrawable(0).setColorFilter(android.graphics.Color.WHITE,
                    android.graphics.PorterDuff.Mode.SRC_ATOP);
        }
    }

    @Override
    public int getItemCount() {
        return answers.size();
    }

    public void updateAnswers(List<ExpertAnswer> newAnswers) {
        this.answers = newAnswers;
        notifyDataSetChanged();
    }

    private String getRelativeTime(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0)
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        if (hours > 0)
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        if (minutes > 0)
            return minutes + " min" + (minutes > 1 ? "s" : "") + " ago";
        return "Just now";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivExpertAvatar;
        TextView tvExpertName, tvAnswerDate, tvAnswerText, tvAIBadge, tvAverageRating;
        RatingBar ratingBar;
        MaterialButton btnDoneRating;
        MaterialButton btnUndoRating;
        float pendingRating = 0;

        ViewHolder(View itemView) {
            super(itemView);
            ivExpertAvatar = itemView.findViewById(R.id.ivExpertAvatar);
            tvExpertName = itemView.findViewById(R.id.tvExpertName);
            tvAnswerDate = itemView.findViewById(R.id.tvAnswerDate);
            tvAnswerText = itemView.findViewById(R.id.tvAnswerText);
            tvAIBadge = itemView.findViewById(R.id.tvAIBadge);
            tvAverageRating = itemView.findViewById(R.id.tvAverageRating);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            btnDoneRating = itemView.findViewById(R.id.btnDoneRating);
            btnUndoRating = itemView.findViewById(R.id.btnUndoRating);
        }
    }
}
