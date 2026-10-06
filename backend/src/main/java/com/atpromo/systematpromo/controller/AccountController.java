package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.repository.UserRepository;
import com.atpromo.systematpromo.security.DeleteConfirmationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeleteConfirmationService deleteConfirmationService;

    public AccountController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                              DeleteConfirmationService deleteConfirmationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.deleteConfirmationService = deleteConfirmationService;
    }

    public record VerifyPasswordRequest(String password) {}

    // ACHADO C6 (crítico) da auditoria de 05/10/2026: antes, este endpoint só
    // confirmava a senha e devolvia 200 — sem nenhum vínculo com a exclusão
    // real que o frontend dispara em seguida. Agora, em caso de sucesso,
    // emite um ticket de uso único (DeleteConfirmationService) que precisa
    // ser enviado no header "X-Delete-Confirmation" do DELETE real, exigido
    // por DeleteConfirmationFilter em toda rota /api/**.
    @PostMapping("/verify-password")
    public ResponseEntity<?> verifyPassword(@RequestBody VerifyPasswordRequest request, Authentication authentication) {
        String email = authentication.getName();

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.password(), userOpt.get().getPassword())) {
            return ResponseEntity.status(401).body("Senha incorreta.");
        }

        String ticket = deleteConfirmationService.issueTicket(email);
        return ResponseEntity.ok(Map.of("ticket", ticket));
    }
}
