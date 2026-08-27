package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.VariableExpense;
import com.atpromo.systematpromo.repository.VariableExpenseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/variable-expenses")
public class VariableExpenseController {

    private final VariableExpenseRepository variableExpenseRepository;

    public VariableExpenseController(VariableExpenseRepository variableExpenseRepository) {
        this.variableExpenseRepository = variableExpenseRepository;
    }

    @GetMapping
    public List<VariableExpense> listAll() {
        return variableExpenseRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<VariableExpense> getById(@PathVariable int id) {
        return variableExpenseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public VariableExpense create(@RequestBody VariableExpense variableExpense) {
        return variableExpenseRepository.save(variableExpense);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VariableExpense> update(@PathVariable int id, @RequestBody VariableExpense variableExpense) {
        if (!variableExpenseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        variableExpense.setId(id);
        return ResponseEntity.ok(variableExpenseRepository.save(variableExpense));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        if (!variableExpenseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        variableExpenseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}