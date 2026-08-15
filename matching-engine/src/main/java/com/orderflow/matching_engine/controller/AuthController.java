package com.orderflow.matching_engine.controller;

import com.orderflow.matching_engine.model.User;
import com.orderflow.matching_engine.repository.UserRepository;
import com.orderflow.matching_engine.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }

        User newUser = new User(username, passwordEncoder.encode(password));
        // Explicitly inject baseline demo assets during registration
        newUser.setBalanceUsd(new BigDecimal("100000.00"));
        newUser.setBalanceBtc(new BigDecimal("10.00"));

        userRepository.save(newUser);

        String token = jwtUtil.generateToken(username);
        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully",
                "token", token,
                "username", username,
                "balanceUsd", newUser.getBalanceUsd(),
                "balanceBtc", newUser.getBalanceBtc()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        User user = userOpt.get();
        String token = jwtUtil.generateToken(username);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "balanceUsd", user.getBalanceUsd(),
                "balanceBtc", user.getBalanceBtc()));
    }
}