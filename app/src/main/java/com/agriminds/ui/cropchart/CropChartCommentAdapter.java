package com.agriminds.ui.cropchart;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.R;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.CropChartComment;
import com.agriminds.data.entity.CropChartReply;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CropChartCommentAdapter extends RecyclerView.Adapter<CropChartCommentAdapter.ViewHolder> {

    private List<CropChartComment> comments;
    private Context context;
    private AppDatabase database;
    private String userId, userName, userType;

    public CropChartCommentAdapter(List<CropChartComment> comments, Context context, String userId, String userName,
            String userType) {
        this.comments = comments;
        this.context = context;
        this.userId = userId;
        this.userName = userName;
        this.userType = userType;
        this.database = AppDatabase.getInstance(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_crop_chart_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CropChartComment comment = comments.get(position);
        holder.tvExpertName.setText(comment.getExpertName());
        holder.tvComment.setText(comment.getComment());
        holder.tvTimestamp.setText(getTimeAgo(comment.getTimestamp()));

        // Fetch and show expert rating
        new Thread(() -> {
            com.agriminds.data.entity.CropChartStar star = database.cropChartStarDao()
                    .getStarByExpert(comment.getCropChartId(), comment.getExpertId());
            ((android.app.Activity) context).runOnUiThread(() -> {
                if (star != null && star.getStars() > 0) {
                    holder.tvExpertRating.setText("★ " + String.format(Locale.getDefault(), "%.1f", star.getStars()));
                    holder.tvExpertRating.setVisibility(View.VISIBLE);
                } else {
                    holder.tvExpertRating.setVisibility(View.GONE);
                }
            });
        }).start();

        // Setup replies RecyclerView
        CropChartReplyAdapter replyAdapter = new CropChartReplyAdapter(new ArrayList<>());
        holder.recyclerViewReplies.setAdapter(replyAdapter);
        holder.recyclerViewReplies.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(context));
        if (context instanceof LifecycleOwner) {
            LiveData<List<CropChartReply>> repliesLive = database.cropChartReplyDao()
                    .getRepliesByComment(comment.getId());
            repliesLive.observe((LifecycleOwner) context, replyAdapter::updateData);
        }

        // Handle reply input
        holder.btnSendReply.setOnClickListener(v -> {
            String replyText = holder.etReply.getText().toString().trim();
            if (replyText.isEmpty()) {
                holder.etReply.setError("Enter reply");
                return;
            }
            CropChartReply reply = new CropChartReply();
            reply.setCommentId(comment.getId());
            reply.setUserId(userId);
            reply.setUserName(userName);
            reply.setUserType(userType);
            reply.setReply(replyText);
            new Thread(() -> {
                database.cropChartReplyDao().insert(reply);
                ((android.app.Activity) context).runOnUiThread(() -> holder.etReply.setText(""));
            }).start();
        });
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public void updateData(List<CropChartComment> newComments) {
        this.comments = newComments;
        notifyDataSetChanged();
    }

    private String getTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            if (days == 1)
                return "1 day ago";
            if (days < 7)
                return days + " days ago";
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvExpertName, tvComment, tvTimestamp, tvExpertRating;
        RecyclerView recyclerViewReplies;
        EditText etReply;
        Button btnSendReply;

        ViewHolder(View itemView) {
            super(itemView);
            tvExpertName = itemView.findViewById(R.id.tvExpertName);
            tvExpertRating = itemView.findViewById(R.id.tvExpertRating);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            recyclerViewReplies = itemView.findViewById(R.id.recyclerViewReplies);
            etReply = itemView.findViewById(R.id.etReply);
            btnSendReply = itemView.findViewById(R.id.btnSendReply);
        }
    }
}
