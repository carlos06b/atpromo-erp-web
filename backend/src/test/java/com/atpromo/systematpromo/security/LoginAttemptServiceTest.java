package com.atpromo.systematpromo.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LoginAttemptService: confirma o comportamento (5 tentativas, bloqueio por
 * e-mail + IP) e documenta a limitação relevante pro relatório — o contador
 * é só em memória (ConcurrentHashMap), então não sobrevive a um
 * restart/deploy e, com mais de uma instância rodando, cada instância tem
 * seu próprio contador (o bloqueio "dilui" em ambientes com múltiplas
 * réplicas).
 *
 * ACHADO H4 (CORRIGIDO): a chave do bloqueio era só o email, permitindo que
 * qualquer um travasse a conta de um colega de qualquer lugar, só sabendo o
 * email dele. Agora a chave é email + IP — ver o teste
 * atacanteNaoConsegueMaisBloquearContaDeOutraPessoaDeOutroIp abaixo.
 */
class LoginAttemptServiceTest {

    private static final String IP_A = "10.0.0.1";
    private static final String IP_B = "203.0.113.50";

    @Test
    void naoDeveBloquearAntesDe5Falhas() {
        LoginAttemptService service = new LoginAttemptService();
        String email = "vitima@atpromo.com";

        for (int i = 0; i < 4; i++) {
            service.registerFailure(email, IP_A);
        }

        assertFalse(service.isBlocked(email, IP_A), "Não deveria bloquear com só 4 falhas");
    }

    @Test
    void deveBloquearApos5Falhas() {
        LoginAttemptService service = new LoginAttemptService();
        String email = "vitima@atpromo.com";

        for (int i = 0; i < 5; i++) {
            service.registerFailure(email, IP_A);
        }

        assertTrue(service.isBlocked(email, IP_A), "Deveria bloquear depois da 5ª falha");
        assertTrue(service.minutesRemaining(email, IP_A) > 0);
    }

    @Test
    void loginComSucesso_deveLimparContador() {
        LoginAttemptService service = new LoginAttemptService();
        String email = "usuario@atpromo.com";

        for (int i = 0; i < 4; i++) {
            service.registerFailure(email, IP_A);
        }
        service.registerSuccess(email, IP_A);

        for (int i = 0; i < 4; i++) {
            service.registerFailure(email, IP_A);
        }

        assertFalse(service.isBlocked(email, IP_A), "O contador deveria ter zerado após o login bem-sucedido");
    }

    @Test
    void bloqueioEhPorEmailNormalizado_naoDiferenciaCaixaOuEspacos() {
        // Isso continua sendo bom (evita bypass trocando caixa) - a parte do
        // email da chave é normalizada, só a parte do IP passou a existir.
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 5; i++) {
            service.registerFailure("Vitima@AtPromo.com", IP_A);
        }

        assertTrue(service.isBlocked("  vitima@atpromo.com  ", IP_A));
    }

    @Test
    void mesmoEmail_deIpsDiferentes_temContadoresIndependentes() {
        // Efeito colateral esperado e aceitável da correção: um usuário
        // legítimo que erra a senha em uma rede e troca de rede ganha um
        // novo contador, em vez de continuar bloqueado. Isso não piora a
        // proteção contra força bruta (cada IP ainda bloqueia após 5
        // tentativas) e evita punir o usuário por trocar de rede.
        LoginAttemptService service = new LoginAttemptService();
        String email = "usuario@atpromo.com";

        for (int i = 0; i < 5; i++) {
            service.registerFailure(email, IP_A);
        }
        assertTrue(service.isBlocked(email, IP_A));

        assertFalse(service.isBlocked(email, IP_B),
                "O mesmo email a partir de outro IP não deveria estar bloqueado");
    }

    @Test
    void atacanteNaoConsegueMaisBloquearContaDeOutraPessoaDeOutroIp() {
        // CORRIGIDO: antes, registerFailure(email) não exigia nenhuma prova
        // de posse do email nem considerava a origem — um atacante sem
        // credencial alguma conseguia trancar o login de qualquer colega só
        // sabendo o email dele e errando a senha 5 vezes propositalmente,
        // de QUALQUER IP. Agora o bloqueio é por email + IP: o atacante, a
        // partir do IP dele (IP_B), consegue no máximo bloquear a
        // combinação "email da vítima + IP do atacante" — a vítima
        // continua conseguindo logar normalmente a partir do IP dela (IP_A).
        LoginAttemptService service = new LoginAttemptService();
        String emailDaVitima = "colega.de.trabalho@atpromo.com";

        for (int i = 0; i < 5; i++) {
            service.registerFailure(emailDaVitima, IP_B); // atacante, a partir do próprio IP
        }

        assertTrue(service.isBlocked(emailDaVitima, IP_B),
                "O IP do atacante deveria estar bloqueado para esse email");
        assertFalse(service.isBlocked(emailDaVitima, IP_A),
                "CORRIGIDO: a vítima, logando do IP dela, não deveria estar bloqueada "
                        + "só porque um atacante errou a senha repetidamente a partir de outro IP");
    }
}
