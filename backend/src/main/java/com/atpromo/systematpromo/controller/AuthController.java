package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.PasswordResetRequest;
import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.repository.PasswordResetRequestRepository;
import com.atpromo.systematpromo.repository.UserRepository;
import com.atpromo.systematpromo.security.JwtUtil;
import com.atpromo.systematpromo.security.LoginAttemptService;
import com.atpromo.systematpromo.security.PasswordResetLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String FORGOT_PASSWORD_GENERIC_MESSAGE =
            "Se esse email existir em nossa base, sua solicitação foi registrada. O administrador vai entrar em contato.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;
    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final PasswordResetLimiter passwordResetLimiter;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          LoginAttemptService loginAttemptService,
                          PasswordResetRequestRepository passwordResetRequestRepository,
                          PasswordResetLimiter passwordResetLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.loginAttemptService = loginAttemptService;
        this.passwordResetRequestRepository = passwordResetRequestRepository;
        this.passwordResetLimiter = passwordResetLimiter;
    }

    public record LoginRequest(String email, String password, Boolean rememberMe) {}
    public record LoginResponse(String token, String name, String jobTittle) {}
    public record ForgotPasswordRequest(String email) {}

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

        boolean remember = Boolean.TRUE.equals(request.rememberMe());
        String token = remember
                ? jwtUtil.generateToken(user.getEmail(), JwtUtil.REMEMBER_ME_EXPIRATION_MS)
                : jwtUtil.generateToken(user.getEmail());

        return ResponseEntity.ok(new LoginResponse(token, user.getName(), user.getJobTittle()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();

        if (email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Informe um email."));
        }

        String ip = clientIp(httpRequest);

        if (passwordResetLimiter.isIpBlocked(ip)) {
            return ResponseEntity.status(429).body(Map.of(
                    "message", "Muitas solicitações vindas deste local. Tente novamente mais tarde."
            ));
        }
        passwordResetLimiter.registerIpRequest(ip);

        if (!passwordResetLimiter.isEmailInCooldown(email)) {
            passwordResetLimiter.registerEmailRequest(email);

            if (userRepository.findByEmail(email).isPresent()) {
                PasswordResetRequest resetRequest = new PasswordResetRequest();
                resetRequest.setEmail(email);
                resetRequest.setRequestedAt(LocalDateTime.now());
                resetRequest.setResolved(false);
                passwordResetRequestRepository.save(resetRequest);
            }
        }

        // Sempre a mesma resposta, exista ou não o email cadastrado -
        // evita que alguém descubra quais emails têm conta testando aqui.
        return ResponseEntity.ok(Map.of("message", FORGOT_PASSWORD_GENERIC_MESSAGE));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}