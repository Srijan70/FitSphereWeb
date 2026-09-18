package com.fitsphere;

/**
 * One day's entry in a user's weekly adaptive workout plan.
 */
public class WorkoutPlanItem {

    private int itemId;
    private int userId;
    private String dayOfWeek;      // MON, TUE, WED, THU, FRI, SAT, SUN
    private String workoutType;
    private int durationMinutes;
    private int intensityLevel;    // 1-5

    public WorkoutPlanItem() {
    }

    public WorkoutPlanItem(int userId, String dayOfWeek, String workoutType,
                            int durationMinutes, int intensityLevel) {
        this.userId = userId;
        this.dayOfWeek = dayOfWeek;
        this.workoutType = workoutType;
        this.durationMinutes = durationMinutes;
        this.intensityLevel = intensityLevel;
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getWorkoutType() { return workoutType; }
    public void setWorkoutType(String workoutType) { this.workoutType = workoutType; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public int getIntensityLevel() { return intensityLevel; }
    public void setIntensityLevel(int intensityLevel) { this.intensityLevel = intensityLevel; }
}
