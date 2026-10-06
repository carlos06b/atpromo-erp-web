package com.atpromo.systematpromo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ACHADO C6 (crítico): exige um ticket de confirmação de senha (emitido por
 * AccountController.verifyPassword(), ver DeleteConfirmationService) em toda
 * requisição DELETE de /api/**. Roda depois de JwtAuthFilter (precisa do
 * usuário autenticado já resolvido) e antes de qualquer controller.
 *
 * Confirmado contra o uso real do frontend: todo DELETE disparado pela
 * interface hoje já passa por ConfirmDeleteDialog (11 páginas, ~17 pontos
 * de exclusão) — ou seja, nenhum fluxo legítimo deixa de enviar o header.
 */
@Component
public class DeleteConfirmationFilter extends OncePerRequestFilter {

    public static final String TICKET_HEADER = "X-Delete-Confirmation";

    private final DeleteConfirmationService deleteConfirmationService;

    public DeleteConfirmationFilter(DeleteConfirmationService deleteConfirmationService) {
        this.deleteConfirmationService = deleteConfirmationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        boolean isDelete = HttpMethod.DELETE.matches(request.getMethod());
        boolean isApi = request.getRequestURI() != null && request.getRequestURI().startsWith("/api/");

        if (!isDelete || !isApi) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication != null ? authentication.getName() : null;
        String ticket = request.getHeader(TICKET_HEADER);

        if (email == null || !deleteConfirmationService.consumeTicket(ticket, email)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"message\":\"Confirme sua senha antes de excluir (ticket de confirmação ausente, inválido ou expirado).\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
