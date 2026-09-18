package com.fitsphere;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Groups users into fitness segments (Beginner / Intermediate / Advanced)
 * using K-Means clustering over normalized activity-history features:
 *
 *   [ adherenceRate, avgDurationMinutes, avgCaloriesPerSession, avgIntensityRating, sessionsPerWeek ]
 *
 * Clusters are ranked by a composite "fitness score" of their centroid so
 * that cluster 0/1/2 always map consistently to Beginner/Intermediate/Advanced.
 *
 * New users without enough logged history fall back to a simple rule-based
 * classification based on their stated activity level (cold-start handling).
 */
public class FitnessSegmentation {

    public static final int MIN_USERS_FOR_CLUSTERING = 3;
    public static final String[] SEGMENT_LABELS = { "Beginner", "Intermediate", "Advanced" };

    /**
     * @param featuresByUser map of userId -> raw feature vector (see class comment for order)
     * @return map of userId -> segment label
     */
    public static Map<Integer, String> segmentAllUsers(Map<Integer, double[]> featuresByUser) {

        Map<Integer, String> result = new HashMap<>();

        if (featuresByUser.size() < MIN_USERS_FOR_CLUSTERING) {
            // Not enough population data to cluster meaningfully yet.
            return result;
        }

        List<Integer> userIds = new ArrayList<>(featuresByUser.keySet());
        int n = userIds.size();
        int dim = featuresByUser.get(userIds.get(0)).length;

        double[][] raw = new double[n][dim];
        for (int i = 0; i < n; i++) {
            raw[i] = featuresByUser.get(userIds.get(i));
        }

        double[][] normalized = minMaxNormalize(raw);

        int k = Math.min(3, n);
        KMeans kmeans = new KMeans(k, 100);
        int[] labels = kmeans.fit(normalized);
        double[][] centroids = kmeans.getCentroids();

        // Rank clusters by composite fitness score (higher = fitter/more advanced)
        Integer[] clusterOrder = rankClustersByFitness(centroids, k);

        // Map rank -> label (fill from lowest rank -> Beginner ... highest -> Advanced)
        Map<Integer, String> clusterToLabel = new HashMap<>();
        for (int rank = 0; rank < clusterOrder.length; rank++) {
            String label = SEGMENT_LABELS[Math.min(rank, SEGMENT_LABELS.length - 1)];
            clusterToLabel.put(clusterOrder[rank], label);
        }

        for (int i = 0; i < n; i++) {
            result.put(userIds.get(i), clusterToLabel.get(labels[i]));
        }

        return result;
    }

    /** Rule-based fallback for users with too little logged history. */
    public static String classifyColdStart(User user) {
        if (user.getActivityLevel() == null) return "Beginner";
        switch (user.getActivityLevel().toUpperCase()) {
            case "SEDENTARY":
            case "LIGHT":
                return "Beginner";
            case "MODERATE":
                return "Intermediate";
            case "ACTIVE":
            case "VERY_ACTIVE":
                return "Advanced";
            default:
                return "Beginner";
        }
    }

    private static double[][] minMaxNormalize(double[][] data) {
        int n = data.length;
        int dim = data[0].length;
        double[] min = new double[dim];
        double[] max = new double[dim];
        java.util.Arrays.fill(min, Double.MAX_VALUE);
        java.util.Arrays.fill(max, -Double.MAX_VALUE);

        for (double[] row : data) {
            for (int d = 0; d < dim; d++) {
                if (row[d] < min[d]) min[d] = row[d];
                if (row[d] > max[d]) max[d] = row[d];
            }
        }

        double[][] normalized = new double[n][dim];
        for (int i = 0; i < n; i++) {
            for (int d = 0; d < dim; d++) {
                double range = max[d] - min[d];
                normalized[i][d] = range == 0 ? 0.5 : (data[i][d] - min[d]) / range;
            }
        }
        return normalized;
    }

    /** Returns cluster indices ordered from "least fit" to "most fit" based on centroid weighting. */
    private static Integer[] rankClustersByFitness(double[][] centroids, int k) {
        Integer[] order = new Integer[k];
        double[] fitnessScore = new double[k];

        // Weighted sum of normalized features: adherence, duration, calories, rating, sessions/week
        double[] weights = { 0.25, 0.2, 0.2, 0.15, 0.2 };

        for (int c = 0; c < k; c++) {
            double score = 0;
            for (int d = 0; d < centroids[c].length && d < weights.length; d++) {
                score += centroids[c][d] * weights[d];
            }
            fitnessScore[c] = score;
            order[c] = c;
        }

        java.util.Arrays.sort(order, (a, b) -> Double.compare(fitnessScore[a], fitnessScore[b]));
        return order;
    }
}
