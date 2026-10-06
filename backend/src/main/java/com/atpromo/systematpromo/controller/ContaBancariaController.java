package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.ContaBancaria;
import com.atpromo.systematpromo.model.Empresa;
import com.atpromo.systematpromo.model.LancamentoExtrato;
import com.atpromo.systematpromo.repository.ContaBancariaRepository;
import com.atpromo.systematpromo.repository.EmpresaRepository;
import com.atpromo.systematpromo.repository.LancamentoExtratoRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contas-bancarias")
public class ContaBancariaController {

    private final ContaBancariaRepository contaBancariaRepository;
    private final EmpresaRepository empresaRepository;
    private final LancamentoExtratoRepository lancamentoExtratoRepository;
    private final AccessControl accessControl;

    public ContaBancariaController(ContaBancariaRepository contaBancariaRepository,
                                   EmpresaRepository empresaRepository,
                                   LancamentoExtratoRepository lancamentoExtratoRepository,
                                   AccessControl accessControl) {
        this.contaBancariaRepository = contaBancariaRepository;
        this.empresaRepository = empresaRepository;
        this.lancamentoExtratoRepository = lancamentoExtratoRepository;
        this.accessControl = accessControl;
    }

    public record ContaBancariaView(
            Integer id,
            Integer empresaId,
            String empresaNome,
            String banco,
            String apelido,
            String agencia,
            String numeroConta,
            BigDecimal saldoInicial,
            String dataSaldoInicial,
            boolean ativa,
            BigDecimal saldoAtual
    ) {}

    @GetMapping
    public ResponseEntity<?> listAll(Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        return ResponseEntity.ok(mapAll(contaBancariaRepository.findAll()));
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
        return contaBancariaRepository.findById(id)
                .<ResponseEntity<?>>map(c -> ResponseEntity.ok(mapOne(c)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ContaBancaria conta, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        String validationError = validate(conta);
        if (validationError != null) {
            return badRequest(validationError);
        }
        conta.setId(null);
        conta.setAtiva(true);
        return ResponseEntity.ok(mapOne(contaBancariaRepository.save(conta)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody ContaBancaria conta, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        if (!contaBancariaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        String validationError = validate(conta);
        if (validationError != null) {
            return badRequest(validationError);
        }
        conta.setId(id);
        return ResponseEntity.ok(mapOne(contaBancariaRepository.save(conta)));
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
        if (!contaBancariaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (lancamentoExtratoRepository.existsByContaBancariaId(id)) {
            return badRequest("Essa conta já tem lançamentos no extrato e não pode ser excluída. Desative-a em vez de excluir, para preservar o histórico.");
        }
        contaBancariaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String validate(ContaBancaria conta) {
        if (conta.getEmpresaId() == null || !empresaRepository.existsById(conta.getEmpresaId())) {
            return "Selecione uma empresa válida.";
        }
        if (conta.getBanco() == null || conta.getBanco().isBlank()) {
            return "Informe o banco.";
        }
        if (conta.getApelido() == null || conta.getApelido().isBlank()) {
            return "Informe um apelido para a conta.";
        }
        return null;
    }

    private List<ContaBancariaView> mapAll(List<ContaBancaria> list) {
        return list.stream().map(this::mapOne).toList();
    }

    private ContaBancariaView mapOne(ContaBancaria c) {
        Empresa empresa = empresaRepository.findById(c.getEmpresaId()).orElse(null);

        BigDecimal saldo = c.getSaldoInicial() != null ? c.getSaldoInicial() : BigDecimal.ZERO;
        for (LancamentoExtrato l : lancamentoExtratoRepository.findByContaBancariaId(c.getId())) {
            BigDecimal valor = l.getValor() != null ? l.getValor() : BigDecimal.ZERO;
            saldo = "ENTRADA".equalsIgnoreCase(l.getTipo()) ? saldo.add(valor) : saldo.subtract(valor);
        }

        return new ContaBancariaView(
                c.getId(),
                c.getEmpresaId(),
                empresa != null ? empresa.getNome() : null,
                c.getBanco(),
                c.getApelido(),
                c.getAgencia(),
                c.getNumeroConta(),
                c.getSaldoInicial(),
                c.getDataSaldoInicial() != null ? c.getDataSaldoInicial().toString() : null,
                c.isAtiva(),
                saldo
        );
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar contas bancárias."));
    }
}