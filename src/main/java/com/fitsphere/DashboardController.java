package com.fitsphere;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final UserDAO userDAO;
    private final ActivityDAO activityDAO;

    public DashboardController(UserDAO userDAO, ActivityDAO activityDAO) {
        this.userDAO = userDAO;
        this.activityDAO = activityDAO;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(AuthController.errorBody("Not logged in."));

        User user = userDAO.getUserById(userId);
        if (user == null) return ResponseEntity.status(404).build();

        double bmi = FitnessCalculator.calculateBMI(user.getWeightKg(), user.getHeightCm());
        double bmr = FitnessCalculator.calculateBMR(user.getGender(), user.getWeightKg(), user.getHeightCm(), user.getAge());
        double tdee = FitnessCalculator.calculateTDEE(bmr, user.getActivityLevel());
        double calorieTarget = FitnessCalculator.calculateCalorieTarget(tdee, user.getGoal());

        DashboardSummaryResponse resp = new DashboardSummaryResponse();
        resp.name = user.getName();
        resp.bmi = bmi;
        resp.bmiCategory = FitnessCalculator.bmiCategory(bmi);
        resp.calorieTarget = calorieTarget;
        resp.streak = activityDAO.getCurrentStreak(userId);
        resp.goal = user.getGoal();

        return ResponseEntity.ok(resp);
    }
}
