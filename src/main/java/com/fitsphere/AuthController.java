package com.fitsphere;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserDAO userDAO;

    public AuthController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (req.name == null || req.email == null || req.password == null
                || req.name.isBlank() || req.email.isBlank() || req.password.isBlank()) {
            return ResponseEntity.badRequest().body(errorBody("Please fill in all required fields."));
        }
        if (userDAO.emailExists(req.email)) {
            return ResponseEntity.badRequest().body(errorBody("An account with this email already exists."));
        }

        User user = new User(req.name, req.email, req.password, req.age, req.gender,
                req.heightCm, req.weightKg, req.goal, req.activityLevel);
        int id = userDAO.registerUser(user);
        user.setUserId(id);

        return ResponseEntity.ok(new UserResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpSession session) {
        User user = userDAO.login(req.email, req.password);
        if (user == null) {
            return ResponseEntity.status(401).body(errorBody("Invalid email or password."));
        }
        session.setAttribute("userId", user.getUserId());
        return ResponseEntity.ok(new UserResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(errorBody("Not logged in."));
        }
        User user = userDAO.getUserById(userId);
        if (user == null) {
            return ResponseEntity.status(401).body(errorBody("Session user no longer exists."));
        }
        return ResponseEntity.ok(new UserResponse(user));
    }

    static java.util.Map<String, String> errorBody(String message) {
        return java.util.Collections.singletonMap("error", message);
    }
}
