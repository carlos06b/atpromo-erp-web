package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.VariableExpense;
import com.atpromo.systematpromo.repository.VariableExpenseRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/variable-expenses")
public class VariableExpenseController {

    private final VariableExpenseRepository variableExpenseRepository;
    private final AccessControl accessControl;

    public VariableExpenseController(VariableExpenseRepository variableExpenseRepository, AccessControl accessControl) {
        this.variableExpenseRepository = variableExpenseRepository;
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
        return ResponseEntity.ok(variableExpenseRepository.findAll());
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
        return variableExpenseRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody VariableExpense variableExpense, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        variableExpense.setId(null);
        return ResponseEntity.ok(variableExpenseRepository.save(variableExpense));
    }

    public record InstallmentPlanRequest(
            String name,
            String description,
            List<BigDecimal> amounts,
            List<String> dates,
            String installmentGroup
    ) {}

    // ACHADO H6: o parcelamento era criado pelo frontend com N chamadas POST
    // sequenciais e independentes (uma por parcela). Se uma falhasse no meio
    // (rede, validacao, timeout), as parcelas anteriores ja ficavam salvas, e
    // tentar de novo gerava um novo installmentGroup duplicando as que ja
    // existiam. Este endpoint recebe o parcelamento completo (valores e
    // datas ja calculados pelo frontend, com a mesma logica de
    // splitAmountInInstallments/addMonthsIso de sempre - ver achado B3) e
    // grava todas as parcelas numa unica transacao: se qualquer save falhar,
    // a transacao inteira e revertida e nenhuma parcela fica salva parcial.
    @Transactional
    @PostMapping("/installment-plan")
    public ResponseEntity<?> createInstallmentPlan(@RequestBody InstallmentPlanRequest request, Authentication authentication) {
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }

        if (request.amounts() == null || request.dates() == null
                || request.amounts().size() < 2
                || request.amounts().size() != request.dates().size()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Parcelamento inválido: informe ao menos 2 parcelas, com a mesma quantidade de valores e datas."
            ));
        }

        int total = request.amounts().size();
        String groupId = request.installmentGroup() != null && !request.installmentGroup().isBlank()
                ? request.installmentGroup()
                : "parc-" + System.currentTimeMillis();

        List<VariableExpense> created = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            VariableExpense expense = new VariableExpense();
            expense.setName(request.name());
            expense.setAmount(request.amounts().get(i));
            expense.setDate(LocalDate.parse(request.dates().get(i)));
            expense.setStatus(false);
            expense.setPaymentDate(null);
            expense.setDescription(request.description());
            expense.setInstallmentGroup(groupId);
            expense.setInstallmentNumber(i + 1);
            expense.setTotalInstallments(total);
            created.add(variableExpenseRepository.save(expense));
        }

        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody VariableExpense variableExpense, Authentication authentication) {
        // ACHADO H2: antes era um deny-list de 1 papel (so bloqueava RH),
        // deixando SUPERVISOR (e qualquer cargo desconhecido) passar sem
        // checagem nenhuma. Agora e um allow-list explicito, igual ao que
        // o frontend (access.js / PAGE_ACCESS) ja decide para esta tela.
        if (!(accessControl.isFinance(authentication) || accessControl.isAdmin(authentication))) {
            return forbidden();
        }
        if (!variableExpenseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        variableExpense.setId(id);
        return ResponseEntity.ok(variableExpenseRepository.save(variableExpense));
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
        if (!variableExpenseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        variableExpenseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar despesas."));
    }
}