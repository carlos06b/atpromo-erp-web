package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.CategoriaLancamento;
import com.atpromo.systematpromo.repository.CategoriaLancamentoRepository;
import com.atpromo.systematpromo.repository.LancamentoExtratoRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categorias-lancamento")
public class CategoriaLancamentoController {

    private final CategoriaLancamentoRepository categoriaLancamentoRepository;
    private final LancamentoExtratoRepository lancamentoExtratoRepository;
    private final AccessControl accessControl;

    public CategoriaLancamentoController(CategoriaLancamentoRepository categoriaLancamentoRepository,
                                          LancamentoExtratoRepository lancamentoExtratoRepository,
                                          AccessControl accessControl) {
        this.categoriaLancamentoRepository = categoriaLancamentoRepository;
        this.lancamentoExtratoRepository = lancamentoExtratoRepository;
        this.accessControl = accessControl;
    }

    public record CreateCategoriaBody(String nome, Boolean ativo) {}

    @GetMapping
    public ResponseEntity<?> listAll(Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        List<CategoriaLancamento> categorias = categoriaLancamentoRepository.findAll();
        categorias.sort(Comparator.comparing(CategoriaLancamento::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return ResponseEntity.ok(categorias);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateCategoriaBody body, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        if (body.nome() == null || body.nome().isBlank()) {
            return badRequest("Informe o nome da categoria.");
        }

        CategoriaLancamento categoria = new CategoriaLancamento();
        categoria.setNome(body.nome().trim());
        categoria.setAtivo(body.ativo() == null || body.ativo());

        return ResponseEntity.status(201).body(categoriaLancamentoRepository.save(categoria));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody CreateCategoriaBody body, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        CategoriaLancamento categoria = categoriaLancamentoRepository.findById(id).orElse(null);
        if (categoria == null) {
            return ResponseEntity.notFound().build();
        }
        if (body.nome() == null || body.nome().isBlank()) {
            return badRequest("Informe o nome da categoria.");
        }

        categoria.setNome(body.nome().trim());
        if (body.ativo() != null) {
            categoria.setAtivo(body.ativo());
        }

        return ResponseEntity.ok(categoriaLancamentoRepository.save(categoria));
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
        if (!categoriaLancamentoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (lancamentoExtratoRepository.existsByCategoriaId(id)) {
            return badRequest("Essa categoria já tem lançamentos vinculados — arquive em vez de excluir.");
        }

        categoriaLancamentoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para gerenciar categorias."));
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
