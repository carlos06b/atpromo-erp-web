package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Loja;
import com.atpromo.systematpromo.repository.LojaRepository;
import com.atpromo.systematpromo.repository.PromoterRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/lojas")
public class LojaController {

    private final LojaRepository lojaRepository;
    private final PromoterRepository promoterRepository;
    private final AccessControl accessControl;

    public LojaController(LojaRepository lojaRepository, PromoterRepository promoterRepository, AccessControl accessControl) {
        this.lojaRepository = lojaRepository;
        this.promoterRepository = promoterRepository;
        this.accessControl = accessControl;
    }

    @GetMapping
    public ResponseEntity<?> listAll(Authentication authentication) {
        if (!allowed(authentication)) {
            return forbidden();
        }
        return ResponseEntity.ok(lojaRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id, Authentication authentication) {
        if (!allowed(authentication)) {
            return forbidden();
        }
        return lojaRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Loja loja, Authentication authentication) {
        if (!allowed(authentication)) {
            return forbidden();
        }
        String validationError = validate(loja, null);
        if (validationError != null) {
            return badRequest(validationError);
        }
        loja.setId(null);
        return ResponseEntity.ok(lojaRepository.save(loja));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Loja loja, Authentication authentication) {
        if (!allowed(authentication)) {
            return forbidden();
        }
        if (!lojaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        String validationError = validate(loja, id);
        if (validationError != null) {
            return badRequest(validationError);
        }
        loja.setId(id);
        return ResponseEntity.ok(lojaRepository.save(loja));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id, Authentication authentication) {
        if (!allowed(authentication)) {
            return forbidden();
        }
        if (!lojaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (!promoterRepository.findByLojaId(id).isEmpty()) {
            return badRequest("Essa loja está vinculada a promotores e não pode ser excluída. Desative-a ou troque a loja dos promotores primeiro.");
        }
        lojaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String validate(Loja loja, Integer editingId) {
        if (loja.getNome() == null || loja.getNome().isBlank()) {
            return "Informe o nome da loja.";
        }
        if (loja.getCnpj() != null && !loja.getCnpj().isBlank()) {
            Optional<Loja> owner = lojaRepository.findAll().stream()
                    .filter(l -> loja.getCnpj().trim().equals(l.getCnpj()))
                    .findFirst();
            if (owner.isPresent() && !owner.get().getId().equals(editingId)) {
                return "Já existe uma loja cadastrada com esse CNPJ.";
            }
        }
        return null;
    }

    private boolean allowed(Authentication authentication) {
        return accessControl.currentUser(authentication) != null;
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar lojas."));
    }
}