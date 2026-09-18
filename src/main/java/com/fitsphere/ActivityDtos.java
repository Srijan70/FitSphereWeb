package com.fitsphere;

import java.util.List;

class ActivityLogRequest {
    public String activityType;
    public int durationMinutes;
    public int steps;
    public int intensityRating;
    public boolean completed;
}

class ActivityLogResponse {
    public int logId;
    public String logDate;
    public String activityType;
    public int durationMinutes;
    public double caloriesBurned;
    public int steps;
    public int intensityRating;
    public boolean completed;

    public ActivityLogResponse(ActivityLog log) {
        this.logId = log.getLogId();
        this.logDate = log.getLogDate().toString();
        this.activityType = log.getActivityType();
        this.durationMinutes = log.getDurationMinutes();
        this.caloriesBurned = log.getCaloriesBurned();
        this.steps = log.getSteps();
        this.intensityRating = log.getIntensityRating();
        this.completed = log.isCompleted();
    }
}

class ChartSeriesResponse {
    public List<String> labels;
    public List<Double> values;

    public ChartSeriesResponse(List<String> labels, List<Double> values) {
        this.labels = labels;
        this.values = values;
    }
}

class TypeDistributionResponse {
    public List<String> labels;
    public List<Integer> values;

    public TypeDistributionResponse(List<String> labels, List<Integer> values) {
        this.labels = labels;
        this.values = values;
    }
}

class WorkoutTypeOption {
    public String name;
    public double met;
    public String category;

    public WorkoutTypeOption(WorkoutCatalog.WorkoutType wt) {
        this.name = wt.name;
        this.met = wt.met;
        this.category = wt.category;
    }
}

class CalorieEstimateResponse {
    public double estimatedCalories;

    public CalorieEstimateResponse(double v) {
        this.estimatedCalories = v;
    }
}
