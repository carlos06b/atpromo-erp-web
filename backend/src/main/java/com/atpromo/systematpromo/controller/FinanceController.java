package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.FinancePromoter;
import com.atpromo.systematpromo.repository.FinancePromoterRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance-promoter")
public class FinanceController {

    private final FinancePromoterRepository financePromoterRepository;

    public FinanceController(FinancePromoterRepository financePromoterRepository) {
        this.financePromoterRepository = financePromoterRepository;
    }

    @GetMapping
    public List<FinancePromoter> listAll() {
        return financePromoterRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinancePromoter> getById(@PathVariable int id) {
        return financePromoterRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public FinancePromoter create(@RequestBody FinancePromoter financePromoter) {
        return financePromoterRepository.save(financePromoter);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FinancePromoter> update(@PathVariable int id, @RequestBody FinancePromoter financePromoter) {
        if (!financePromoterRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        financePromoter.setId(id);
        return ResponseEntity.ok(financePromoterRepository.save(financePromoter));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        if (!financePromoterRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        financePromoterRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}