package com.fitsphere;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendation")
public class RecommendationController {

    private final UserDAO userDAO;
    private final ActivityDAO activityDAO;
    private final WorkoutDAO workoutDAO;

    public RecommendationController(UserDAO userDAO, ActivityDAO activityDAO, WorkoutDAO workoutDAO) {
        this.userDAO = userDAO;
        this.activityDAO = activityDAO;
        this.workoutDAO = workoutDAO;
    }

    @GetMapping("/current")
    public ResponseEntity<?> current(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(AuthController.errorBody("Not logged in."));

        User user = userDAO.getUserById(userId);
        List<WorkoutPlanItem> plan = workoutDAO.getCurrentPlan(userId);

        RecommendationResponse resp = new RecommendationResponse();
        resp.segment = estimateSegment(user);
        resp.hasPlan = !plan.isEmpty();
        resp.plan = new ArrayList<>();
        for (WorkoutPlanItem item : plan) resp.plan.add(new WorkoutPlanItemResponse(item));
        resp.explanation = resp.hasPlan
                ? "Here is your current weekly plan. Log a few more activities and regenerate any time to adapt it to your recent progress."
                : "You don't have a plan yet. Click \"Generate This Week's Plan\" to get your first AI-personalized weekly workout schedule.";

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(AuthController.errorBody("Not logged in."));

        User user = userDAO.getUserById(userId);
        String segment = estimateSegment(user);

        List<ActivityLog> recentLogs = activityDAO.getRecentLogs(userId, 14);
        AdaptiveRecommendationEngine.RecommendationResult result =
                AdaptiveRecommendationEngine.generate(user, recentLogs, segment);

        workoutDAO.saveWeeklyPlan(userId, result.weeklyPlan);

        RecommendationResponse resp = new RecommendationResponse();
        resp.segment = result.segment;
        resp.adherenceRate = result.adherenceRate;
        resp.avgRating = result.avgRating;
        resp.direction = result.direction;
        resp.explanation = result.explanation;
        resp.hasPlan = true;
        resp.plan = new ArrayList<>();
        for (WorkoutPlanItem item : result.weeklyPlan) resp.plan.add(new WorkoutPlanItemResponse(item));

        return ResponseEntity.ok(resp);
    }

    /** Population-wide K-Means when enough users exist; otherwise cold-start rule. */
    private String estimateSegment(User user) {
        Map<Integer, double[]> allFeatures = activityDAO.computeFeaturesForAllUsers(30, 3);

        if (allFeatures.containsKey(user.getUserId()) && allFeatures.size() >= FitnessSegmentation.MIN_USERS_FOR_CLUSTERING) {
            Map<Integer, String> segments = FitnessSegmentation.segmentAllUsers(allFeatures);
            String segment = segments.get(user.getUserId());
            if (segment != null) return segment;
        }
        return FitnessSegmentation.classifyColdStart(user);
    }
}
