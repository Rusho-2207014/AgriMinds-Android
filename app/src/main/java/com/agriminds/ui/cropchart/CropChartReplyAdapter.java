package com.agriminds.ui.cropchart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.R;
import com.agriminds.data.entity.CropChartReply;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CropChartReplyAdapter extends RecyclerView.Adapter<CropChartReplyAdapter.ViewHolder> {
    private List<CropChartReply> replies;

    public CropChartReplyAdapter(List<CropChartReply> replies) {
        this.replies = replies;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_crop_chart_reply, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CropChartReply reply = replies.get(position);
        holder.tvReplyUserName.setText(reply.getUserName());
        holder.tvReplyText.setText(reply.getReply());
        holder.tvReplyTimestamp.setText(getTimeAgo(reply.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return replies.size();
    }

    public void updateData(List<CropChartReply> newReplies) {
        this.replies = newReplies;
        notifyDataSetChanged();
    }

    private String getTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / 60000;
        if (minutes < 1)
            return "Just now";
        if (minutes < 60)
            return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24)
            return hours + " hr ago";
        long days = hours / 24;
        if (days < 7)
            return days + " day ago";
        return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(timestamp));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvReplyUserName, tvReplyText, tvReplyTimestamp;

        ViewHolder(View itemView) {
            super(itemView);
            tvReplyUserName = itemView.findViewById(R.id.tvReplyUserName);
            tvReplyText = itemView.findViewById(R.id.tvReplyText);
            tvReplyTimestamp = itemView.findViewById(R.id.tvReplyTimestamp);
        }
    }
}
