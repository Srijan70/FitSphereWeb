package com.fitsphere;

import java.util.List;

class WorkoutPlanItemResponse {
    public String dayOfWeek;
    public String workoutType;
    public int durationMinutes;
    public int intensityLevel;

    public WorkoutPlanItemResponse(WorkoutPlanItem item) {
        this.dayOfWeek = item.getDayOfWeek();
        this.workoutType = item.getWorkoutType();
        this.durationMinutes = item.getDurationMinutes();
        this.intensityLevel = item.getIntensityLevel();
    }
}

class RecommendationResponse {
    public String segment;
    public double adherenceRate;
    public double avgRating;
    public String direction;
    public String explanation;
    public List<WorkoutPlanItemResponse> plan;
    public boolean hasPlan;
}
