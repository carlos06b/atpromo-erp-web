package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.repository.PasswordResetRequestRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/password-reset-requests")
public class PasswordResetRequestController {

    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final AccessControl accessControl;

    public PasswordResetRequestController(PasswordResetRequestRepository passwordResetRequestRepository,
                                          AccessControl accessControl) {
        this.passwordResetRequestRepository = passwordResetRequestRepository;
        this.accessControl = accessControl;
    }

    @GetMapping
    public ResponseEntity<?> listPending(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return forbidden();
        }
        return ResponseEntity.ok(passwordResetRequestRepository.findByResolvedFalseOrderByRequestedAtDesc());
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolve(@PathVariable Integer id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return forbidden();
        }

        return passwordResetRequestRepository.findById(id)
                .map(request -> {
                    request.setResolved(true);
                    passwordResetRequestRepository.save(request);
                    return ResponseEntity.ok().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean isAdmin(Authentication authentication) {
        return accessControl.isAdmin(authentication);
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar isso."));
    }
}