package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record VerifyPasswordRequest(String password) {}

    @PostMapping("/verify-password")
    public ResponseEntity<?> verifyPassword(@RequestBody VerifyPasswordRequest request, Authentication authentication) {
        String email = authentication.getName();

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPassword())) {
            return ResponseEntity.status(401).body("Senha incorreta.");
        }

        return ResponseEntity.ok().build();
    }
}