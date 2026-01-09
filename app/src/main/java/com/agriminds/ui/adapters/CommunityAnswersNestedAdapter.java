package com.agriminds.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.R;
import com.agriminds.data.entity.ExpertAnswer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommunityAnswersNestedAdapter extends RecyclerView.Adapter<CommunityAnswersNestedAdapter.ViewHolder> {

    private List<ExpertAnswer> answers;
    private Context context;
    private int currentExpertId;
    private SimpleDateFormat dateFormat;

    public CommunityAnswersNestedAdapter(List<ExpertAnswer> answers, Context context, int currentExpertId) {
        this.answers = answers;
        this.context = context;
        this.currentExpertId = currentExpertId;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_answer_nested, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExpertAnswer answer = answers.get(position);

        holder.tvExpertName.setText(answer.getExpertName());
        holder.tvAnswerText.setText(answer.getAnswerText());
        holder.tvDate.setText(dateFormat.format(new Date(answer.getCreatedAt())));

        // Show "My Answer" badge if this is the current expert's answer
        if (answer.getExpertId() == currentExpertId) {
            holder.tvMyAnswerBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvMyAnswerBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return answers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvExpertName, tvAnswerText, tvDate, tvMyAnswerBadge;

        ViewHolder(View itemView) {
            super(itemView);
            tvExpertName = itemView.findViewById(R.id.tvExpertName);
            tvAnswerText = itemView.findViewById(R.id.tvAnswerText);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvMyAnswerBadge = itemView.findViewById(R.id.tvMyAnswerBadge);
        }
    }
}
