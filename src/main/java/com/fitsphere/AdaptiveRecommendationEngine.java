package com.fitsphere;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates a personalized adaptive weekly workout plan.
 *
 * Goal determines the weekly training split:
 * MUSCLE_GAIN  -> 5-day Push/Pull/Legs based split
 * WEIGHT_LOSS  -> 4-day Upper/Lower split
 * MAINTENANCE  -> 3-day Full Body split
 * ENDURANCE    -> 4-day Upper/Lower split
 *
 * Fitness segment, recent adherence and intensity rating are still used
 * to adjust workout intensity and duration.
 */
public class AdaptiveRecommendationEngine {

    private static final String[] DAYS = {
            "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"
    };

    public static class RecommendationResult {
        public String segment;
        public double adherenceRate;
        public double avgRating;
        public String direction;
        public String explanation;
        public List<WorkoutPlanItem> weeklyPlan;
    }

    public static RecommendationResult generate(
            User user,
            List<ActivityLog> recentLogs,
            String segment) {

        RecommendationResult result = new RecommendationResult();

        result.segment = segment;

        // Analyse recent activity
        double adherence = computeAdherence(recentLogs);
        double avgRating = computeAvgRating(recentLogs);

        String direction = decideAdjustment(adherence, avgRating);

        result.adherenceRate = adherence;
        result.avgRating = avgRating;
        result.direction = direction;

        // Base intensity and duration depend on fitness segment
        int baseIntensity = segmentBaseIntensity(segment);
        int baseDuration = segmentBaseDuration(segment);

        // Adapt based on recent performance
        int intensity = applyIntensityAdjustment(baseIntensity, direction);
        int duration = applyDurationAdjustment(baseDuration, direction);

        // Build goal-specific weekly plan
        result.weeklyPlan = buildWeeklyPlan(
                user,
                intensity,
                duration
        );

        result.explanation = buildExplanation(
                segment,
                adherence,
                avgRating,
                direction
        );

        return result;
    }

    // ---------------------------------------------------------
    // ADHERENCE
    // ---------------------------------------------------------

    private static double computeAdherence(List<ActivityLog> logs) {

        if (logs == null || logs.isEmpty()) {
            return 0.5;
        }

        long completed = logs.stream()
                .filter(ActivityLog::isCompleted)
                .count();

        return (double) completed / logs.size();
    }

    // ---------------------------------------------------------
    // AVERAGE RATING
    // ---------------------------------------------------------

    private static double computeAvgRating(List<ActivityLog> logs) {

        if (logs == null || logs.isEmpty()) {
            return 3.0;
        }

        double sum = 0;

        for (ActivityLog log : logs) {
            sum += log.getIntensityRating();
        }

        return sum / logs.size();
    }

    // ---------------------------------------------------------
    // ADAPTATION DECISION
    // ---------------------------------------------------------

    private static String decideAdjustment(
            double adherence,
            double avgRating) {

        if (adherence >= 0.8 && avgRating >= 4.0) {
            return "increase";
        }

        if (adherence < 0.5 || avgRating <= 2.0) {
            return "decrease";
        }

        return "maintain";
    }

    // ---------------------------------------------------------
    // BASE INTENSITY
    // ---------------------------------------------------------

    private static int segmentBaseIntensity(String segment) {

        if ("Advanced".equals(segment)) {
            return 4;
        }

        if ("Intermediate".equals(segment)) {
            return 3;
        }

        return 2;
    }

    // ---------------------------------------------------------
    // BASE DURATION
    // ---------------------------------------------------------

    private static int segmentBaseDuration(String segment) {

        if ("Advanced".equals(segment)) {
            return 50;
        }

        if ("Intermediate".equals(segment)) {
            return 40;
        }

        return 30;
    }

    // ---------------------------------------------------------
    // INTENSITY ADJUSTMENT
    // ---------------------------------------------------------

    private static int applyIntensityAdjustment(
            int base,
            String direction) {

        if ("increase".equals(direction)) {
            return Math.min(base + 1, 5);
        }

        if ("decrease".equals(direction)) {
            return Math.max(base - 1, 1);
        }

        return base;
    }

    // ---------------------------------------------------------
    // DURATION ADJUSTMENT
    // ---------------------------------------------------------

    private static int applyDurationAdjustment(
            int base,
            String direction) {

        if ("increase".equals(direction)) {
            return Math.min(base + 10, 90);
        }

        if ("decrease".equals(direction)) {
            return Math.max(base - 10, 15);
        }

        return base;
    }

    // ---------------------------------------------------------
    // BUILD WEEKLY PLAN
    // ---------------------------------------------------------

