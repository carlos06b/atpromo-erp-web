package com.atpromo.systematpromo.security;

import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ACHADO C2 (crítico) da auditoria de 05/10/2026: AccessControl.isAdmin()
 * era um "deny-list" (true pra qualquer jobTittle que não fosse exatamente
 * RH, FINANCEIRO ou SUPERVISOR), em vez de um "allow-list" (true só quando o
 * jobTittle é exatamente ADMIN). Qualquer cargo nulo, vazio ou desconhecido
 * virava ADMIN automaticamente — acesso total ao sistema.
 *
 * Estes testes comprovam a CORREÇÃO: isAdmin() agora nega por padrão, e só
 * concede admin pra quem tem jobTittle "ADMIN" de fato. Também cobre
 * isKnownRole(), usado como checagem mínima em endpoints de leitura
 * compartilhados entre vários papéis (achado C5).
 */
class AccessControlTest {

    private AccessControl newAccessControlFor(String email, String jobTittle) {
        UserRepository userRepository = mock(UserRepository.class);
        if (jobTittle == null && email == null) {
            when(userRepository.findByEmail(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
        } else {
            User user = new User(1, email, jobTittle, "Usuário Teste", "hash-irrelevante");
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        }
        return new AccessControl(userRepository);
    }

    private Authentication authFor(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
    }

    @Test
    void isAdmin_deveRetornarFalse_quandoJobTittleForNulo() {
        AccessControl accessControl = newAccessControlFor("sem.cargo@atpromo.com", null);
        Authentication auth = authFor("sem.cargo@atpromo.com");

        assertFalse(accessControl.isAdmin(auth),
                "CORRIGIDO: jobTittle nulo não deve mais virar ADMIN");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "Vendedor", "SUPERVISORA", "RH ", "ESTOQUE", "admin123", "Gerente"})
    void isAdmin_deveRetornarFalse_paraQualquerCargoDesconhecidoOuVazio(String jobTittleInvalido) {
        AccessControl accessControl = newAccessControlFor("cargo.estranho@atpromo.com", jobTittleInvalido);
        Authentication auth = authFor("cargo.estranho@atpromo.com");

        assertFalse(accessControl.isAdmin(auth),
                "CORRIGIDO: jobTittle='" + jobTittleInvalido + "' não deve mais virar ADMIN");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "admin", "  Admin  "})
    void isAdmin_deveRetornarTrue_apenasParaJobTittleAdminExplicito(String jobTittleAdmin) {
        AccessControl accessControl = newAccessControlFor("admin@atpromo.com", jobTittleAdmin);
        assertTrue(accessControl.isAdmin(authFor("admin@atpromo.com")));
    }

    @Test
    void isAdmin_deveRetornarFalse_paraRh() {
        AccessControl accessControl = newAccessControlFor("rh@atpromo.com", "RH");
        assertFalse(accessControl.isAdmin(authFor("rh@atpromo.com")));
    }

    @Test
    void isAdmin_deveRetornarFalse_paraFinanceiro() {
        AccessControl accessControl = newAccessControlFor("fin@atpromo.com", "Financeiro");
        assertFalse(accessControl.isAdmin(authFor("fin@atpromo.com")));
    }

    @Test
    void isAdmin_deveRetornarFalse_paraSupervisor() {
        AccessControl accessControl = newAccessControlFor("sup@atpromo.com", " supervisor ");
        assertFalse(accessControl.isAdmin(authFor("sup@atpromo.com")));
    }

    @Test
    void isRh_deveSerCaseInsensitiveETrimada() {
        AccessControl accessControl = newAccessControlFor("rh2@atpromo.com", "  rH  ");
        assertTrue(accessControl.isRh(authFor("rh2@atpromo.com")));
    }

    @Test
    void isAdmin_deveRetornarFalse_quandoUsuarioNaoExisteMais() {
        AccessControl accessControl = newAccessControlFor(null, null);
        assertFalse(accessControl.isAdmin(authFor("nao.existe@atpromo.com")));
    }

    @Test
    void currentUser_deveRetornarNull_quandoAuthenticationForNull() {
        UserRepository userRepository = mock(UserRepository.class);
        AccessControl accessControl = new AccessControl(userRepository);
        assertFalse(accessControl.isAdmin(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RH", "FINANCEIRO", "SUPERVISOR", "ADMIN"})
    void isKnownRole_deveRetornarTrue_paraOsQuatroPapeisReconhecidos(String jobTittle) {
        AccessControl accessControl = newAccessControlFor("usuario@atpromo.com", jobTittle);
        assertTrue(accessControl.isKnownRole(authFor("usuario@atpromo.com")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Estoquista", "qualquer-coisa"})
    void isKnownRole_deveRetornarFalse_paraCargoDesconhecido(String jobTittleInvalido) {
        AccessControl accessControl = newAccessControlFor("usuario@atpromo.com", jobTittleInvalido);
        assertFalse(accessControl.isKnownRole(authFor("usuario@atpromo.com")));
    }
}
