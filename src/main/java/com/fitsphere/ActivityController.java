package com.fitsphere;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final UserDAO userDAO;
    private final ActivityDAO activityDAO;

    public ActivityController(UserDAO userDAO, ActivityDAO activityDAO) {
        this.userDAO = userDAO;
        this.activityDAO = activityDAO;
    }

    private Integer requireUser(HttpSession session) {
        return (Integer) session.getAttribute("userId");
    }

    @GetMapping("/catalog")
    public ResponseEntity<?> catalog() {
        List<WorkoutTypeOption> options = new ArrayList<>();
        for (WorkoutCatalog.WorkoutType wt : WorkoutCatalog.ALL) {
            options.add(new WorkoutTypeOption(wt));
        }
        return ResponseEntity.ok(options);
    }

    @GetMapping("/estimate")
    public ResponseEntity<?> estimate(@RequestParam String type, @RequestParam int duration, HttpSession session) {
        Integer userId = requireUser(session);
        if (userId == null) return ResponseEntity.status(401).body(AuthController.errorBody("Not logged in."));

        User user = userDAO.getUserById(userId);
        WorkoutCatalog.WorkoutType wt = WorkoutCatalog.findByName(type);
        if (wt == null) return ResponseEntity.badRequest().body(AuthController.errorBody("Unknown activity type."));

        double calories = FitnessCalculator.estimateCaloriesBurned(wt.met, user.getWeightKg(), duration);
        return ResponseEntity.ok(new CalorieEstimateResponse(calories));
    }

    @PostMapping
    public ResponseEntity<?> logActivity(@RequestBody ActivityLogRequest req, HttpSession session) {
        Integer userId = requireUser(session);
        if (userId == null) return ResponseEntity.status(401).body(AuthController.errorBody("Not logged in."));

        User user = userDAO.getUserById(userId);
        WorkoutCatalog.WorkoutType wt = WorkoutCatalog.findByName(req.activityType);
        if (wt == null) return ResponseEntity.badRequest().body(AuthController.errorBody("Unknown activity type."));

        double calories = FitnessCalculator.estimateCaloriesBurned(wt.met, user.getWeightKg(), req.durationMinutes);

        ActivityLog log = new ActivityLog(userId, LocalDate.now(), req.activityType, req.durationMinutes,
                calories, req.steps, req.intensityRating, req.completed);

        boolean ok = activityDAO.addLog(log);
        if (!ok) return ResponseEntity.status(500).body(AuthController.errorBody("Could not save activity."));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/recent")
    public ResponseEntity<?> recent(@RequestParam(defaultValue = "30") int days, HttpSession session) {
        Integer userId = requireUser(session);
        if (userId == null) return ResponseEntity.status(401).body(AuthController.errorBody("Not logged in."));

        List<ActivityLogResponse> resp = new ArrayList<>();
        for (ActivityLog log : activityDAO.getRecentLogs(userId, days)) {
            resp.add(new ActivityLogResponse(log));
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/weekly-calories")
    public ResponseEntity<?> weeklyCalories(HttpSession session) {
        Integer userId = requireUser(session);
        if (userId == null) return ResponseEntity.status(401).body(AuthController.errorBody("Not logged in."));

        LinkedHashMap<String, Double> data = activityDAO.getWeeklyCalories(userId);
        return ResponseEntity.ok(new ChartSeriesResponse(new ArrayList<>(data.keySet()), new ArrayList<>(data.values())));
    }

    @GetMapping("/type-distribution")
    public ResponseEntity<?> typeDistribution(@RequestParam(defaultValue = "30") int days, HttpSession session) {
        Integer userId = requireUser(session);
        if (userId == null) return ResponseEntity.status(401).body(AuthController.errorBody("Not logged in."));

        LinkedHashMap<String, Integer> data = activityDAO.getActivityTypeDistribution(userId, days);
        return ResponseEntity.ok(new TypeDistributionResponse(new ArrayList<>(data.keySet()), new ArrayList<>(data.values())));
    }

    @GetMapping("/adherence-trend")
    public ResponseEntity<?> adherenceTrend(@RequestParam(defaultValue = "4") int weeks, HttpSession session) {
        Integer userId = requireUser(session);
        if (userId == null) return ResponseEntity.status(401).body(AuthController.errorBody("Not logged in."));

        List<Double> values = activityDAO.getWeeklyAdherenceTrend(userId, weeks);
        List<String> labels = new ArrayList<>();
        for (int i = weeks - 1; i >= 0; i--) {
            labels.add(i == 0 ? "This Wk" : "Wk -" + i);
        }
        return ResponseEntity.ok(new ChartSeriesResponse(labels, values));
    }
}
