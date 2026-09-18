package com.fitsphere;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WorkoutDAO {

    private final JdbcTemplate jdbc;

    public WorkoutDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<WorkoutPlanItem> MAPPER = (rs, rowNum) -> {
        WorkoutPlanItem item = new WorkoutPlanItem();
        item.setItemId(rs.getInt("item_id"));
        item.setUserId(rs.getInt("user_id"));
        item.setDayOfWeek(rs.getString("day_of_week"));
        item.setWorkoutType(rs.getString("workout_type"));
        item.setDurationMinutes(rs.getInt("duration_minutes"));
        item.setIntensityLevel(rs.getInt("intensity_level"));
        return item;
    };

    public boolean saveWeeklyPlan(int userId, List<WorkoutPlanItem> items) {
        jdbc.update("DELETE FROM workout_plan_items WHERE user_id=?", userId);
        String insertSql = "INSERT INTO workout_plan_items(user_id, day_of_week, workout_type, " +
                            "duration_minutes, intensity_level) VALUES(?,?,?,?,?)";
        for (WorkoutPlanItem item : items) {
            jdbc.update(insertSql, item.getUserId(), item.getDayOfWeek(), item.getWorkoutType(),
                    item.getDurationMinutes(), item.getIntensityLevel());
        }
        return true;
    }

    public List<WorkoutPlanItem> getCurrentPlan(int userId) {
        String sql = "SELECT * FROM workout_plan_items WHERE user_id=? " +
                     "ORDER BY FIELD(day_of_week,'MON','TUE','WED','THU','FRI','SAT','SUN')";
        return jdbc.query(sql, MAPPER, userId);
    }
}
