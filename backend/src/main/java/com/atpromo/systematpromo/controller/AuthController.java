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
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // ACHADO H4: o bloqueio agora é por email + IP (ver LoginAttemptService),
        // para que trancar a conta de outra pessoa exija estar atrás do
        // mesmo IP que ela, em vez de bastar saber o email.
        String ip = clientIp(httpRequest);

        if (loginAttemptService.isBlocked(request.email(), ip)) {
            long minutes = loginAttemptService.minutesRemaining(request.email(), ip);
            return ResponseEntity.status(429).body(Map.of(
                    "message",
                    "Muitas tentativas de login com esse e-mail. Tente novamente em cerca de " + minutes + " minuto(s)."
            ));
        }

        Optional<User> userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty()) {
            loginAttemptService.registerFailure(request.email(), ip);
            return ResponseEntity.status(401).body("Email ou senha inválidos.");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptService.registerFailure(request.email(), ip);
            return ResponseEntity.status(401).body("Email ou senha inválidos.");
        }

        loginAttemptService.registerSuccess(request.email(), ip);

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

    // ACHADO H3: antes confiava sem validacao no header X-Forwarded-For,
    // que e enviado pelo PROPRIO cliente - bastava variar esse header a
    // cada chamada para nunca bater o limite de 5 solicitacoes/hora por IP
    // em PasswordResetLimiter. Agora usamos so request.getRemoteAddr(), que
    // e o IP da conexao TCP real (nao controlavel pelo cliente). Se esta
    // aplicacao algum dia ficar atras de um proxy confiavel que precise
    // reescrever o IP (ex. um load balancer que normalize X-Forwarded-For),
    // isso deve voltar a usar o header, mas validando explicitamente que a
    // requisicao veio desse proxy confiavel - nunca confiando no primeiro
    // valor do header como estava antes.
    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}