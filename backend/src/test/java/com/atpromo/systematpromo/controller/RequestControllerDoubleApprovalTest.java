package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.FinancePromoter;
import com.atpromo.systematpromo.model.Promoter;
import com.atpromo.systematpromo.model.Request;
import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.repository.FinancePromoterRepository;
import com.atpromo.systematpromo.repository.LancamentoExtratoRepository;
import com.atpromo.systematpromo.repository.PromoterRepository;
import com.atpromo.systematpromo.repository.RequestRepository;
import com.atpromo.systematpromo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ACHADO H1 (CORRIGIDO): RequestController.approve() fazia um "check-then-act"
 * sem lock nem @Transactional (findById -> verifica status PENDENTE em
 * memória -> salva APROVADO -> cria FinancePromoter). Duas requisições
 * PUT /approve quase simultâneas para a mesma solicitação podiam ambas ler
 * "PENDENTE" antes de qualquer uma commitar, e as duas criavam um
 * FinancePromoter — ou seja, duas ordens de pagamento para uma única
 * solicitação aprovada uma vez.
 *
 * A correção substitui o check-then-act por um UPDATE condicional atômico
 * (RequestRepository.updateStatusIfPending), que só afeta a linha "no
 * momento exato" em que o banco a executa, com status ainda PENDENTE. Sob
 * concorrência real, o próprio lock de linha do UPDATE garante que, de duas
 * transações simultâneas, no máximo uma recebe "1 linha afetada" — a outra
 * recebe 0 e é tratada como "já processada" (409).
 *
 * Este teste modela esse comportamento atômico do banco com um mock de
 * estado compartilhado: a primeira chamada a updateStatusIfPending "ganha"
 * (retorna 1 e consome o estado PENDENTE), a segunda chamada já não encontra
 * mais PENDENTE e retorna 0 — exatamente o que duas transações reais
 * concorrentes veriam ao serializar no lock da mesma linha.
 */
class RequestControllerDoubleApprovalTest {

    private RequestRepository requestRepository;
    private PromoterRepository promoterRepository;
    private UserRepository userRepository;
    private FinancePromoterRepository financePromoterRepository;
    private LancamentoExtratoRepository lancamentoExtratoRepository;
    private RequestController controller;

    private static final int REQUEST_ID = 42;

    @BeforeEach
    void setUp() {
        requestRepository = mock(RequestRepository.class);
        promoterRepository = mock(PromoterRepository.class);
        userRepository = mock(UserRepository.class);
        financePromoterRepository = mock(FinancePromoterRepository.class);
        lancamentoExtratoRepository = mock(LancamentoExtratoRepository.class);

        controller = new RequestController(
                requestRepository, promoterRepository, userRepository,
                financePromoterRepository, lancamentoExtratoRepository
        );

        User financeiro = new User(1, "financeiro@atpromo.com", "FINANCEIRO", "Usuária Financeiro", "hash");
        when(userRepository.findByEmail("financeiro@atpromo.com")).thenReturn(Optional.of(financeiro));

        Promoter promoter = new Promoter();
        promoter.setId(7);
        promoter.setName("Promotor Teste");
        when(promoterRepository.findById(7)).thenReturn(Optional.of(promoter));

        when(requestRepository.findById(REQUEST_ID)).thenAnswer(invocation -> Optional.of(
                new Request(REQUEST_ID, 1, null, 7, "BONIFICACAO", BigDecimal.valueOf(500), "msg", "PENDENTE", LocalDateTime.now())
        ));
        when(financePromoterRepository.save(any(FinancePromoter.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Authentication financeAuth() {
        return new UsernamePasswordAuthenticationToken("financeiro@atpromo.com", null, Collections.emptyList());
    }

    /**
     * Estado em memória que representa "o que o banco faria": um UPDATE
     * condicional só pode consumir o status PENDENTE uma única vez, não
     * importa quantas chamadas concorrentes cheguem.
     */
    private void mockUpdateAtomicoComoOBancoFaria() {
        boolean[] aindaPendente = {true};
        when(requestRepository.updateStatusIfPending(eq(REQUEST_ID), eq("APROVADO"), anyInt()))
                .thenAnswer(invocation -> {
                    synchronized (aindaPendente) {
                        if (aindaPendente[0]) {
                            aindaPendente[0] = false;
                            return 1;
                        }
                        return 0;
                    }
                });
    }

    @Test
    void duasAprovacoesQuaseSimultaneas_apenasUmaCriaPagamento_gracasAoUpdateAtomico() {
        mockUpdateAtomicoComoOBancoFaria();
        Authentication auth = financeAuth();

        ResponseEntity<?> primeiraResposta = controller.approve(REQUEST_ID, auth);
        ResponseEntity<?> segundaResposta = controller.approve(REQUEST_ID, auth);

        assertEquals(200, primeiraResposta.getStatusCode().value());
        assertEquals(409, segundaResposta.getStatusCode().value(),
                "CORRIGIDO: a segunda aprovação quase-simultânea deve ser rejeitada com 409, "
                        + "pois o UPDATE atômico já não encontra mais a solicitação PENDENTE");

        verify(financePromoterRepository, times(1)).save(any(FinancePromoter.class));
    }

    @Test
    void aprovacaoSequencialNormal_continuaFuncionando() {
        // Contraprova: uma única aprovação normal (sem concorrência) continua
        // funcionando exatamente como antes.
        mockUpdateAtomicoComoOBancoFaria();
        Authentication auth = financeAuth();

        ResponseEntity<?> resposta = controller.approve(REQUEST_ID, auth);

        assertEquals(200, resposta.getStatusCode().value());
        verify(financePromoterRepository, times(1)).save(any(FinancePromoter.class));
    }

    @Test
    void solicitacaoJaProcessada_retorna409_semCriarNovoPagamento() {
        // Se o UPDATE atômico já retorna 0 desde a primeira chamada (porque a
        // solicitação já tinha sido aprovada/rejeitada antes), o controller
        // não deve criar nenhum FinancePromoter novo.
        when(requestRepository.updateStatusIfPending(eq(REQUEST_ID), eq("APROVADO"), anyInt()))
                .thenReturn(0);

        ResponseEntity<?> resposta = controller.approve(REQUEST_ID, financeAuth());

        assertEquals(409, resposta.getStatusCode().value());
        verify(financePromoterRepository, times(0)).save(any(FinancePromoter.class));
    }
}
