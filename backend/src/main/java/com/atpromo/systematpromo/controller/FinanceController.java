package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.FinancePromoter;
import com.atpromo.systematpromo.repository.FinancePromoterRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance-promoter")
public class FinanceController {

    private final FinancePromoterRepository financePromoterRepository;
    private final AccessControl accessControl;

    public FinanceController(FinancePromoterRepository financePromoterRepository, AccessControl accessControl) {
        this.financePromoterRepository = financePromoterRepository;
        this.accessControl = accessControl;
    }

    // ACHADO C3 (crítico) da auditoria de 05/10/2026: nenhum dos 5 endpoints
    // deste controller tinha qualquer checagem de papel — qualquer usuário
    // autenticado conseguia ler, criar, editar ou apagar lançamentos
    // financeiros de qualquer promotor. O único lugar do frontend que chama
    // essa API é a tela de Folha de Pagamento (RH/FINANCEIRO/ADMIN).
    @GetMapping
    public ResponseEntity<?> listAll(Authentication authentication) {
        if (!allowed(authentication)) {
            return forbidden();
        }
        return ResponseEntity.ok(financePromoterRepository.findAll().stream()
                .filter(f -> f.getSourceRequestId() == null)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id, Authentication authentication) {
        if (!allowed(authentication)) {
            return forbidden();
        }
        return financePromoterRepository.findById(id)
                .map(f -> ResponseEntity.ok((Object) f))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody FinancePromoter financePromoter, Authentication authentication) {
        if (!allowed(authentication)) {
            return forbidden();
        }
        financePromoter.setId(null);
        return ResponseEntity.ok(financePromoterRepository.save(financePromoter));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody FinancePromoter financePromoter, Authentication authentication) {
        if (!allowed(authentication)) {
            return forbidden();
        }
        if (!financePromoterRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        financePromoter.setId(id);
        return ResponseEntity.ok(financePromoterRepository.save(financePromoter));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id, Authentication authentication) {
        if (!allowed(authentication)) {
            return forbidden();
        }
        if (!financePromoterRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        financePromoterRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private boolean allowed(Authentication authentication) {
        return accessControl.isRh(authentication)
                || accessControl.isFinance(authentication)
                || accessControl.isAdmin(authentication);
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar lançamentos financeiros."));
    }
}
