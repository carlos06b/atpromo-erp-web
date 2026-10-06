package com.atpromo.systematpromo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ACHADO C6 (crítico): comprova que DeleteConfirmationFilter bloqueia
 * qualquer DELETE /api/** sem um ticket válido, mesmo que o usuário já
 * esteja autenticado com um papel que teria permissão no controller — ou
 * seja, agora a exclusão real está de fato ligada à confirmação de senha.
 */
class DeleteConfirmationFilterTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String email) {
        Authentication auth = new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void deleteSemTicket_eBloqueadoCom401_naoChegaNoController() throws Exception {
        autenticarComo("user@atpromo.com");
        DeleteConfirmationService service = new DeleteConfirmationService();
        DeleteConfirmationFilter filter = new DeleteConfirmationFilter(service);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        PrintWriter writer = mock(PrintWriter.class);

        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/api/promoters/10");
        when(request.getHeader(DeleteConfirmationFilter.TICKET_HEADER)).thenReturn(null);
        when(response.getWriter()).thenReturn(writer);

        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void deleteComTicketValidoDoMesmoUsuario_passaParaOController() throws Exception {
        autenticarComo("user@atpromo.com");
        DeleteConfirmationService service = new DeleteConfirmationService();
        String ticket = service.issueTicket("user@atpromo.com");
        DeleteConfirmationFilter filter = new DeleteConfirmationFilter(service);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/api/promoters/10");
        when(request.getHeader(DeleteConfirmationFilter.TICKET_HEADER)).thenReturn(ticket);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void deleteComTicketDeOutroUsuario_eBloqueado() throws Exception {
        autenticarComo("atacante@atpromo.com");
        DeleteConfirmationService service = new DeleteConfirmationService();
        String ticketDaVitima = service.issueTicket("vitima@atpromo.com");
        DeleteConfirmationFilter filter = new DeleteConfirmationFilter(service);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        PrintWriter writer = mock(PrintWriter.class);

        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/api/promoters/10");
        when(request.getHeader(DeleteConfirmationFilter.TICKET_HEADER)).thenReturn(ticketDaVitima);
        when(response.getWriter()).thenReturn(writer);

        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void requisicaoQueNaoEDelete_passaDireto_semExigirTicket() throws Exception {
        autenticarComo("user@atpromo.com");
        DeleteConfirmationService service = new DeleteConfirmationService();
        DeleteConfirmationFilter filter = new DeleteConfirmationFilter(service);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/promoters");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
