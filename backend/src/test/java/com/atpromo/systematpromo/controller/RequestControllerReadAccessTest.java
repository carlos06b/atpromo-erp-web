package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.repository.FinancePromoterRepository;
import com.atpromo.systematpromo.repository.LancamentoExtratoRepository;
import com.atpromo.systematpromo.repository.PromoterRepository;
import com.atpromo.systematpromo.repository.RequestRepository;
import com.atpromo.systematpromo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ACHADO C4 (crítico) da auditoria de 05/10/2026, agora corrigido:
 * listAll()/listPending()/listByPeriod()/getById()/getTypes() de
 * RequestController não tinham NENHUMA checagem de papel — qualquer
 * autenticado via chave Pix, valor e mensagem de toda solicitação da
 * empresa.
 *
 * Estes testes comprovam a correção: RH, FINANCEIRO e ADMIN continuam
 * acessando (único uso real no frontend é a tela de Solicitações, que é
 * RH/FINANCEIRO/ADMIN); qualquer outro papel recebe 403.
 */
class RequestControllerReadAccessTest {

    private RequestRepository requestRepository;
    private UserRepository userRepository;
    private RequestController controller;

    @BeforeEach
    void setUp() {
        requestRepository = mock(RequestRepository.class);
        userRepository = mock(UserRepository.class);
        PromoterRepository promoterRepository = mock(PromoterRepository.class);
        FinancePromoterRepository financePromoterRepository = mock(FinancePromoterRepository.class);
        LancamentoExtratoRepository lancamentoExtratoRepository = mock(LancamentoExtratoRepository.class);

        controller = new RequestController(
                requestRepository, promoterRepository, userRepository,
                financePromoterRepository, lancamentoExtratoRepository
        );
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
    void papelNaoAutorizado_recebe403EmTodasAsLeituras(String jobTittleNaoAutorizado) {
        comoUsuario("usuario@atpromo.com", jobTittleNaoAutorizado);
        Authentication auth = authFor("usuario@atpromo.com");

        assertEquals(403, controller.listAll(auth).getStatusCode().value());
        assertEquals(403, controller.listPending(auth).getStatusCode().value());
        assertEquals(403, controller.getById(1, auth).getStatusCode().value());
        assertEquals(403, controller.getTypes(auth).getStatusCode().value());
        assertEquals(403,
                controller.listByPeriod(LocalDate.now().minusDays(1), LocalDate.now(), auth).getStatusCode().value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RH", "FINANCEIRO", "ADMIN"})
    void papelAutorizado_continuaLendo_comportamentoPreservado(String jobTittleAutorizado) {
        comoUsuario("usuario@atpromo.com", jobTittleAutorizado);
        Authentication auth = authFor("usuario@atpromo.com");

        when(requestRepository.findAll()).thenReturn(List.of());
        when(requestRepository.findByStatusIgnoreCase("PENDENTE")).thenReturn(List.of());

        assertEquals(200, controller.listAll(auth).getStatusCode().value());
        assertEquals(200, controller.listPending(auth).getStatusCode().value());
        assertEquals(200, controller.getTypes(auth).getStatusCode().value());
    }

    @Test
    void semAutenticacao_recebe403() {
        assertEquals(403, controller.listAll(null).getStatusCode().value());
    }
}
