package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.repository.UserRepository;
import com.atpromo.systematpromo.security.JwtUtil;
import com.atpromo.systematpromo.security.LoginAttemptService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.loginAttemptService = loginAttemptService;
    }

    public record LoginRequest(String email, String password) {}
    public record LoginResponse(String token, String name, String jobTittle) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (loginAttemptService.isBlocked(request.email())) {
            long minutes = loginAttemptService.minutesRemaining(request.email());
            return ResponseEntity.status(429).body(Map.of(
                    "message",
                    "Muitas tentativas de login com esse e-mail. Tente novamente em cerca de " + minutes + " minuto(s)."
            ));
        }

        Optional<User> userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty()) {
            loginAttemptService.registerFailure(request.email());
            return ResponseEntity.status(401).body("Email ou senha inválidos.");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptService.registerFailure(request.email());
            return ResponseEntity.status(401).body("Email ou senha inválidos.");
        }

        loginAttemptService.registerSuccess(request.email());
        String token = jwtUtil.generateToken(user.getEmail());

        return ResponseEntity.ok(new LoginResponse(token, user.getName(), user.getJobTittle()));
    }
}