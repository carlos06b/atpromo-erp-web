package com.atpromo.systematpromo.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ACHADO C6 (crítico): comprova o mecanismo de ticket de confirmação de
 * senha que agora liga /account/verify-password ao DELETE real.
 */
class DeleteConfirmationServiceTest {

    @Test
    void ticketValido_doMesmoEmail_eAceito() {
        DeleteConfirmationService service = new DeleteConfirmationService();
        String ticket = service.issueTicket("user@atpromo.com");

        assertNotNull(ticket);
        assertTrue(service.consumeTicket(ticket, "user@atpromo.com"));
    }

    @Test
    void ticket_podeSerUsadoVariasVezesDentroDaValidade_paraSuportarExclusaoEmLote() {
        // Decisão de design (ver Javadoc da classe): o ticket vale pra N
        // deletes dentro da janela de validade, não só um — por causa do
        // fluxo de exclusão em lote de Descritivos (confirmBulkDelete).
        DeleteConfirmationService service = new DeleteConfirmationService();
        String ticket = service.issueTicket("user@atpromo.com");

        assertTrue(service.consumeTicket(ticket, "user@atpromo.com"), "primeiro uso deve funcionar");
        assertTrue(service.consumeTicket(ticket, "user@atpromo.com"),
                "segundo uso do mesmo ticket, ainda dentro da validade, também deve funcionar");
    }

    @Test
    void ticketEmitidoParaOutroEmail_naoPodeSerUsadoPorUsuarioDiferente() {
        DeleteConfirmationService service = new DeleteConfirmationService();
        String ticket = service.issueTicket("vitima@atpromo.com");

        assertFalse(service.consumeTicket(ticket, "atacante@atpromo.com"),
                "um ticket emitido para outro usuário não pode ser reaproveitado");
    }

    @Test
    void ticketInexistenteOuNulo_eRejeitado() {
        DeleteConfirmationService service = new DeleteConfirmationService();

        assertFalse(service.consumeTicket("ticket-que-nao-existe", "user@atpromo.com"));
        assertFalse(service.consumeTicket(null, "user@atpromo.com"));
        assertFalse(service.consumeTicket("", "user@atpromo.com"));
    }

    @Test
    void ticketExpirado_eRejeitado() throws InterruptedException {
        // TTL de 20ms só para este teste (construtor package-private) — evita
        // ter que esperar os 2 minutos reais do TTL de produção.
        DeleteConfirmationService service = new DeleteConfirmationService(20);
        String ticket = service.issueTicket("user@atpromo.com");

        Thread.sleep(50);

        assertFalse(service.consumeTicket(ticket, "user@atpromo.com"), "ticket expirado deve ser rejeitado");
    }
}
