package com.agriminds.ui.home;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.R;
import com.agriminds.data.entity.ToDo;

import java.util.List;

public class ToDoAdapter extends RecyclerView.Adapter<ToDoAdapter.ToDoViewHolder> {

    private List<ToDo> todoList;
    private final OnItemActionListener listener;

    public interface OnItemActionListener {
        void onCheckChanged(ToDo todo, boolean isChecked);

        void onDeleteClick(ToDo todo);
    }

    public ToDoAdapter(List<ToDo> todoList, OnItemActionListener listener) {
        this.todoList = todoList;
        this.listener = listener;
    }

    public void updateList(List<ToDo> newList) {
        this.todoList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ToDoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_todo, parent, false);
        return new ToDoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ToDoViewHolder holder, int position) {
        ToDo todo = todoList.get(position);
        holder.bind(todo);
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    class ToDoViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbTodo;
        TextView tvTodoTask;
        ImageButton btnDelete;

        public ToDoViewHolder(@NonNull View itemView) {
            super(itemView);
            cbTodo = itemView.findViewById(R.id.cbTodo);
            tvTodoTask = itemView.findViewById(R.id.tvTodoTask);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(ToDo todo) {
            tvTodoTask.setText(todo.getTask());
            // Need to remove listener before setting state to avoid creating loops if we
            // had one
            cbTodo.setOnCheckedChangeListener(null);
            cbTodo.setChecked(todo.isCompleted());

            if (todo.isCompleted()) {
                tvTodoTask.setPaintFlags(tvTodoTask.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvTodoTask.setAlpha(0.6f);
            } else {
                tvTodoTask.setPaintFlags(tvTodoTask.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvTodoTask.setAlpha(1.0f);
            }

            cbTodo.setOnCheckedChangeListener((buttonView, isChecked) -> {
                listener.onCheckChanged(todo, isChecked);
            });

            btnDelete.setOnClickListener(v -> listener.onDeleteClick(todo));
        }
    }
}
