package com.fitsphere;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserDAO {

    private final JdbcTemplate jdbc;

    public UserDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<User> MAPPER = (rs, rowNum) -> {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setAge(rs.getInt("age"));
        u.setGender(rs.getString("gender"));
        u.setHeightCm(rs.getDouble("height_cm"));
        u.setWeightKg(rs.getDouble("weight_kg"));
        u.setGoal(rs.getString("goal"));
        u.setActivityLevel(rs.getString("activity_level"));
        return u;
    };

    public int registerUser(User user) {
        String sql = "INSERT INTO users(name, email, password, age, gender, height_cm, weight_kg, goal, activity_level) " +
                     "VALUES(?,?,?,?,?,?,?,?,?)";
        jdbc.update(sql, user.getName(), user.getEmail(), user.getPassword(), user.getAge(), user.getGender(),
                user.getHeightCm(), user.getWeightKg(), user.getGoal(), user.getActivityLevel());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    public User login(String email, String password) {
        List<User> results = jdbc.query("SELECT * FROM users WHERE email=? AND password=?", MAPPER, email, password);
        return results.isEmpty() ? null : results.get(0);
    }

    public User getUserById(int userId) {
        List<User> results = jdbc.query("SELECT * FROM users WHERE user_id=?", MAPPER, userId);
        return results.isEmpty() ? null : results.get(0);
    }

    public boolean updateProfile(User user) {
        String sql = "UPDATE users SET name=?, age=?, gender=?, height_cm=?, weight_kg=?, goal=?, activity_level=? " +
                     "WHERE user_id=?";
        int rows = jdbc.update(sql, user.getName(), user.getAge(), user.getGender(), user.getHeightCm(),
                user.getWeightKg(), user.getGoal(), user.getActivityLevel(), user.getUserId());
        return rows > 0;
    }

    public boolean emailExists(String email) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email=?", Integer.class, email);
        return count != null && count > 0;
    }
}
