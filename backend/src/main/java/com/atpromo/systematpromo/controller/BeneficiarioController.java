package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Beneficiario;
import com.atpromo.systematpromo.repository.BeneficiarioRepository;
import com.atpromo.systematpromo.repository.LancamentoExtratoRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/beneficiarios")
public class BeneficiarioController {

    private final BeneficiarioRepository beneficiarioRepository;
    private final LancamentoExtratoRepository lancamentoExtratoRepository;
    private final AccessControl accessControl;

    public BeneficiarioController(BeneficiarioRepository beneficiarioRepository,
                                   LancamentoExtratoRepository lancamentoExtratoRepository,
                                   AccessControl accessControl) {
        this.beneficiarioRepository = beneficiarioRepository;
        this.lancamentoExtratoRepository = lancamentoExtratoRepository;
        this.accessControl = accessControl;
    }

    public record CreateBeneficiarioBody(String nome, Boolean ativo) {}

    @GetMapping
    public ResponseEntity<?> listAll(Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        List<Beneficiario> beneficiarios = beneficiarioRepository.findAll();
        beneficiarios.sort(Comparator.comparing(Beneficiario::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return ResponseEntity.ok(beneficiarios);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateBeneficiarioBody body, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        if (body.nome() == null || body.nome().isBlank()) {
            return badRequest("Informe o nome do beneficiário.");
        }

        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setNome(body.nome().trim());
        beneficiario.setAtivo(body.ativo() == null || body.ativo());

        return ResponseEntity.status(201).body(beneficiarioRepository.save(beneficiario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody CreateBeneficiarioBody body, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        Beneficiario beneficiario = beneficiarioRepository.findById(id).orElse(null);
        if (beneficiario == null) {
            return ResponseEntity.notFound().build();
        }
        if (body.nome() == null || body.nome().isBlank()) {
            return badRequest("Informe o nome do beneficiário.");
        }

        beneficiario.setNome(body.nome().trim());
        if (body.ativo() != null) {
            beneficiario.setAtivo(body.ativo());
        }

        return ResponseEntity.ok(beneficiarioRepository.save(beneficiario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        if (!beneficiarioRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (lancamentoExtratoRepository.existsByBeneficiarioId(id)) {
            return badRequest("Esse beneficiário já tem lançamentos vinculados — arquive em vez de excluir.");
        }

        beneficiarioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para gerenciar beneficiários."));
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
