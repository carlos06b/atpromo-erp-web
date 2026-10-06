package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.FinancePromoter;
import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.repository.FinancePromoterRepository;
import com.atpromo.systematpromo.repository.UserRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ACHADO C3 (crítico) da auditoria de 05/10/2026, agora corrigido:
 * FinanceController (/api/finance-promoter) não tinha NENHUMA verificação de
 * perfil em nenhum dos 5 endpoints — qualquer usuário autenticado, incluindo
 * um cargo desconhecido, podia ler, criar, editar e excluir pagamentos de
 * promotor por essa rota.
 *
 * Estes testes comprovam a correção: agora só RH, FINANCEIRO ou ADMIN
 * passam (o único uso real dessa API no frontend é a tela de Folha de
 * Pagamento, que é RH/FINANCEIRO/ADMIN); qualquer outro papel recebe 403.
 */
class FinanceControllerAccessTest {

    private FinancePromoterRepository repo;
    private UserRepository userRepository;
    private FinanceController controller;

    @BeforeEach
    void setUp() {
        repo = mock(FinancePromoterRepository.class);
        userRepository = mock(UserRepository.class);
        controller = new FinanceController(repo, new AccessControl(userRepository));
    }

    private Authentication authFor(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
    }

    private void comoUsuario(String email, String jobTittle) {
        User user = new User(1, email, jobTittle, "Fulano", "hash");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"SUPERVISOR", "Estoquista"})
    void papelNaoAutorizado_recebe403EmTodosOsEndpoints(String jobTittleNaoAutorizado) {
        comoUsuario("usuario@atpromo.com", jobTittleNaoAutorizado);
        Authentication auth = authFor("usuario@atpromo.com");

        FinancePromoter qualquer = new FinancePromoter();

        assertEquals(403, controller.listAll(auth).getStatusCode().value(),
                "CORRIGIDO: listAll não deve mais estar aberto a '" + jobTittleNaoAutorizado + "'");
        assertEquals(403, controller.getById(1, auth).getStatusCode().value());
        assertEquals(403, controller.create(qualquer, auth).getStatusCode().value());
        assertEquals(403, controller.update(1, qualquer, auth).getStatusCode().value());
        assertEquals(403, controller.delete(1, auth).getStatusCode().value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RH", "FINANCEIRO", "ADMIN"})
    void papelAutorizado_continuaFuncionando_comportamentoPreservado(String jobTittleAutorizado) {
        comoUsuario("usuario@atpromo.com", jobTittleAutorizado);
        Authentication auth = authFor("usuario@atpromo.com");

        FinancePromoter fabricado = new FinancePromoter();
        fabricado.setIdPromoter(99);
        fabricado.setAmount(new BigDecimal("5000.00"));
        fabricado.setDate(LocalDate.now());
        fabricado.setStatus("PAGO");
        fabricado.setType("BONIFICACAO");
        fabricado.setDescription("Lançamento de teste");

        when(repo.save(any(FinancePromoter.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repo.findAll()).thenReturn(List.of(fabricado));

        ResponseEntity<?> criar = controller.create(fabricado, auth);
        ResponseEntity<?> listar = controller.listAll(auth);

        assertEquals(200, criar.getStatusCode().value());
        assertEquals(200, listar.getStatusCode().value());
    }
}
