package com.example.taskflow;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskListener {
        void onTaskClick(int position);
        void onTaskStatusChange(int position, boolean isCompleted);
        void onTaskLongClick(int position);
        void onTaskDelete(int position);
    }

    private List<Task> displayList;
    private List<Task> fullList;
    private OnTaskListener listener;

    public TaskAdapter(List<Task> fullList, List<Task> displayList, OnTaskListener listener) {
        this.fullList = fullList;
        this.displayList = displayList;
        this.listener = listener;
    }

    public void updateDisplayList(List<Task> newDisplayList, List<Task> newFullList) {
        this.displayList = newDisplayList;
        this.fullList = newFullList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.taskitem, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = displayList.get(position);
        holder.tvTitle.setText(task.getTitle());
        holder.tvPriority.setText(task.getPriority());
        holder.tvDueDate.setText("Due: " + task.getDueDate());
        holder.checkBox.setChecked(task.isCompleted());

        int priorityColor;
        switch (task.getPriority()) {
            case "High":
                priorityColor = holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark);
                break;
            case "Medium":
                priorityColor = holder.itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark);
                break;
            case "Low":
                priorityColor = holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark);
                break;
            default:
                priorityColor = holder.itemView.getContext().getResources().getColor(android.R.color.darker_gray);
                break;
        }
        holder.priorityIndicator.setBackgroundColor(priorityColor);

        if (task.isCompleted()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                int originalPos = fullList.indexOf(task);
                if (originalPos != -1) {
                    listener.onTaskStatusChange(originalPos, isChecked);
                }
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int originalPos = fullList.indexOf(task);
            if (originalPos != -1) {
                listener.onTaskDelete(originalPos);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            int originalPos = fullList.indexOf(task);
            if (originalPos != -1) {
                listener.onTaskClick(originalPos);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            int originalPos = fullList.indexOf(task);
            if (originalPos != -1) {
                listener.onTaskLongClick(originalPos);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public Task getTaskAt(int position) {
        return displayList.get(position);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPriority, tvDueDate;
        CheckBox checkBox;
        View priorityIndicator;
        ImageButton btnDelete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.taskTitle);
            tvPriority = itemView.findViewById(R.id.taskPriority);
            tvDueDate = itemView.findViewById(R.id.taskDueDate);
            checkBox = itemView.findViewById(R.id.taskCheckbox);
            priorityIndicator = itemView.findViewById(R.id.priorityIndicator);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
