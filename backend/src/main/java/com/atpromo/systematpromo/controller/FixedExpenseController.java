package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.FixedExpense;
import com.atpromo.systematpromo.repository.FixedExpenseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fixed-expenses")
public class FixedExpenseController {

    private final FixedExpenseRepository fixedExpenseRepository;

    public FixedExpenseController(FixedExpenseRepository fixedExpenseRepository) {
        this.fixedExpenseRepository = fixedExpenseRepository;
    }

    @GetMapping
    public List<FixedExpense> listAll() {
        return fixedExpenseRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FixedExpense> getById(@PathVariable int id) {
        return fixedExpenseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public FixedExpense create(@RequestBody FixedExpense fixedExpense) {
        return fixedExpenseRepository.save(fixedExpense);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FixedExpense> update(@PathVariable int id, @RequestBody FixedExpense fixedExpense) {
        if (!fixedExpenseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        fixedExpense.setId(id);
        return ResponseEntity.ok(fixedExpenseRepository.save(fixedExpense));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        if (!fixedExpenseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        fixedExpenseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}