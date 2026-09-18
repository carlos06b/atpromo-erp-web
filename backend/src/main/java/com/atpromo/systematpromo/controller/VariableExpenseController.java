package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.VariableExpense;
import com.atpromo.systematpromo.repository.VariableExpenseRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/variable-expenses")
public class VariableExpenseController {

    private final VariableExpenseRepository variableExpenseRepository;
    private final AccessControl accessControl;

    public VariableExpenseController(VariableExpenseRepository variableExpenseRepository, AccessControl accessControl) {
        this.variableExpenseRepository = variableExpenseRepository;
        this.accessControl = accessControl;
    }

    @GetMapping
    public ResponseEntity<?> listAll(Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        return ResponseEntity.ok(variableExpenseRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        return variableExpenseRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody VariableExpense variableExpense, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        variableExpense.setId(null);
        return ResponseEntity.ok(variableExpenseRepository.save(variableExpense));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody VariableExpense variableExpense, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        if (!variableExpenseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        variableExpense.setId(id);
        return ResponseEntity.ok(variableExpenseRepository.save(variableExpense));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id, Authentication authentication) {
        if (accessControl.isRh(authentication)) {
            return forbidden();
        }
        if (!variableExpenseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        variableExpenseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar despesas."));
    }
}