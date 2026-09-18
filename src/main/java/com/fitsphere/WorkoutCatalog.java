package com.fitsphere;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Static catalog of workout types used by the recommendation engine.
 * Each entry carries a MET value (for calorie estimation), a category,
 * and the fitness goals it best supports. scoreForUser() implements a
 * simple content-based scoring function used by the recommender.
 */
public class WorkoutCatalog {

    public static class WorkoutType {
        public final String name;
        public final double met;
        public final String category; // CARDIO, STRENGTH, HIIT, FLEXIBILITY, RECOVERY
        public final Set<String> goalTags;
        public final int beginnerFriendly; // 1 (hard) - 5 (very beginner friendly)

        public WorkoutType(String name, double met, String category, int beginnerFriendly, String... goalTags) {
            this.name = name;
            this.met = met;
            this.category = category;
            this.beginnerFriendly = beginnerFriendly;
            this.goalTags = new HashSet<>(Arrays.asList(goalTags));
        }
    }

    public static final List<WorkoutType> ALL = new ArrayList<>();
    static {
        ALL.add(new WorkoutType("Brisk Walking", 4.3, "CARDIO", 5, "WEIGHT_LOSS", "MAINTENANCE"));
        ALL.add(new WorkoutType("Running", 9.8, "CARDIO", 2, "WEIGHT_LOSS", "ENDURANCE"));
        ALL.add(new WorkoutType("Cycling", 7.5, "CARDIO", 3, "WEIGHT_LOSS", "ENDURANCE"));
        ALL.add(new WorkoutType("Swimming", 8.3, "CARDIO", 3, "ENDURANCE", "WEIGHT_LOSS"));
        ALL.add(new WorkoutType("HIIT Circuit", 8.0, "HIIT", 1, "WEIGHT_LOSS"));
        ALL.add(new WorkoutType("Full Body Strength", 6.0, "STRENGTH", 3, "MUSCLE_GAIN", "MAINTENANCE"));
        ALL.add(new WorkoutType("Upper Body Strength", 5.0, "STRENGTH", 3, "MUSCLE_GAIN"));
        ALL.add(new WorkoutType("Lower Body Strength", 5.5, "STRENGTH", 3, "MUSCLE_GAIN"));
        ALL.add(new WorkoutType("Core & Mobility", 3.5, "FLEXIBILITY", 4, "MAINTENANCE", "ENDURANCE"));
        ALL.add(new WorkoutType("Yoga / Stretching", 3.0, "FLEXIBILITY", 5, "MAINTENANCE"));
        ALL.add(new WorkoutType("Rest / Active Recovery", 1.5, "RECOVERY", 5,
                "WEIGHT_LOSS", "MUSCLE_GAIN", "MAINTENANCE", "ENDURANCE"));
    }

    public static WorkoutType findByName(String name) {
        for (WorkoutType wt : ALL) {
            if (wt.name.equalsIgnoreCase(name)) return wt;
        }
        return null;
    }

    /**
     * Content-based scoring function: how good a fit is this workout type
     * for the given user's goal + fitness segment, penalized for recent
     * repetition so the weekly plan stays varied.
     */
    public static double scoreForUser(WorkoutType wt, String goal, String segment, List<String> recentTypes) {
        double score = 0;

        if (goal != null && wt.goalTags.contains(goal.toUpperCase())) {
            score += 3.0;
        }

        // Segment fit: beginners favour higher "beginnerFriendly" score,
        // advanced users get a small bonus for lower beginnerFriendly (harder) workouts.
        if ("Beginner".equals(segment)) {
            score += wt.beginnerFriendly * 0.4;
        } else if ("Advanced".equals(segment)) {
            score += (6 - wt.beginnerFriendly) * 0.4;
            if (wt.category.equals("HIIT") || wt.category.equals("STRENGTH")) score += 0.5;
        } else { // Intermediate
            score += 1.0;
        }

        // Repetition penalty - discourage the same workout on consecutive days.
        if (recentTypes != null) {
            for (String recent : recentTypes) {
                if (recent.equalsIgnoreCase(wt.name)) {
                    score -= 2.0;
                }
            }
        }

        return score;
    }
}