    private static List<WorkoutPlanItem> buildWeeklyPlan(
            User user,
            int intensity,
            int duration) {

        List<WorkoutPlanItem> plan = new ArrayList<>();

        String goal = user.getGoal();

        /*
         * MUSCLE GAIN
         * 5-day PPL based split
         */
        if ("MUSCLE_GAIN".equalsIgnoreCase(goal)) {

            add(plan, user, "MON",
                    "Push", duration, intensity);

            add(plan, user, "TUE",
                    "Pull + Abs", duration, intensity);

            add(plan, user, "WED",
                    "Legs", duration, intensity);

            add(plan, user, "THU",
                    "Active Rest", 15, Math.max(1, intensity - 1));

            add(plan, user, "FRI",
                    "Chest + Shoulders + Triceps", duration, intensity);

            add(plan, user, "SAT",
                    "Back + Biceps + Abs", duration, intensity);

            add(plan, user, "SUN",
                    "Rest", 15, 1);

            return plan;
        }

        /*
         * WEIGHT LOSS / FAT LOSS
         * 4-day Upper / Lower split
         */
        if ("WEIGHT_LOSS".equalsIgnoreCase(goal)) {

            add(plan, user, "MON",
                    "Upper A", duration, intensity);

            add(plan, user, "TUE",
                    "Lower A", duration, intensity);

            add(plan, user, "WED",
                    "Active Rest", 15, Math.max(1, intensity - 1));

            add(plan, user, "THU",
                    "Upper B", duration, intensity);

            add(plan, user, "FRI",
                    "Active Rest", 15, Math.max(1, intensity - 1));

            add(plan, user, "SAT",
                    "Lower B", duration, intensity);

            add(plan, user, "SUN",
                    "Rest", 15, 1);

            return plan;
        }

        /*
         * ENDURANCE
         * 4-day Upper / Lower split
         */
        if ("ENDURANCE".equalsIgnoreCase(goal)) {

            add(plan, user, "MON",
                    "Upper A", duration, intensity);

            add(plan, user, "TUE",
                    "Lower A", duration, intensity);

            add(plan, user, "WED",
                    "Active Rest", 15, Math.max(1, intensity - 1));

            add(plan, user, "THU",
                    "Upper B", duration, intensity);

            add(plan, user, "FRI",
                    "Active Rest", 15, Math.max(1, intensity - 1));

            add(plan, user, "SAT",
                    "Lower B", duration, intensity);

            add(plan, user, "SUN",
                    "Rest", 15, 1);

            return plan;
        }

        /*
         * MAINTENANCE / GENERAL FITNESS
         * 3-day Full Body split
         */
        add(plan, user, "MON",
                "Full Body A", duration, intensity);

        add(plan, user, "TUE",
                "Active Rest", 15, Math.max(1, intensity - 1));

        add(plan, user, "WED",
                "Full Body B", duration, intensity);

        add(plan, user, "THU",
                "Active Rest", 15, Math.max(1, intensity - 1));

        add(plan, user, "FRI",
                "Full Body C", duration, intensity);

        add(plan, user, "SAT",
                "Active Rest", 15, Math.max(1, intensity - 1));

        add(plan, user, "SUN",
                "Rest", 15, 1);

        return plan;
    }

    // ---------------------------------------------------------
    // ADD PLAN ITEM
    // ---------------------------------------------------------

    private static void add(
            List<WorkoutPlanItem> plan,
            User user,
            String day,
            String workoutType,
            int duration,
            int intensity) {

        plan.add(
                new WorkoutPlanItem(
                        user.getUserId(),
                        day,
                        workoutType,
                        duration,
                        intensity
                )
        );
    }

    // ---------------------------------------------------------
    // EXPLANATION
    // ---------------------------------------------------------

    private static String buildExplanation(
            String segment,
            double adherence,
            double avgRating,
            String direction) {

        StringBuilder sb = new StringBuilder();

        sb.append(String.format(
                "Segment: %s | Adherence: %.0f%% | Avg. rating: %.1f/5\n\n",
                segment,
                adherence * 100,
                avgRating
        ));

        switch (direction) {

            case "increase":

                sb.append(
                        "Great consistency! Since you completed most sessions "
                                + "with strong ratings, this week's intensity and "
                                + "duration have been increased slightly."
                );

                break;

            case "decrease":

                sb.append(
                        "Recent sessions were difficult to complete or had lower "
                                + "ratings, so this week's intensity and duration "
                                + "have been reduced to help you recover and stay consistent."
                );

                break;

            default:

                sb.append(
                        "You're maintaining a steady pace. This week's plan "
                                + "keeps the current intensity and duration."
                );

                break;
        }

        return sb.toString();
    }
}