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

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessControl accessControl;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, AccessControl accessControl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessControl = accessControl;
    }

    public record UserView(Integer id, String name, String email, String jobTittle) {}

    @GetMapping
    public ResponseEntity<?> listAll(Authentication authentication) {
        if (!accessControl.isAdmin(authentication)) {
            return forbidden();
        }
        List<UserView> users = userRepository.findAll().stream()
                .map(this::toView)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id, Authentication authentication) {
        if (!accessControl.isAdmin(authentication)) {
            return forbidden();
        }
        return userRepository.findById(id)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(toView(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody User user, Authentication authentication) {
        if (!accessControl.isAdmin(authentication)) {
            return forbidden();
        }
        user.setId(null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);
        return ResponseEntity.ok(toView(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody User user, Authentication authentication) {
        if (!accessControl.isAdmin(authentication)) {
            return forbidden();
        }
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        user.setId(id);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);
        return ResponseEntity.ok(toView(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id, Authentication authentication) {
        if (!accessControl.isAdmin(authentication)) {
            return forbidden();
        }
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private UserView toView(User user) {
        return new UserView(user.getId(), user.getName(), user.getEmail(), user.getJobTittle());
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Apenas administradores podem gerenciar usuários."));
    }
}