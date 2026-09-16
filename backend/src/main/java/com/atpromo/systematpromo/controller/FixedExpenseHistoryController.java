package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.FixedExpenseHistory;
import com.atpromo.systematpromo.repository.FixedExpenseHistoryRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/fixed-expense-history")
public class FixedExpenseHistoryController {

    private final FixedExpenseHistoryRepository fixedExpenseHistoryRepository;
    private final AccessControl accessControl;

    public FixedExpenseHistoryController(FixedExpenseHistoryRepository fixedExpenseHistoryRepository, AccessControl accessControl) {
        this.fixedExpenseHistoryRepository = fixedExpenseHistoryRepository;
        this.accessControl = accessControl;
    }

    @GetMapping
    public ResponseEntity<?> listAll(Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        return ResponseEntity.ok(fixedExpenseHistoryRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        return fixedExpenseHistoryRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody FixedExpenseHistory fixedExpenseHistory, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        return ResponseEntity.ok(fixedExpenseHistoryRepository.save(fixedExpenseHistory));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody FixedExpenseHistory fixedExpenseHistory, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        if (!fixedExpenseHistoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        fixedExpenseHistory.setId(id);
        return ResponseEntity.ok(fixedExpenseHistoryRepository.save(fixedExpenseHistory));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        if (!fixedExpenseHistoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        fixedExpenseHistoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar despesas."));
    }
}