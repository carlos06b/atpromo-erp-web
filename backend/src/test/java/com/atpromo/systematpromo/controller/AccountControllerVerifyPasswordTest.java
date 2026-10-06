package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.repository.UserRepository;
import com.atpromo.systematpromo.security.DeleteConfirmationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ACHADO C6 (crítico), parte do lado de AccountController: antes,
 * verify-password só devolvia 200 vazio. Agora devolve um ticket de uso
 * único que o DELETE real precisa enviar (ver DeleteConfirmationFilter).
 */
class AccountControllerVerifyPasswordTest {

    @Test
    void senhaCorreta_devolveUmTicketDeConfirmacao() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        DeleteConfirmationService deleteConfirmationService = new DeleteConfirmationService();

        User user = new User(1, "user@atpromo.com", "RH", "Usuário", passwordEncoder.encode("SenhaForte123!"));
        when(userRepository.findByEmail("user@atpromo.com")).thenReturn(Optional.of(user));

        AccountController controller = new AccountController(userRepository, passwordEncoder, deleteConfirmationService);
        Authentication auth = new UsernamePasswordAuthenticationToken("user@atpromo.com", null, Collections.emptyList());

        ResponseEntity<?> resposta = controller.verifyPassword(
                new AccountController.VerifyPasswordRequest("SenhaForte123!"), auth);

        assertEquals(200, resposta.getStatusCode().value());
        Object body = resposta.getBody();
        assertTrue(body instanceof Map, "esperado um corpo com o ticket");
        Object ticket = ((Map<?, ?>) body).get("ticket");
        assertNotNull(ticket, "verify-password deveria devolver um ticket de confirmação");

        // o ticket emitido precisa ser consumível de verdade pelo DELETE real
        assertTrue(deleteConfirmationService.consumeTicket((String) ticket, "user@atpromo.com"));
    }

    @Test
    void senhaErrada_naoEmiteTicket() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        DeleteConfirmationService deleteConfirmationService = new DeleteConfirmationService();

        User user = new User(1, "user@atpromo.com", "RH", "Usuário", passwordEncoder.encode("SenhaForte123!"));
        when(userRepository.findByEmail("user@atpromo.com")).thenReturn(Optional.of(user));

        AccountController controller = new AccountController(userRepository, passwordEncoder, deleteConfirmationService);
        Authentication auth = new UsernamePasswordAuthenticationToken("user@atpromo.com", null, Collections.emptyList());

        ResponseEntity<?> resposta = controller.verifyPassword(
                new AccountController.VerifyPasswordRequest("senha-errada"), auth);

        assertEquals(401, resposta.getStatusCode().value());
    }
}
