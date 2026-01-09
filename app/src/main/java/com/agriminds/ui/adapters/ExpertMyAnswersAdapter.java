package com.agriminds.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.AnswersActivity;
import com.agriminds.R;
import com.agriminds.data.AppDatabase;
import com.agriminds.ui.expert.ExpertDashboardActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpertMyAnswersAdapter extends RecyclerView.Adapter<ExpertMyAnswersAdapter.ViewHolder> {

    private List<ExpertDashboardActivity.QuestionWithReplies> answers;
    private Context context;
    private int expertId;
    private String expertName;
    private SimpleDateFormat dateFormat;
    private AppDatabase database;

    public ExpertMyAnswersAdapter(List<ExpertDashboardActivity.QuestionWithReplies> answers, Context context,
            int expertId, String expertName) {
        this.answers = answers != null ? answers : new ArrayList<>();
        this.context = context;
        this.expertId = expertId;
        this.expertName = expertName;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        this.database = AppDatabase.getInstance(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_answer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExpertDashboardActivity.QuestionWithReplies item = answers.get(position);

        holder.tvQuestionText.setText(item.question.getQuestionText());
        holder.tvFarmerName.setText("Asked by: " + item.question.getFarmerName());
        holder.tvDate.setText(getTimeAgo(item.answer.getCreatedAt()));
        holder.tvAnswerPreview.setText(item.answer.getAnswerText());

        // Show reply indicator
        if (item.replyCount > 0) {
            holder.tvReplyBadge.setVisibility(View.VISIBLE);
            holder.tvReplyBadge.setText(item.replyCount + " " + (item.replyCount == 1 ? "reply" : "replies"));
        } else {
            holder.tvReplyBadge.setVisibility(View.GONE);
        }

        // Load and display rating
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Float avgRating = database.expertRatingDao().getAverageRatingForAnswer(item.answer.getId());
            ((android.app.Activity) context).runOnUiThread(() -> {
                if (avgRating != null && avgRating > 0) {
                    holder.tvRating.setText(String.format(Locale.getDefault(), "%.1f★", avgRating));
                } else {
                    holder.tvRating.setText("0.0★");
                }
            });
        });

        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AnswersActivity.class);
            intent.putExtra("questionId", item.question.getId());
            intent.putExtra("questionText", item.question.getQuestionText());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return answers.size();
    }

    public void updateAnswers(List<ExpertDashboardActivity.QuestionWithReplies> newAnswers) {
        this.answers = newAnswers != null ? newAnswers : new ArrayList<>();
        notifyDataSetChanged();
    }

    private String getTimeAgo(long timestamp) {
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
        CardView cardView;
        TextView tvQuestionText, tvFarmerName, tvDate, tvAnswerPreview, tvReplyBadge, tvRating;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvQuestionText = itemView.findViewById(R.id.tvQuestionText);
            tvFarmerName = itemView.findViewById(R.id.tvFarmerName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAnswerPreview = itemView.findViewById(R.id.tvAnswerPreview);
            tvReplyBadge = itemView.findViewById(R.id.tvReplyBadge);
            tvRating = itemView.findViewById(R.id.tvRating);
        }
    }
}
