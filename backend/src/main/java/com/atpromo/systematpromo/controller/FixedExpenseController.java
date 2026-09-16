package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.FixedExpense;
import com.atpromo.systematpromo.repository.FixedExpenseHistoryRepository;
import com.atpromo.systematpromo.repository.FixedExpenseRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/fixed-expenses")
public class FixedExpenseController {

    private final FixedExpenseRepository fixedExpenseRepository;
    private final FixedExpenseHistoryRepository fixedExpenseHistoryRepository;
    private final AccessControl accessControl;

    public FixedExpenseController(FixedExpenseRepository fixedExpenseRepository,
                                  FixedExpenseHistoryRepository fixedExpenseHistoryRepository,
                                  AccessControl accessControl) {
        this.fixedExpenseRepository = fixedExpenseRepository;
        this.fixedExpenseHistoryRepository = fixedExpenseHistoryRepository;
        this.accessControl = accessControl;
    }

    @GetMapping
    public ResponseEntity<?> listAll(Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        return ResponseEntity.ok(fixedExpenseRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        return fixedExpenseRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody FixedExpense fixedExpense, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        return ResponseEntity.ok(fixedExpenseRepository.save(fixedExpense));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody FixedExpense fixedExpense, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        if (!fixedExpenseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        fixedExpense.setId(id);
        return ResponseEntity.ok(fixedExpenseRepository.save(fixedExpense));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        if (!fixedExpenseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (fixedExpenseHistoryRepository.existsByFixedExpenseId(id)) {
            return ResponseEntity.status(409).body(Map.of(
                    "message",
                    "Essa despesa já tem meses gerados no histórico (usados em relatórios) e não pode ser excluída. Marque-a como cancelada em vez de excluir, para preservar esse histórico."
            ));
        }
        fixedExpenseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar despesas."));
    }
}