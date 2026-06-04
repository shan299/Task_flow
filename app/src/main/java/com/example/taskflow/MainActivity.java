package com.example.taskflow;

import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskListener {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> taskList;
    private List<Task> displayList;
    private TextView taskSummary, tvProgressPercent;
    private ProgressBar progressBar;
    private CalendarView calendarView;
    private static final String PREFS_NAME = "TaskPrefs";
    private static final String TASKS_KEY = "tasks";
    private static final String CHANNEL_ID = "task_reminders";
    private String selectedDateForNewTask = "No date";
    private String calendarFilterDate = null;
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        taskList = loadTasks();
        displayList = new ArrayList<>(taskList);
        sortTasks();

        taskSummary = findViewById(R.id.taskSummary);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        progressBar = findViewById(R.id.progressBar);
        calendarView = findViewById(R.id.calendarView);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(taskList, displayList, this);
        recyclerView.setAdapter(adapter);

        // Calendar Date Change
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            calendarFilterDate = dayOfMonth + "/" + (month + 1) + "/" + year;
            applyFilters();
            Toast.makeText(this, "Showing tasks for " + calendarFilterDate, Toast.LENGTH_SHORT).show();
        });

        // Swipe to Delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Task task = adapter.getTaskAt(position);
                int originalPos = taskList.indexOf(task);
                if (originalPos != -1) {
                    taskList.remove(originalPos);
                    saveTasks();
                    applyFilters();
                    updateSummary();
                    Toast.makeText(MainActivity.this, "Task Deleted", Toast.LENGTH_SHORT).show();
                }
            }
        }).attachToRecyclerView(recyclerView);

        // Search
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                applyFilters();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                applyFilters();
                return true;
            }
        });

        // Theme Toggle
        ImageButton btnThemeToggle = findViewById(R.id.btnThemeToggle);
        btnThemeToggle.setOnClickListener(v -> {
            int currentMode = AppCompatDelegate.getDefaultNightMode();
            if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        });

        // Reset Filter (click on summary)
        taskSummary.setOnClickListener(v -> {
            calendarFilterDate = null;
            applyFilters();
            Toast.makeText(this, "Showing all tasks", Toast.LENGTH_SHORT).show();
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showTaskDialog(null, -1));

        updateSummary();
        applyFilters();
    }

    private void applyFilters() {
        displayList.clear();
        for (Task task : taskList) {
            boolean matchesQuery = task.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase());
            boolean matchesDate = (calendarFilterDate == null) || task.getDueDate().equals(calendarFilterDate);
            if (matchesQuery && matchesDate) {
                displayList.add(task);
            }
        }
        adapter.updateDisplayList(displayList, taskList);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Task Reminders";
            String description = "Channel for task reminders";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void showTaskDialog(Task taskToEdit, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);

        EditText etTitle = view.findViewById(R.id.etTaskTitle);
        Spinner spinnerPriority = view.findViewById(R.id.spinnerPriority);
        Button btnPickDate = view.findViewById(R.id.btnPickDate);

        String[] priorities = {"High", "Medium", "Low"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(priorityAdapter);

        if (taskToEdit != null) {
            builder.setTitle("Edit Task");
            etTitle.setText(taskToEdit.getTitle());
            selectedDateForNewTask = taskToEdit.getDueDate();
            btnPickDate.setText(selectedDateForNewTask);
            for (int i = 0; i < priorities.length; i++) {
                if (priorities[i].equals(taskToEdit.getPriority())) {
                    spinnerPriority.setSelection(i);
                    break;
                }
            }
        } else {
            builder.setTitle("Add New Task");
            selectedDateForNewTask = calendarFilterDate != null ? calendarFilterDate : "No date";
            btnPickDate.setText(selectedDateForNewTask.equals("No date") ? "Select Date" : selectedDateForNewTask);
        }

        btnPickDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (datePicker, year, month, day) -> {
                selectedDateForNewTask = day + "/" + (month + 1) + "/" + year;
                btnPickDate.setText(selectedDateForNewTask);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        builder.setView(view);
        builder.setPositiveButton(taskToEdit == null ? "Add" : "Update", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String priority = spinnerPriority.getSelectedItem().toString();

            if (!title.isEmpty()) {
                if (taskToEdit == null) {
                    taskList.add(new Task(title, priority, selectedDateForNewTask));
                    sendNotification("Task Added", "You have a new task: " + title);
                } else {
                    taskToEdit.setTitle(title);
                    taskToEdit.setPriority(priority);
                    taskToEdit.setDueDate(selectedDateForNewTask);
                }
                saveTasks();
                sortTasks();
                applyFilters();
                updateSummary();
            } else {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateSummary() {
        int total = taskList.size();
        int remaining = 0;
        int completed = 0;
        for (Task t : taskList) {
            if (!t.isCompleted()) {
                remaining++;
            } else {
                completed++;
            }
        }
        taskSummary.setText(remaining + " tasks remaining (Tap to show all)");

        if (total > 0) {
            int percent = (completed * 100) / total;
            progressBar.setProgress(percent);
            tvProgressPercent.setText(percent + "%");
        } else {
            progressBar.setProgress(0);
            tvProgressPercent.setText("0%");
        }
    }

    private void sortTasks() {
        Collections.sort(taskList, (t1, t2) -> {
            if (t1.isCompleted() != t2.isCompleted()) {
                return t1.isCompleted() ? 1 : -1;
            }
            return Long.compare(t2.getId(), t1.getId());
        });
    }

    private void saveTasks() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray jsonArray = new JSONArray();
        try {
            for (Task task : taskList) {
                jsonArray.put(task.toJSONObject());
            }
            editor.putString(TASKS_KEY, jsonArray.toString());
            editor.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private List<Task> loadTasks() {
        List<Task> list = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(TASKS_KEY, null);
        if (json != null) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                for (int i = 0; i < jsonArray.length(); i++) {
                    list.add(Task.fromJSONObject(jsonArray.getJSONObject(i)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    @Override
    public void onTaskClick(int position) {
        Task task = displayList.get(position);
        String dateStr = task.getDueDate();
        if (!dateStr.equals("No date")) {
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
            try {
                Date date = sdf.parse(dateStr);
                if (date != null) {
                    calendarView.setDate(date.getTime(), true, true);
                    Toast.makeText(this, "Task date: " + dateStr, Toast.LENGTH_SHORT).show();
                    calendarFilterDate = dateStr;
                    applyFilters();
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "No due date set for this task", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onTaskStatusChange(int position, boolean isCompleted) {
        Task task = taskList.get(position);
        task.setCompleted(isCompleted);
        saveTasks();
        sortTasks();
        applyFilters();
        updateSummary();
    }

    @Override
    public void onTaskLongClick(int position) {
        showTaskDialog(taskList.get(position), position);
    }

    @Override
    public void onTaskDelete(int position) {
        taskList.remove(position);
        saveTasks();
        applyFilters();
        updateSummary();
        Toast.makeText(this, "Task Deleted", Toast.LENGTH_SHORT).show();
    }
}