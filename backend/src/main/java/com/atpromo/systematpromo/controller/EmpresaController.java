package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Empresa;
import com.atpromo.systematpromo.repository.ContaBancariaRepository;
import com.atpromo.systematpromo.repository.EmpresaRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaRepository empresaRepository;
    private final ContaBancariaRepository contaBancariaRepository;
    private final AccessControl accessControl;

    public EmpresaController(EmpresaRepository empresaRepository,
                             ContaBancariaRepository contaBancariaRepository,
                             AccessControl accessControl) {
        this.empresaRepository = empresaRepository;
        this.contaBancariaRepository = contaBancariaRepository;
        this.accessControl = accessControl;
    }

    @GetMapping
    public ResponseEntity<?> listAll(Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        return ResponseEntity.ok(empresaRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        return empresaRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Empresa empresa, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        if (empresa.getNome() == null || empresa.getNome().isBlank()) {
            return badRequest("Informe o nome da empresa.");
        }
        empresa.setId(null);
        empresa.setAtiva(true);
        return ResponseEntity.ok(empresaRepository.save(empresa));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Empresa empresa, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        if (!empresaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (empresa.getNome() == null || empresa.getNome().isBlank()) {
            return badRequest("Informe o nome da empresa.");
        }
        empresa.setId(id);
        return ResponseEntity.ok(empresaRepository.save(empresa));
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
        if (!empresaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (!contaBancariaRepository.findByEmpresaId(id).isEmpty()) {
            return badRequest("Essa empresa tem contas bancárias cadastradas e não pode ser excluída. Desative-a em vez de excluir, para preservar o histórico.");
        }
        empresaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar empresas."));
    }
}