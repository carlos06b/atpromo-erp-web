package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Promoter;
import com.atpromo.systematpromo.repository.PromoterRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promoters")
public class PromoterController {

    private final PromoterRepository promoterRepository;

    public PromoterController(PromoterRepository promoterRepository) {
        this.promoterRepository = promoterRepository;
    }

    @GetMapping
    public List<Promoter> listAll() {
        return promoterRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Promoter> getById(@PathVariable int id) {
        return promoterRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Promoter create(@RequestBody Promoter promoter) {
        return promoterRepository.save(promoter);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Promoter> update(@PathVariable int id, @RequestBody Promoter promoter) {
        if (!promoterRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        promoter.setId(id);
        return ResponseEntity.ok(promoterRepository.save(promoter));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        if (!promoterRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        promoterRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}