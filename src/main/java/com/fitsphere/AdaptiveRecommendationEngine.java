package com.fitsphere;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Generates a personalized, adaptive weekly workout plan.
 *
 * Adaptivity comes from a simple feedback-control loop: recent adherence
 * (completion rate) and average self-rated intensity feedback push the
 * plan's difficulty up, down, or hold it steady - similar in spirit to a
 * reinforcement-learning reward signal driving policy adjustment.
 *
 * Day-by-day workout selection uses WorkoutCatalog's content-based scoring
 * with an epsilon-greedy exploration step, so the plan stays varied instead
 * of always picking the single top-scoring workout.
 */
public class AdaptiveRecommendationEngine {

    private static final String[] DAYS = { "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN" };
    private static final double EPSILON = 0.2; // 20% chance to explore instead of exploit
    private static final int RECENT_WINDOW = 2; // days to look back for repetition penalty

    public static class RecommendationResult {
        public String segment;
        public double adherenceRate;
        public double avgRating;
        public String direction;       // "increase", "maintain", "decrease"
        public String explanation;
        public List<WorkoutPlanItem> weeklyPlan;
    }

    public static RecommendationResult generate(User user, List<ActivityLog> recentLogs, String segment) {

        RecommendationResult result = new RecommendationResult();
        result.segment = segment;

        double adherence = computeAdherence(recentLogs);
        double avgRating = computeAvgRating(recentLogs);
        String direction = decideAdjustment(adherence, avgRating);

        result.adherenceRate = adherence;
        result.avgRating = avgRating;
        result.direction = direction;

        int baseIntensity = segmentBaseIntensity(segment);
        int baseDuration = segmentBaseDuration(segment);

        int intensity = applyIntensityAdjustment(baseIntensity, direction);
        int duration = applyDurationAdjustment(baseDuration, direction);

        result.weeklyPlan = buildWeeklyPlan(user, segment, intensity, duration);
        result.explanation = buildExplanation(segment, adherence, avgRating, direction);

        return result;
    }

    private static double computeAdherence(List<ActivityLog> logs) {
        if (logs == null || logs.isEmpty()) return 0.5; // neutral default for brand-new users
        long completed = logs.stream().filter(ActivityLog::isCompleted).count();
        return (double) completed / logs.size();
    }

    private static double computeAvgRating(List<ActivityLog> logs) {
        if (logs == null || logs.isEmpty()) return 3.0;
        double sum = 0;
        for (ActivityLog log : logs) sum += log.getIntensityRating();
        return sum / logs.size();
    }

    private static String decideAdjustment(double adherence, double avgRating) {
        if (adherence >= 0.8 && avgRating >= 4.0) return "increase";
        if (adherence < 0.5 || avgRating <= 2.0) return "decrease";
        return "maintain";
    }

    private static int segmentBaseIntensity(String segment) {
        if ("Advanced".equals(segment)) return 4;
        if ("Intermediate".equals(segment)) return 3;
        return 2; // Beginner
    }

    private static int segmentBaseDuration(String segment) {
        if ("Advanced".equals(segment)) return 50;
        if ("Intermediate".equals(segment)) return 40;
        return 30; // Beginner
    }

    private static int applyIntensityAdjustment(int base, String direction) {
        if ("increase".equals(direction)) return Math.min(base + 1, 5);
        if ("decrease".equals(direction)) return Math.max(base - 1, 1);
        return base;
    }

    private static int applyDurationAdjustment(int base, String direction) {
        if ("increase".equals(direction)) return Math.min(base + 10, 90);
        if ("decrease".equals(direction)) return Math.max(base - 10, 15);
        return base;
    }

    private static List<WorkoutPlanItem> buildWeeklyPlan(User user, String segment, int intensity, int duration) {

        List<WorkoutPlanItem> plan = new ArrayList<>();
        LinkedList<String> recentTypes = new LinkedList<>();
        Random rand = new Random();

        int restDaySpacing = "Beginner".equals(segment) ? 3 : ("Intermediate".equals(segment) ? 4 : 6);

        for (int i = 0; i < DAYS.length; i++) {
            String day = DAYS[i];

            boolean isRestDay = (i + 1) % restDaySpacing == 0;

            WorkoutCatalog.WorkoutType chosen;

            if (isRestDay) {
                chosen = WorkoutCatalog.findByName("Rest / Active Recovery");
            } else {
                chosen = pickWorkout(user.getGoal(), segment, recentTypes, rand);
            }

            int itemDuration = chosen.category.equals("RECOVERY") ? Math.max(15, duration - 15) : duration;

            plan.add(new WorkoutPlanItem(user.getUserId(), day, chosen.name, itemDuration, intensity));

            recentTypes.addLast(chosen.name);
            while (recentTypes.size() > RECENT_WINDOW) recentTypes.removeFirst();
        }

        return plan;
    }

    /** Epsilon-greedy selection over content-based scores for variety. */
    private static WorkoutCatalog.WorkoutType pickWorkout(String goal, String segment,
                                                            List<String> recentTypes, Random rand) {

        List<WorkoutCatalog.WorkoutType> candidates = new ArrayList<>();
        for (WorkoutCatalog.WorkoutType wt : WorkoutCatalog.ALL) {
            if (!wt.category.equals("RECOVERY")) candidates.add(wt);
        }

        candidates.sort((a, b) -> Double.compare(
                WorkoutCatalog.scoreForUser(b, goal, segment, recentTypes),
                WorkoutCatalog.scoreForUser(a, goal, segment, recentTypes)));

        if (rand.nextDouble() < EPSILON && candidates.size() > 1) {
            // Explore: pick randomly among the top 3 rather than always the single best.
            int topN = Math.min(3, candidates.size());
            return candidates.get(rand.nextInt(topN));
        }

        return candidates.get(0); // Exploit: best-scoring workout
    }

    private static String buildExplanation(String segment, double adherence, double avgRating, String direction) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Segment: %s | Adherence: %.0f%% | Avg. rating: %.1f/5\n\n", segment, adherence * 100, avgRating));

        switch (direction) {
            case "increase":
                sb.append("Great consistency! Since you completed most sessions with strong ratings, ")
                  .append("this week's intensity and duration have been nudged up to keep challenging you.");
                break;
            case "decrease":
                sb.append("It looks like recent sessions were tough to complete or rated low, so this week's ")
                  .append("plan has been eased up in intensity and duration to help you rebuild momentum.");
                break;
            default:
                sb.append("You're holding a steady pace. This week's plan keeps the same intensity and duration ")
                  .append("as before while introducing some variety.");
        }
        return sb.toString();
    }
}
