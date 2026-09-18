package com.fitsphere;

import java.time.LocalDate;

/**
 * A single logged activity/workout session for a user.
 */
public class ActivityLog {

    private int logId;
    private int userId;
    private LocalDate logDate;
    private String activityType;
    private int durationMinutes;
    private double caloriesBurned;
    private int steps;
    private int intensityRating;   // 1-5
    private boolean completed;

    public ActivityLog() {
    }

    public ActivityLog(int userId, LocalDate logDate, String activityType, int durationMinutes,
                        double caloriesBurned, int steps, int intensityRating, boolean completed) {
        this.userId = userId;
        this.logDate = logDate;
        this.activityType = activityType;
        this.durationMinutes = durationMinutes;
        this.caloriesBurned = caloriesBurned;
        this.steps = steps;
        this.intensityRating = intensityRating;
        this.completed = completed;
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public double getCaloriesBurned() { return caloriesBurned; }
    public void setCaloriesBurned(double caloriesBurned) { this.caloriesBurned = caloriesBurned; }

    public int getSteps() { return steps; }
    public void setSteps(int steps) { this.steps = steps; }

    public int getIntensityRating() { return intensityRating; }
    public void setIntensityRating(int intensityRating) { this.intensityRating = intensityRating; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
