package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Promoter;
import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.repository.FinancePromoterRepository;
import com.atpromo.systematpromo.repository.PromoterRepository;
import com.atpromo.systematpromo.security.AccessControl;
import com.atpromo.systematpromo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ACHADO M4 (CORRIGIDO): generatePayrollLines() validava "start não pode
 * ser depois de end" lançando um RuntimeException cru. Sem nenhum
 * @ExceptionHandler/@ControllerAdvice no projeto para essa exceção, isso
 * virava um HTTP 500 genérico (erro de servidor) para o que é, na
 * verdade, um erro de entrada do usuário (deveria ser 400).
 *
 * Isso não muda nenhuma regra de negócio: a condição inválida continua
 * sendo rejeitada exatamente da mesma forma (start depois de end nunca é
 * aceito). Só corrige o tipo de resposta HTTP e evita expor um stack
 * trace de uma RuntimeException não tratada.
 *
 * O frontend (FolhaDePagamento.jsx) já valida isso no cliente antes de
 * chamar a API, então esse caminho só é alcançado por uma chamada direta
 * à API (fora da tela) ou por uma falha nessa validação do cliente -
 * não muda nada visível pra quem usa a tela normalmente.
 */
class PayrollControllerGenerateLinesTest {

    private PromoterRepository promoterRepository;
    private FinancePromoterRepository financePromoterRepository;
    private UserRepository userRepository;
    private PayrollController controller;

    @BeforeEach
    void setUp() {
        promoterRepository = mock(PromoterRepository.class);
        financePromoterRepository = mock(FinancePromoterRepository.class);
        userRepository = mock(UserRepository.class);
        AccessControl accessControl = new AccessControl(userRepository);
        controller = new PayrollController(promoterRepository, financePromoterRepository, accessControl);

        when(promoterRepository.findAll()).thenReturn(Collections.<Promoter>emptyList());
    }

    private Authentication authFor(String email, String jobTittle) {
        User user = new User(1, email, jobTittle, "Usuário Teste", "hash");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        return new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
    }

    @Test
    void startDepoisDeEnd_retorna400ComMensagemClara_naoLanca500() {
        Authentication auth = authFor("financeiro@atpromo.com", "FINANCEIRO");
        LocalDate start = LocalDate.of(2026, 3, 10);
        LocalDate end = LocalDate.of(2026, 3, 1);

        ResponseEntity<?> resposta = controller.generatePayrollLines(start, end, "TODOS", auth);

        assertEquals(400, resposta.getStatusCode().value());
        assertFalse(resposta.getBody() == null || resposta.getBody().toString().isBlank(),
                "A resposta de erro deveria trazer uma mensagem explicando o problema");
    }

    @Test
    void startAntesDeEnd_continuaFuncionandoNormalmente() {
        Authentication auth = authFor("financeiro@atpromo.com", "FINANCEIRO");
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        ResponseEntity<?> resposta = controller.generatePayrollLines(start, end, "TODOS", auth);

        assertEquals(200, resposta.getStatusCode().value());
    }

    @Test
    void startIgualAEnd_continuaSendoAceito() {
        Authentication auth = authFor("financeiro@atpromo.com", "FINANCEIRO");
        LocalDate dia = LocalDate.of(2026, 3, 15);

        ResponseEntity<?> resposta = controller.generatePayrollLines(dia, dia, "TODOS", auth);

        assertEquals(200, resposta.getStatusCode().value());
    }
}
