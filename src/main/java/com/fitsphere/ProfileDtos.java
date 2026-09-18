package com.fitsphere;

class ProfileUpdateRequest {
    public String name;
    public int age;
    public String gender;
    public double heightCm;
    public double weightKg;
    public String goal;
    public String activityLevel;
}

class ProfileNumbersResponse {
    public double bmi;
    public String bmiCategory;
    public double bmr;
    public double tdee;
    public double calorieTarget;
    public int proteinG;
    public int carbG;
    public int fatG;
}

class DashboardSummaryResponse {
    public String name;
    public double bmi;
    public String bmiCategory;
    public double calorieTarget;
    public int streak;
    public String goal;
}
