package com.atpromo.systematpromo.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ACHADO C6 (crítico) da auditoria de 05/10/2026: a "confirmação por senha"
 * ao excluir registros (ConfirmDeleteDialog no frontend) era só UX —
 * /account/verify-password e o DELETE real eram duas chamadas HTTP
 * completamente desconectadas, sem nenhum vínculo entre elas. Qualquer
 * requisição DELETE direta (com um JWT válido e o papel certo) já apagava o
 * registro, sem nunca passar pela verificação de senha.
 *
 * Esta classe emite, após a senha ser confirmada em AccountController, um
 * ticket de curta duração (2 minutos), vinculado ao e-mail do usuário
 * autenticado. DeleteConfirmationFilter exige esse ticket em qualquer
 * requisição DELETE de /api/**.
 *
 * DECISÃO DE DESIGN: o ticket vale pra VÁRIOS deletes dentro da janela de
 * 2 minutos, não só um — porque "Descritivos" tem uma exclusão em lote
 * (confirmBulkDelete, N chamadas DELETE sequenciais a partir de uma única
 * confirmação de senha) que quebraria com um ticket de uso único. Trocar
 * pra "uso único" mais tarde exigiria trocar esse fluxo de lote pra um
 * endpoint de exclusão em lote só no backend (uma chamada só) — fica
 * registrado aqui como possível melhoria futura, não implementada agora
 * por não ser um dos achados do relatório.
 *
 * Em memória, como LoginAttemptService/PasswordResetLimiter já existentes no
 * projeto — suficiente para uma única instância do backend; se o backend
 * rodar com múltiplas instâncias atrás de um load balancer, isso precisa
 * virar um cache compartilhado (Redis, etc.) para continuar funcionando
 * corretamente entre instâncias diferentes.
 */
@Component
public class DeleteConfirmationService {

    private static final long DEFAULT_TICKET_TTL_MS = 2 * 60 * 1000; // 2 minutos

    private final ConcurrentHashMap<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final long ticketTtlMs;

    public DeleteConfirmationService() {
        this(DEFAULT_TICKET_TTL_MS);
    }

    // construtor visível só para testes poderem usar um TTL curto e
    // verificar expiração sem precisar esperar 2 minutos de verdade.
    DeleteConfirmationService(long ticketTtlMs) {
        this.ticketTtlMs = ticketTtlMs;
    }

    public String issueTicket(String email) {
        String ticketId = UUID.randomUUID().toString();
        tickets.put(ticketId, new Ticket(normalize(email), Instant.now().plusMillis(ticketTtlMs)));
        return ticketId;
    }

    /**
     * Valida o ticket pra este e-mail. Não remove o ticket (ver DECISÃO DE
     * DESIGN na classe) — ele continua válido pra qualquer outro DELETE do
     * mesmo usuário até expirar, o que é o que permite uma exclusão em lote
     * (N chamadas DELETE) a partir de uma única confirmação de senha.
     */
    public boolean consumeTicket(String ticketId, String email) {
        if (ticketId == null || ticketId.isBlank()) {
            return false;
        }
        Ticket ticket = tickets.get(ticketId);
        if (ticket == null) {
            return false;
        }
        if (Instant.now().isAfter(ticket.expiresAt())) {
            tickets.remove(ticketId);
            return false;
        }
        return ticket.email().equals(normalize(email));
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private record Ticket(String email, Instant expiresAt) {
    }
}
