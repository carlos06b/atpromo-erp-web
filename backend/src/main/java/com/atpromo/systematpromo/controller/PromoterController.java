package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Promoter;
import com.atpromo.systematpromo.repository.PromoterRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/promoters")
public class PromoterController {

    private final PromoterRepository promoterRepository;
    private final AccessControl accessControl;

    public PromoterController(PromoterRepository promoterRepository, AccessControl accessControl) {
        this.promoterRepository = promoterRepository;
        this.accessControl = accessControl;
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
    public ResponseEntity<?> create(@RequestBody Promoter promoter, Authentication authentication) {
        if (accessControl.isFinance(authentication)) {
            return forbidden();
        }
        promoter.setId(null);
        return ResponseEntity.ok(promoterRepository.save(promoter));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Promoter promoter, Authentication authentication) {
        if (accessControl.isFinance(authentication)) {
            return forbidden();
        }
        if (!promoterRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        promoter.setId(id);
        return ResponseEntity.ok(promoterRepository.save(promoter));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id, Authentication authentication) {
        if (accessControl.isFinance(authentication)) {
            return forbidden();
        }
        if (!promoterRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        promoterRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para alterar promotores."));
    }
}