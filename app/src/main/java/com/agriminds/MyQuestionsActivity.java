package com.agriminds;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.adapter.MyQuestionsAdapter;
import com.agriminds.data.AppDatabase;
import com.agriminds.data.entity.Question;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MyQuestionsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MyQuestionsAdapter adapter;
    private View emptyState;
    private AppDatabase database;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_questions);

        database = AppDatabase.getInstance(this);
        currentUserId = getSharedPreferences("AgriMindsPrefs", MODE_PRIVATE)
                .getInt("userId", -1);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerViewQuestions);
        emptyState = findViewById(R.id.emptyState);
        FloatingActionButton fabAskQuestion = findViewById(R.id.fabAskQuestion);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyQuestionsAdapter(new ArrayList<>(), this, currentUserId);
        recyclerView.setAdapter(adapter);

        fabAskQuestion.setOnClickListener(v -> {
            // TODO: Open Ask Question Dialog/Activity
            Toast.makeText(this, "Ask Question feature coming soon", Toast.LENGTH_SHORT).show();
        });

        loadQuestions();
    }

    private void loadQuestions() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Question> questions = database.questionDao().getQuestionsByFarmer(currentUserId);
            runOnUiThread(() -> {
                if (questions.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyState.setVisibility(View.GONE);
                    adapter.updateQuestions(questions);
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadQuestions(); // Reload when returning to activity
    }
}
