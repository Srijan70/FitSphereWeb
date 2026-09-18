package com.fitsphere;

/**
 * Represents a registered FitSphere user and their fitness profile.
 */
public class User {

    private int userId;
    private String name;
    private String email;
    private String password;
    private int age;
    private String gender;          // Male / Female / Other
    private double heightCm;
    private double weightKg;
    private String goal;            // WEIGHT_LOSS / MUSCLE_GAIN / MAINTENANCE / ENDURANCE
    private String activityLevel;   // SEDENTARY / LIGHT / MODERATE / ACTIVE / VERY_ACTIVE

    public User() {
    }

    public User(String name, String email, String password, int age, String gender,
                double heightCm, double weightKg, String goal, String activityLevel) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.age = age;
        this.gender = gender;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.goal = goal;
        this.activityLevel = activityLevel;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public double getHeightCm() { return heightCm; }
    public void setHeightCm(double heightCm) { this.heightCm = heightCm; }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
}
