package com.fitsphere;

import java.util.Random;

/**
 * A minimal, dependency-free K-Means clustering implementation
 * (with k-means++ centroid initialization for stability).
 *
 * This is the unsupervised-learning building block used by
 * FitnessSegmentation to group users into fitness levels.
 */
public class KMeans {

    private final int k;
    private final int maxIterations;
    private double[][] centroids;
    private static final long SEED = 42L; // fixed seed => reproducible clustering for demos

    public KMeans(int k, int maxIterations) {
        this.k = k;
        this.maxIterations = maxIterations;
    }

    /** Runs clustering on the given data matrix (rows = samples). Returns cluster label per row. */
    public int[] fit(double[][] data) {
        int n = data.length;
        int dim = data[0].length;

        centroids = initCentroidsPlusPlus(data);
        int[] labels = new int[n];

        for (int iter = 0; iter < maxIterations; iter++) {

            boolean changed = false;
            for (int i = 0; i < n; i++) {
                int best = closestCentroid(data[i]);
                if (labels[i] != best) {
                    labels[i] = best;
                    changed = true;
                }
            }

            double[][] newCentroids = new double[k][dim];
            int[] counts = new int[k];

            for (int i = 0; i < n; i++) {
                int c = labels[i];
                counts[c]++;
                for (int d = 0; d < dim; d++) {
                    newCentroids[c][d] += data[i][d];
                }
            }

            for (int c = 0; c < k; c++) {
                if (counts[c] == 0) {
                    newCentroids[c] = centroids[c]; // keep old centroid if cluster is empty
                    continue;
                }
                for (int d = 0; d < dim; d++) {
                    newCentroids[c][d] /= counts[c];
                }
            }

            centroids = newCentroids;
            if (!changed) break;
        }

        return labels;
    }

    private double[][] initCentroidsPlusPlus(double[][] data) {
        Random rand = new Random(SEED);
        int n = data.length;
        int dim = data[0].length;
        double[][] chosen = new double[k][dim];

        int firstIdx = rand.nextInt(n);
        chosen[0] = data[firstIdx].clone();

        double[] distSq = new double[n];

        for (int c = 1; c < k; c++) {
            double sum = 0;
            for (int i = 0; i < n; i++) {
                double minD = Double.MAX_VALUE;
                for (int cc = 0; cc < c; cc++) {
                    double d = distanceSquared(data[i], chosen[cc]);
                    if (d < minD) minD = d;
                }
                distSq[i] = minD;
                sum += minD;
            }

            double r = rand.nextDouble() * sum;
            double cum = 0;
            int pick = n - 1;
            for (int i = 0; i < n; i++) {
                cum += distSq[i];
                if (cum >= r) { pick = i; break; }
            }
            chosen[c] = data[pick].clone();
        }

        return chosen;
    }

    private int closestCentroid(double[] point) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int c = 0; c < centroids.length; c++) {
            double d = distanceSquared(point, centroids[c]);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        return best;
    }

    private double distanceSquared(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return sum;
    }

    public double[][] getCentroids() {
        return centroids;
    }
}
