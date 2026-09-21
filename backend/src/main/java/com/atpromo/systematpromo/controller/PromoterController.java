package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Promoter;
import com.atpromo.systematpromo.repository.PromoterRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    public record PromoterView(
            Integer id, String name, String cpf, String phone, String uf, String city,
            LocalDate dateBirth, boolean active, BigDecimal salary, String type,
            String pix, String pixType, String companyLink, Integer lojaId,
            LocalDate admissionDate, LocalDate terminationDate
    ) {}

    @GetMapping
    public List<PromoterView> listAll(Authentication authentication) {
        boolean hideSalary = accessControl.isSupervisor(authentication);
        return promoterRepository.findAll().stream().map(p -> toView(p, hideSalary)).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromoterView> getById(@PathVariable int id, Authentication authentication) {
        boolean hideSalary = accessControl.isSupervisor(authentication);
        return promoterRepository.findById(id)
                .map(p -> ResponseEntity.ok(toView(p, hideSalary)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Promoter promoter, Authentication authentication) {
        if (!canWrite(authentication)) {
            return forbidden();
        }
        promoter.setId(null);
        return ResponseEntity.ok(promoterRepository.save(promoter));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Promoter promoter, Authentication authentication) {
        if (!canWrite(authentication)) {
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
        if (!canWrite(authentication)) {
            return forbidden();
        }
        if (!promoterRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        promoterRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private boolean canWrite(Authentication authentication) {
        return !accessControl.isFinance(authentication) && !accessControl.isSupervisor(authentication);
    }

    private PromoterView toView(Promoter p, boolean hideSalary) {
        return new PromoterView(
                p.getId(), p.getName(), p.getCpf(), p.getPhone(), p.getUf(), p.getCity(),
                p.getDateBirth(), p.isActive(), hideSalary ? null : p.getSalary(), p.getType(),
                p.getPix(), p.getPixType(), p.getCompanyLink(), p.getLojaId(),
                p.getAdmissionDate(), p.getTerminationDate()
        );
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para alterar promotores."));
    }
}