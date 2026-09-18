package com.fitsphere;

/**
 * Domain-science calculations: BMI, BMR (Mifflin-St Jeor), TDEE,
 * calorie targets, macro splits and MET-based calorie estimation.
 */
public class FitnessCalculator {

    public static double calculateBMI(double weightKg, double heightCm) {
        double heightM = heightCm / 100.0;
        return weightKg / (heightM * heightM);
    }

    public static String bmiCategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25) return "Normal";
        if (bmi < 30) return "Overweight";
        return "Obese";
    }

    /** Mifflin-St Jeor equation. */
    public static double calculateBMR(String gender, double weightKg, double heightCm, int age) {
        double base = 10 * weightKg + 6.25 * heightCm - 5 * age;
        if (gender != null && gender.equalsIgnoreCase("Male")) {
            return base + 5;
        }
        return base - 161;
    }

    public static double activityMultiplier(String activityLevel) {
        if (activityLevel == null) return 1.2;
        switch (activityLevel.toUpperCase()) {
            case "SEDENTARY":   return 1.2;
            case "LIGHT":       return 1.375;
            case "MODERATE":    return 1.55;
            case "ACTIVE":      return 1.725;
            case "VERY_ACTIVE": return 1.9;
            default:            return 1.2;
        }
    }

    public static double calculateTDEE(double bmr, String activityLevel) {
        return bmr * activityMultiplier(activityLevel);
    }

    public static double calculateCalorieTarget(double tdee, String goal) {
        if (goal == null) return tdee;
        switch (goal.toUpperCase()) {
            case "WEIGHT_LOSS":  return tdee - 500;
            case "MUSCLE_GAIN":  return tdee + 300;
            case "ENDURANCE":    return tdee + 150;
            default:             return tdee; // MAINTENANCE
        }
    }

    /** Returns [proteinGrams, carbGrams, fatGrams]. */
    public static int[] macroSplitGrams(double calorieTarget, String goal) {
        double proteinPct, carbPct, fatPct;
        String g = goal == null ? "MAINTENANCE" : goal.toUpperCase();
        switch (g) {
            case "WEIGHT_LOSS":
                proteinPct = 0.35; carbPct = 0.35; fatPct = 0.30; break;
            case "MUSCLE_GAIN":
                proteinPct = 0.30; carbPct = 0.45; fatPct = 0.25; break;
            case "ENDURANCE":
                proteinPct = 0.20; carbPct = 0.55; fatPct = 0.25; break;
            default:
                proteinPct = 0.25; carbPct = 0.45; fatPct = 0.30;
        }
        int proteinG = (int) Math.round((calorieTarget * proteinPct) / 4.0);
        int carbG = (int) Math.round((calorieTarget * carbPct) / 4.0);
        int fatG = (int) Math.round((calorieTarget * fatPct) / 9.0);
        return new int[] { proteinG, carbG, fatG };
    }

    /** Calories = MET x weight(kg) x duration(hours). */
    public static double estimateCaloriesBurned(double met, double weightKg, int durationMinutes) {
        return met * weightKg * (durationMinutes / 60.0);
    }
}
