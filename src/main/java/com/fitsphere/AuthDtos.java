package com.fitsphere;

class RegisterRequest {
    public String name;
    public String email;
    public String password;
    public int age;
    public String gender;
    public double heightCm;
    public double weightKg;
    public String goal;
    public String activityLevel;
}

class LoginRequest {
    public String email;
    public String password;
}

class UserResponse {
    public int userId;
    public String name;
    public String email;
    public int age;
    public String gender;
    public double heightCm;
    public double weightKg;
    public String goal;
    public String activityLevel;

    public UserResponse(User u) {
        this.userId = u.getUserId();
        this.name = u.getName();
        this.email = u.getEmail();
        this.age = u.getAge();
        this.gender = u.getGender();
        this.heightCm = u.getHeightCm();
        this.weightKg = u.getWeightKg();
        this.goal = u.getGoal();
        this.activityLevel = u.getActivityLevel();
    }
}
