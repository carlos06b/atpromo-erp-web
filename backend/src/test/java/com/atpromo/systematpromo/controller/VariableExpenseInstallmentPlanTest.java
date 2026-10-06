package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.model.VariableExpense;
import com.atpromo.systematpromo.repository.UserRepository;
import com.atpromo.systematpromo.repository.VariableExpenseRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ACHADO H6 (CORRIGIDO): o parcelamento de despesa variável era criado pelo
 * frontend com N chamadas POST sequenciais e independentes (uma por
 * parcela) em Despesas.jsx. Se a parcela K de N falhasse, as K-1 anteriores
 * já ficavam salvas, e tentar salvar de novo gerava um novo
 * installmentGroup duplicando as que já existiam.
 *
 * A correção move a criação para um único endpoint, POST
 * /api/variable-expenses/installment-plan, marcado @Transactional: todas as
 * parcelas são salvas numa única transação no banco, então uma falha no
 * meio reverte TODAS as parcelas dessa tentativa, nunca deixando um estado
 * parcial.
 *
 * Limitação deste teste (igual à documentada para H1): como o controller é
 * instanciado diretamente aqui (sem o proxy transacional real do Spring), o
 * rollback em si do @Transactional não é exercitado por um teste de
 * unidade — ele depende do Spring de verdade, então só pode ser confirmado
 * com um teste de integração (ex. com Testcontainers) ou manualmente após o
 * deploy. Este teste cobre o que é verificável sem o container: controle de
 * acesso, validação de entrada, e que o conteúdo de cada parcela criada é
 * exatamente o esperado.
 */
class VariableExpenseInstallmentPlanTest {

    private VariableExpenseRepository variableExpenseRepository;
    private UserRepository userRepository;
    private VariableExpenseController controller;

    @BeforeEach
    void setUp() {
        variableExpenseRepository = mock(VariableExpenseRepository.class);
        userRepository = mock(UserRepository.class);
        AccessControl accessControl = new AccessControl(userRepository);
        controller = new VariableExpenseController(variableExpenseRepository, accessControl);

        when(variableExpenseRepository.save(any(VariableExpense.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Authentication authFor(String email, String jobTittle) {
        User user = new User(1, email, jobTittle, "Usuário Teste", "hash");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        return new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
    }

    private VariableExpenseController.InstallmentPlanRequest validRequest() {
        return new VariableExpenseController.InstallmentPlanRequest(
                "Notebook",
                "Compra para o escritório",
                List.of(new BigDecimal("333.34"), new BigDecimal("333.33"), new BigDecimal("333.33")),
                List.of("2026-01-10", "2026-02-10", "2026-03-10"),
                "parc-teste"
        );
    }

    @Test
    void supervisorNaoConsegueCriarParcelamento() {
        Authentication auth = authFor("supervisor@atpromo.com", "SUPERVISOR");
        ResponseEntity<?> resposta = controller.createInstallmentPlan(validRequest(), auth);
        assertEquals(403, resposta.getStatusCode().value());
    }

    @Test
    void financeiroConsegueCriarParcelamento() {
        Authentication auth = authFor("financeiro@atpromo.com", "FINANCEIRO");
        ResponseEntity<?> resposta = controller.createInstallmentPlan(validRequest(), auth);
        assertEquals(201, resposta.getStatusCode().value());
    }

    @Test
    void menosDeDuasParcelas_retorna400() {
        Authentication auth = authFor("financeiro@atpromo.com", "FINANCEIRO");
        VariableExpenseController.InstallmentPlanRequest request = new VariableExpenseController.InstallmentPlanRequest(
                "Notebook", null, List.of(new BigDecimal("100.00")), List.of("2026-01-10"), "parc-1"
        );

        ResponseEntity<?> resposta = controller.createInstallmentPlan(request, auth);

        assertEquals(400, resposta.getStatusCode().value());
        verify(variableExpenseRepository, times(0)).save(any(VariableExpense.class));
    }

    @Test
    void quantidadeDeValoresDiferenteDeQuantidadeDeDatas_retorna400() {
        Authentication auth = authFor("financeiro@atpromo.com", "FINANCEIRO");
        VariableExpenseController.InstallmentPlanRequest request = new VariableExpenseController.InstallmentPlanRequest(
                "Notebook", null,
                List.of(new BigDecimal("100.00"), new BigDecimal("100.00")),
                List.of("2026-01-10"),
                "parc-1"
        );

        ResponseEntity<?> resposta = controller.createInstallmentPlan(request, auth);

        assertEquals(400, resposta.getStatusCode().value());
        verify(variableExpenseRepository, times(0)).save(any(VariableExpense.class));
    }

    @Test
    void parcelamentoValido_criaUmaParcelaPorValorComOsCamposCorretos() {
        Authentication auth = authFor("financeiro@atpromo.com", "FINANCEIRO");

        ResponseEntity<?> resposta = controller.createInstallmentPlan(validRequest(), auth);

        assertEquals(201, resposta.getStatusCode().value());
        verify(variableExpenseRepository, times(3)).save(any(VariableExpense.class));

        @SuppressWarnings("unchecked")
        List<VariableExpense> criadas = (List<VariableExpense>) resposta.getBody();
        assertEquals(3, criadas.size());

        for (int i = 0; i < 3; i++) {
            VariableExpense parcela = criadas.get(i);
            assertEquals("parc-teste", parcela.getInstallmentGroup());
            assertEquals(i + 1, parcela.getInstallmentNumber());
            assertEquals(3, parcela.getTotalInstallments());
            assertEquals("Notebook", parcela.getName());
            assertEquals(false, parcela.isStatus());
        }
    }

    @Test
    void semInstallmentGroupInformado_geraUmAutomaticamente() {
        Authentication auth = authFor("financeiro@atpromo.com", "FINANCEIRO");
        VariableExpenseController.InstallmentPlanRequest request = new VariableExpenseController.InstallmentPlanRequest(
                "Notebook", null,
                List.of(new BigDecimal("50.00"), new BigDecimal("50.00")),
                List.of("2026-01-10", "2026-02-10"),
                null
        );

        ResponseEntity<?> resposta = controller.createInstallmentPlan(request, auth);

        @SuppressWarnings("unchecked")
        List<VariableExpense> criadas = (List<VariableExpense>) resposta.getBody();
        assertEquals(criadas.get(0).getInstallmentGroup(), criadas.get(1).getInstallmentGroup(),
                "Mesmo sem installmentGroup informado, todas as parcelas da mesma chamada devem compartilhar o mesmo grupo gerado");
    }
}
