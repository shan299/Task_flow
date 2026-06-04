package com.example.taskflow;

import org.json.JSONException;
import org.json.JSONObject;

public class Task {
    private String title;
    private String priority; // "High", "Medium", "Low"
    private boolean isCompleted;
    private long id; // timestamp as simple unique ID
    private String dueDate;

    public Task(String title, String priority, String dueDate) {
        this.title = title;
        this.priority = priority;
        this.dueDate = dueDate;
        this.isCompleted = false;
        this.id = System.currentTimeMillis();
    }

    public Task(String title, String priority, boolean isCompleted, long id, String dueDate) {
        this.title = title;
        this.priority = priority;
        this.isCompleted = isCompleted;
        this.id = id;
        this.dueDate = dueDate;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public long getId() { return id; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("title", title);
        jsonObject.put("priority", priority);
        jsonObject.put("isCompleted", isCompleted);
        jsonObject.put("id", id);
        jsonObject.put("dueDate", dueDate);
        return jsonObject;
    }

    public static Task fromJSONObject(JSONObject jsonObject) throws JSONException {
        return new Task(
                jsonObject.getString("title"),
                jsonObject.getString("priority"),
                jsonObject.getBoolean("isCompleted"),
                jsonObject.getLong("id"),
                jsonObject.optString("dueDate", "No date")
        );
    }
}
