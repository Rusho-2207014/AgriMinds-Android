package com.agriminds.ui.myfarm;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.agriminds.R;

public class MyFarmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_farm);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Farm");
        }

        setupToDoList();
    }

    private void setupToDoList() {
        android.widget.EditText etNewTask = findViewById(R.id.etNewTask);
        android.widget.Button btnAddTask = findViewById(R.id.btnAddTask);
        androidx.recyclerview.widget.RecyclerView rvToDoList = findViewById(R.id.rvToDoList);
        android.widget.TextView tvEmptyTodo = findViewById(R.id.tvEmptyTodo);

        rvToDoList.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        com.agriminds.data.AppDatabase database = com.agriminds.data.AppDatabase.getInstance(this);
        android.content.SharedPreferences prefs = getSharedPreferences("AgriMindsPrefs", MODE_PRIVATE);
        String userId = String.valueOf(prefs.getInt("userId", -1));

        com.agriminds.ui.home.ToDoAdapter adapter = new com.agriminds.ui.home.ToDoAdapter(new java.util.ArrayList<>(),
                new com.agriminds.ui.home.ToDoAdapter.OnItemActionListener() {
                    @Override
                    public void onCheckChanged(com.agriminds.data.entity.ToDo todo, boolean isChecked) {
                        todo.setCompleted(isChecked);
                        new Thread(() -> database.toDoDao().update(todo)).start();
                    }

                    @Override
                    public void onDeleteClick(com.agriminds.data.entity.ToDo todo) {
                        new Thread(() -> database.toDoDao().delete(todo)).start();
                    }
                });
        rvToDoList.setAdapter(adapter);

        // Load tasks
        database.toDoDao().getToDosByUser(userId).observe(this, todos -> {
            if (todos != null && !todos.isEmpty()) {
                adapter.updateList(todos);
                tvEmptyTodo.setVisibility(android.view.View.GONE);
                rvToDoList.setVisibility(android.view.View.VISIBLE);
            } else {
                adapter.updateList(new java.util.ArrayList<>()); // Clear list
                tvEmptyTodo.setVisibility(android.view.View.VISIBLE);
                rvToDoList.setVisibility(android.view.View.GONE);
            }
        });

        btnAddTask.setOnClickListener(v -> {
            String taskText = etNewTask.getText().toString().trim();
            if (!taskText.isEmpty()) {
                com.agriminds.data.entity.ToDo newToDo = new com.agriminds.data.entity.ToDo(userId, taskText,
                        System.currentTimeMillis());
                new Thread(() -> {
                    database.toDoDao().insert(newToDo);
                    runOnUiThread(() -> etNewTask.setText(""));
                }).start();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
