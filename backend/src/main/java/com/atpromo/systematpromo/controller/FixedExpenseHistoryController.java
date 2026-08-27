package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.FixedExpenseHistory;
import com.atpromo.systematpromo.repository.FixedExpenseHistoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fixed-expense-history")
public class FixedExpenseHistoryController {

    private final FixedExpenseHistoryRepository fixedExpenseHistoryRepository;

    public FixedExpenseHistoryController(FixedExpenseHistoryRepository fixedExpenseHistoryRepository) {
        this.fixedExpenseHistoryRepository = fixedExpenseHistoryRepository;
    }

    @GetMapping
    public List<FixedExpenseHistory> listAll() {
        return fixedExpenseHistoryRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FixedExpenseHistory> getById(@PathVariable int id) {
        return fixedExpenseHistoryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public FixedExpenseHistory create(@RequestBody FixedExpenseHistory fixedExpenseHistory) {
        return fixedExpenseHistoryRepository.save(fixedExpenseHistory);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FixedExpenseHistory> update(@PathVariable int id, @RequestBody FixedExpenseHistory fixedExpenseHistory) {
        if (!fixedExpenseHistoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        fixedExpenseHistory.setId(id);
        return ResponseEntity.ok(fixedExpenseHistoryRepository.save(fixedExpenseHistory));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        if (!fixedExpenseHistoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        fixedExpenseHistoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}