package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Promoter;
import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.repository.PromoterRepository;
import com.atpromo.systematpromo.repository.UserRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ACHADOS C2 e C5 (críticos) da auditoria de 05/10/2026, agora corrigidos:
 * - C2: PromoterController.canWrite() era outro deny-list independente do
 *   mesmo bug de AccessControl.isAdmin() — um cargo desconhecido conseguia
 *   criar/editar/excluir promotores (CPF, salário, chave Pix).
 * - C5: listAll()/getById() não tinham NENHUMA checagem — qualquer
 *   autenticado, incluindo cargo desconhecido, lia todos os promotores.
 *
 * Estes testes comprovam que, depois da correção, um cargo desconhecido não
 * consegue mais nem ler nem escrever, enquanto RH, FINANCEIRO, SUPERVISOR e
 * ADMIN continuam funcionando como antes (RH/ADMIN escrevem; os 4 leem, com
 * salário escondido pra SUPERVISOR).
 */
class PromoterControllerAccessTest {

    private PromoterRepository promoterRepository;
    private UserRepository userRepository;
    private PromoterController controller;

    @BeforeEach
    void setUp() {
        promoterRepository = mock(PromoterRepository.class);
        userRepository = mock(UserRepository.class);
        AccessControl accessControl = new AccessControl(userRepository);
        controller = new PromoterController(promoterRepository, accessControl);
    }

    private Authentication authFor(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
    }

    private void comoUsuario(String email, String jobTittle) {
        User user = new User(5, email, jobTittle, "Fulano", "hash");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    }

    @Test
    void usuarioComCargoDesconhecido_naoConsegueMaisCriarNemExcluirPromotor() {
        comoUsuario("estoquista@atpromo.com", "ESTOQUISTA");
        Authentication auth = authFor("estoquista@atpromo.com");

        ResponseEntity<?> criar = controller.create(new Promoter(), auth);
        ResponseEntity<?> excluir = controller.delete(10, auth);

        assertEquals(403, criar.getStatusCode().value(),
                "CORRIGIDO: cargo 'ESTOQUISTA' não deve mais conseguir criar promotor");
        assertEquals(403, excluir.getStatusCode().value(),
                "CORRIGIDO: cargo 'ESTOQUISTA' não deve mais conseguir excluir promotor");
    }

    @Test
    void usuarioComCargoDesconhecido_naoConsegueMaisLerPromotores() {
        comoUsuario("estoquista@atpromo.com", "ESTOQUISTA");
        Authentication auth = authFor("estoquista@atpromo.com");

        ResponseEntity<?> resposta = controller.listAll(auth);

        assertEquals(403, resposta.getStatusCode().value(),
                "CORRIGIDO (achado C5): cargo desconhecido não deve mais conseguir listar promotores");
    }

    @Test
    void rh_continuaConseguindoCriarPromotor_comportamentoPreservado() {
        comoUsuario("rh@atpromo.com", "RH");
        when(promoterRepository.save(any(Promoter.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> resposta = controller.create(new Promoter(), authFor("rh@atpromo.com"));

        assertEquals(200, resposta.getStatusCode().value());
    }

    @Test
    void financeiro_naoConsegueCriarPromotor_comportamentoPreservado() {
        comoUsuario("fin@atpromo.com", "FINANCEIRO");

        ResponseEntity<?> resposta = controller.create(new Promoter(), authFor("fin@atpromo.com"));

        assertEquals(403, resposta.getStatusCode().value());
    }

    @Test
    void financeiro_continuaConseguindoLerPromotores_comportamentoPreservado() {
        // FINANCEIRO não escreve, mas precisa continuar lendo (usado na tela
        // de Folha de Pagamento) — ver uso real confirmado no frontend.
        comoUsuario("fin@atpromo.com", "FINANCEIRO");
        when(promoterRepository.findAll()).thenReturn(List.of());

        ResponseEntity<?> resposta = controller.listAll(authFor("fin@atpromo.com"));

        assertEquals(200, resposta.getStatusCode().value());
    }
}
