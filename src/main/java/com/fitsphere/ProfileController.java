package com.fitsphere;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserDAO userDAO;

    public ProfileController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @GetMapping
    public ResponseEntity<?> getProfile(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(AuthController.errorBody("Not logged in."));

        User user = userDAO.getUserById(userId);
        if (user == null) return ResponseEntity.status(404).build();
        return ResponseEntity.ok(new UserResponse(user));
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody ProfileUpdateRequest req, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(AuthController.errorBody("Not logged in."));

        User user = userDAO.getUserById(userId);
        if (user == null) return ResponseEntity.status(404).build();

        user.setName(req.name);
        user.setAge(req.age);
        user.setGender(req.gender);
        user.setHeightCm(req.heightCm);
        user.setWeightKg(req.weightKg);
        user.setGoal(req.goal);
        user.setActivityLevel(req.activityLevel);

        boolean ok = userDAO.updateProfile(user);
        if (!ok) return ResponseEntity.status(500).body(AuthController.errorBody("Update failed."));
        return ResponseEntity.ok(new UserResponse(user));
    }

    @GetMapping("/numbers")
    public ResponseEntity<?> getNumbers(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(AuthController.errorBody("Not logged in."));

        User user = userDAO.getUserById(userId);
        if (user == null) return ResponseEntity.status(404).build();

        ProfileNumbersResponse resp = computeNumbers(user);
        return ResponseEntity.ok(resp);
    }

    static ProfileNumbersResponse computeNumbers(User user) {
        double bmi = FitnessCalculator.calculateBMI(user.getWeightKg(), user.getHeightCm());
        double bmr = FitnessCalculator.calculateBMR(user.getGender(), user.getWeightKg(), user.getHeightCm(), user.getAge());
        double tdee = FitnessCalculator.calculateTDEE(bmr, user.getActivityLevel());
        double calorieTarget = FitnessCalculator.calculateCalorieTarget(tdee, user.getGoal());
        int[] macros = FitnessCalculator.macroSplitGrams(calorieTarget, user.getGoal());

        ProfileNumbersResponse resp = new ProfileNumbersResponse();
        resp.bmi = bmi;
        resp.bmiCategory = FitnessCalculator.bmiCategory(bmi);
        resp.bmr = bmr;
        resp.tdee = tdee;
        resp.calorieTarget = calorieTarget;
        resp.proteinG = macros[0];
        resp.carbG = macros[1];
        resp.fatG = macros[2];
        return resp;
    }
}
