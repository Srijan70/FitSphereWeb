package com.fitsphere;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Repository
public class ActivityDAO {

    private final JdbcTemplate jdbc;

    public ActivityDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<ActivityLog> MAPPER = (rs, rowNum) -> {
        ActivityLog log = new ActivityLog();
        log.setLogId(rs.getInt("log_id"));
        log.setUserId(rs.getInt("user_id"));
        log.setLogDate(rs.getDate("log_date").toLocalDate());
        log.setActivityType(rs.getString("activity_type"));
        log.setDurationMinutes(rs.getInt("duration_minutes"));
        log.setCaloriesBurned(rs.getDouble("calories_burned"));
        log.setSteps(rs.getInt("steps"));
        log.setIntensityRating(rs.getInt("intensity_rating"));
        log.setCompleted(rs.getBoolean("completed"));
        return log;
    };

    public boolean addLog(ActivityLog log) {
        String sql = "INSERT INTO activity_logs(user_id, log_date, activity_type, duration_minutes, " +
                     "calories_burned, steps, intensity_rating, completed) VALUES(?,?,?,?,?,?,?,?)";
        int rows = jdbc.update(sql, log.getUserId(), Date.valueOf(log.getLogDate()), log.getActivityType(),
                log.getDurationMinutes(), log.getCaloriesBurned(), log.getSteps(), log.getIntensityRating(), log.isCompleted());
        return rows > 0;
    }

    public List<ActivityLog> getRecentLogs(int userId, int days) {
        String sql = "SELECT * FROM activity_logs WHERE user_id=? AND log_date >= (CURDATE() - INTERVAL ? DAY) " +
                     "ORDER BY log_date DESC, log_id DESC";
        return jdbc.query(sql, MAPPER, userId, days);
    }

    public boolean deleteLog(int logId) {
        return jdbc.update("DELETE FROM activity_logs WHERE log_id=?", logId) > 0;
    }

    public LinkedHashMap<String, Double> getWeeklyCalories(int userId) {
        LinkedHashMap<String, Double> result = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE dd");

        Map<LocalDate, Double> byDate = new HashMap<>();
        String sql = "SELECT log_date, SUM(calories_burned) as total FROM activity_logs " +
                     "WHERE user_id=? AND log_date BETWEEN ? AND ? GROUP BY log_date";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, userId, Date.valueOf(today.minusDays(6)), Date.valueOf(today));
        for (Map<String, Object> row : rows) {
            LocalDate d = ((Date) row.get("log_date")).toLocalDate();
            Object total = row.get("total");
            byDate.put(d, total == null ? 0.0 : ((Number) total).doubleValue());
        }

        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            result.put(d.format(fmt), byDate.getOrDefault(d, 0.0));
        }
        return result;
    }

    public LinkedHashMap<String, Integer> getActivityTypeDistribution(int userId, int days) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT activity_type, COUNT(*) as cnt FROM activity_logs " +
                     "WHERE user_id=? AND log_date >= (CURDATE() - INTERVAL ? DAY) " +
                     "GROUP BY activity_type ORDER BY cnt DESC";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, userId, days);
        for (Map<String, Object> row : rows) {
            result.put((String) row.get("activity_type"), ((Number) row.get("cnt")).intValue());
        }
        return result;
    }

    public List<Double> getWeeklyAdherenceTrend(int userId, int weeksBack) {
        List<Double> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int w = weeksBack - 1; w >= 0; w--) {
            LocalDate end = today.minusWeeks(w);
            LocalDate start = end.minusDays(6);
            trend.add(computeAdherenceForRange(userId, start, end));
        }
        return trend;
    }

    private double computeAdherenceForRange(int userId, LocalDate start, LocalDate end) {
        String sql = "SELECT COUNT(*) as total, SUM(completed) as completedCount FROM activity_logs " +
                     "WHERE user_id=? AND log_date BETWEEN ? AND ?";
        Map<String, Object> row = jdbc.queryForMap(sql, userId, Date.valueOf(start), Date.valueOf(end));
        int total = ((Number) row.get("total")).intValue();
        if (total == 0) return 0;
        Object completedObj = row.get("completedCount");
        int completed = completedObj == null ? 0 : ((Number) completedObj).intValue();
        return (completed / (double) total) * 100.0;
    }

    public int getCurrentStreak(int userId) {
        String sql = "SELECT DISTINCT log_date FROM activity_logs WHERE user_id=? AND completed=TRUE ORDER BY log_date DESC";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, userId);
        List<LocalDate> dates = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            dates.add(((Date) row.get("log_date")).toLocalDate());
        }
        if (dates.isEmpty()) return 0;

        int streak = 0;
        LocalDate expected = LocalDate.now();
        if (!dates.get(0).equals(expected)) {
            expected = expected.minusDays(1);
        }
        for (LocalDate d : dates) {
            if (d.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (d.isBefore(expected)) {
                break;
            }
        }
        return streak;
    }

    /** [adherenceRate(0-1), avgDurationMinutes, avgCaloriesPerSession, avgIntensityRating, sessionsPerWeek] */
    public double[] computeFeaturesForUser(int userId, int days) {
        String sql = "SELECT COUNT(*) as total, SUM(completed) as completedCount, " +
                     "AVG(duration_minutes) as avgDuration, AVG(calories_burned) as avgCalories, " +
                     "AVG(intensity_rating) as avgRating FROM activity_logs " +
                     "WHERE user_id=? AND log_date >= (CURDATE() - INTERVAL ? DAY)";
        Map<String, Object> row = jdbc.queryForMap(sql, userId, days);
        int total = ((Number) row.get("total")).intValue();
        if (total == 0) return null;

        double adherence = ((Number) row.get("completedCount")).doubleValue() / total;
        double avgDuration = ((Number) row.get("avgDuration")).doubleValue();
        double avgCalories = ((Number) row.get("avgCalories")).doubleValue();
        double avgRating = ((Number) row.get("avgRating")).doubleValue();
        double sessionsPerWeek = total / (days / 7.0);

        return new double[] { adherence, avgDuration, avgCalories, avgRating, sessionsPerWeek };
    }

    public Map<Integer, double[]> computeFeaturesForAllUsers(int days, int minLogs) {
        Map<Integer, double[]> features = new HashMap<>();
        String sql = "SELECT user_id FROM activity_logs WHERE log_date >= (CURDATE() - INTERVAL ? DAY) " +
                     "GROUP BY user_id HAVING COUNT(*) >= ?";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, days, minLogs);
        for (Map<String, Object> row : rows) {
            int uid = ((Number) row.get("user_id")).intValue();
            double[] f = computeFeaturesForUser(uid, days);
            if (f != null) features.put(uid, f);
        }
        return features;
    }
}
