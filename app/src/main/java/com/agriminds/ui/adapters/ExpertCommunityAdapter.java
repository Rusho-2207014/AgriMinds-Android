package com.agriminds.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.R;
import com.agriminds.data.entity.ExpertAnswer;
import com.agriminds.data.entity.Question;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExpertCommunityAdapter extends RecyclerView.Adapter<ExpertCommunityAdapter.ViewHolder> {

    public static class QuestionGroup {
        public Question question;
        public List<ExpertAnswer> answers;

        public QuestionGroup(Question question, List<ExpertAnswer> answers) {
            this.question = question;
            this.answers = answers;
        }
    }

    private List<QuestionGroup> questionGroups;
    private Context context;
    private int expertId;

    public ExpertCommunityAdapter(List<ExpertAnswer> answers, Context context, int expertId, String expertName) {
        this.context = context;
        this.expertId = expertId;
        this.questionGroups = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_question_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuestionGroup group = questionGroups.get(position);

        holder.tvQuestionText.setText(group.question.getQuestionText());

        // Show question info with time ago
        if (group.question.getFarmerName() != null) {
            String questionInfo = "Asked by " + group.question.getFarmerName() + " • "
                    + getTimeAgo(group.question.getCreatedAt());
            holder.tvQuestionInfo.setText(questionInfo);
            holder.tvQuestionInfo.setVisibility(View.VISIBLE);
        } else {
            holder.tvQuestionInfo.setVisibility(View.GONE);
        }

        // Setup nested answers adapter
        CommunityAnswersNestedAdapter answersAdapter = new CommunityAnswersNestedAdapter(
                group.answers, context, expertId);
        holder.recyclerViewAnswers.setAdapter(answersAdapter);
    }

    @Override
    public int getItemCount() {
        return questionGroups.size();
    }

    public void updateAnswers(List<ExpertAnswer> answers, Map<Integer, Question> questions) {
        // Group answers by question
        LinkedHashMap<Integer, List<ExpertAnswer>> grouped = new LinkedHashMap<>();

        for (ExpertAnswer answer : answers) {
            if (!grouped.containsKey(answer.getQuestionId())) {
                grouped.put(answer.getQuestionId(), new ArrayList<>());
            }
            grouped.get(answer.getQuestionId()).add(answer);
        }

        // Convert to QuestionGroup list and sort by latest answer time
        questionGroups.clear();
        for (Map.Entry<Integer, List<ExpertAnswer>> entry : grouped.entrySet()) {
            Question question = questions.get(entry.getKey());
            if (question != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                questionGroups.add(new QuestionGroup(question, entry.getValue()));
            }
        }

        // Sort question groups by the current expert's answer time (most recent first)
        questionGroups.sort((group1, group2) -> {
            // Find the current expert's answer in each group
            long time1 = 0;
            for (ExpertAnswer ans : group1.answers) {
                if (ans.getExpertId() == expertId) {
                    time1 = ans.getCreatedAt();
                    break;
                }
            }

            long time2 = 0;
            for (ExpertAnswer ans : group2.answers) {
                if (ans.getExpertId() == expertId) {
                    time2 = ans.getCreatedAt();
                    break;
                }
            }

            return Long.compare(time2, time1); // DESC order (newest first)
        });

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
        TextView tvQuestionText;
        TextView tvQuestionInfo;
        RecyclerView recyclerViewAnswers;

        ViewHolder(View itemView) {
            super(itemView);
            tvQuestionText = itemView.findViewById(R.id.tvQuestionText);
            tvQuestionInfo = itemView.findViewById(R.id.tvQuestionInfo);
            recyclerViewAnswers = itemView.findViewById(R.id.recyclerViewAnswers);
            recyclerViewAnswers.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        }
    }
}
