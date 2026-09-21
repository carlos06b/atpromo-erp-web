package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.repository.UserRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final List<String> ALLOWED_ROLES = List.of("RH", "FINANCEIRO", "SUPERVISOR", "ADMIN");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessControl accessControl;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, AccessControl accessControl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessControl = accessControl;
    }

    public record UserView(Integer id, String name, String email, String jobTittle) {}
    public record UserRequest(String name, String email, String password, String jobTittle) {}

    @GetMapping
    public ResponseEntity<?> listAll(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return forbidden();
        }
        List<UserView> views = userRepository.findAll().stream().map(this::toView).toList();
        return ResponseEntity.ok(views);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return forbidden();
        }
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(toView(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody UserRequest request, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return forbidden();
        }

        String validationError = validate(request, true);
        if (validationError != null) {
            return badRequest(validationError);
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            return badRequest("Já existe um usuário com esse email.");
        }

        User user = new User();
        user.setId(null); // proteção contra mass assignment
        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setJobTittle(request.jobTittle().trim().toUpperCase());
        user.setPassword(passwordEncoder.encode(request.password()));

        User saved = userRepository.save(user);
        return ResponseEntity.ok(toView(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody UserRequest request, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return forbidden();
        }

        Optional<User> existingOpt = userRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean changingPassword = request.password() != null && !request.password().isBlank();
        String validationError = validate(request, changingPassword);
        if (validationError != null) {
            return badRequest(validationError);
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        Optional<User> emailOwner = userRepository.findByEmail(normalizedEmail);
        if (emailOwner.isPresent() && !emailOwner.get().getId().equals(id)) {
            return badRequest("Já existe um usuário com esse email.");
        }

        User user = existingOpt.get();
        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setJobTittle(request.jobTittle().trim().toUpperCase());

        if (changingPassword) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(toView(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return forbidden();
        }

        User currentUser = accessControl.currentUser(authentication);
        if (currentUser != null && currentUser.getId() != null && currentUser.getId() == id) {
            return badRequest("Você não pode excluir o próprio usuário logado.");
        }

        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String validate(UserRequest request, boolean requirePassword) {
        if (request.name() == null || request.name().isBlank()) {
            return "Informe o nome.";
        }
        if (request.email() == null || !EMAIL_PATTERN.matcher(request.email().trim()).matches()) {
            return "Informe um email válido.";
        }
        if (request.jobTittle() == null || !ALLOWED_ROLES.contains(request.jobTittle().trim().toUpperCase())) {
            return "Selecione um perfil válido (RH, FINANCEIRO, SUPERVISOR ou ADMIN).";
        }
        if (requirePassword && (request.password() == null || request.password().length() < 8)) {
            return "A senha deve ter pelo menos 8 caracteres.";
        }
        return null;
    }

    private UserView toView(User user) {
        return new UserView(user.getId(), user.getName(), user.getEmail(), user.getJobTittle());
    }

    private boolean isAdmin(Authentication authentication) {
        return accessControl.isAdmin(authentication);
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para gerenciar usuários."));
    }
}