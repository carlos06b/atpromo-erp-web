package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.CentroCusto;
import com.atpromo.systematpromo.repository.CentroCustoRepository;
import com.atpromo.systematpromo.repository.LancamentoExtratoRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/centros-custo")
public class CentroCustoController {

    private final CentroCustoRepository centroCustoRepository;
    private final LancamentoExtratoRepository lancamentoExtratoRepository;
    private final AccessControl accessControl;

    public CentroCustoController(CentroCustoRepository centroCustoRepository,
                                  LancamentoExtratoRepository lancamentoExtratoRepository,
                                  AccessControl accessControl) {
        this.centroCustoRepository = centroCustoRepository;
        this.lancamentoExtratoRepository = lancamentoExtratoRepository;
        this.accessControl = accessControl;
    }

    public record CreateCentroCustoBody(String nome, Boolean ativo) {}

    @GetMapping
    public ResponseEntity<?> listAll(Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        List<CentroCusto> centrosCusto = centroCustoRepository.findAll();
        centrosCusto.sort(Comparator.comparing(CentroCusto::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return ResponseEntity.ok(centrosCusto);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateCentroCustoBody body, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        if (body.nome() == null || body.nome().isBlank()) {
            return badRequest("Informe o nome do centro de custo.");
        }

        CentroCusto centroCusto = new CentroCusto();
        centroCusto.setNome(body.nome().trim());
        centroCusto.setAtivo(body.ativo() == null || body.ativo());

        return ResponseEntity.status(201).body(centroCustoRepository.save(centroCusto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody CreateCentroCustoBody body, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        CentroCusto centroCusto = centroCustoRepository.findById(id).orElse(null);
        if (centroCusto == null) {
            return ResponseEntity.notFound().build();
        }
        if (body.nome() == null || body.nome().isBlank()) {
            return badRequest("Informe o nome do centro de custo.");
        }

        centroCusto.setNome(body.nome().trim());
        if (body.ativo() != null) {
            centroCusto.setAtivo(body.ativo());
        }

        return ResponseEntity.ok(centroCustoRepository.save(centroCusto));
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
        if (!centroCustoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (lancamentoExtratoRepository.existsByCentroCustoId(id)) {
            return badRequest("Esse centro de custo já tem lançamentos vinculados — arquive em vez de excluir.");
        }

        centroCustoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para gerenciar centros de custo."));
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
