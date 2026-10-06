package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.User;
import com.atpromo.systematpromo.repository.PasswordResetRequestRepository;
import com.atpromo.systematpromo.repository.UserRepository;
import com.atpromo.systematpromo.security.JwtUtil;
import com.atpromo.systematpromo.security.LoginAttemptService;
import com.atpromo.systematpromo.security.PasswordResetLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ACHADO H3 (CORRIGIDO): AuthController.clientIp() confiava direto no
 * cabeçalho "X-Forwarded-For" enviado pelo PRÓPRIO cliente (não havia proxy
 * confiável configurado que sobrescrevesse isso). Como o limite de "5
 * solicitações de redefinição de senha por IP por hora"
 * (PasswordResetLimiter) era calculado com esse valor, um atacante que
 * variava o X-Forwarded-For a cada chamada contornava completamente esse
 * limite.
 *
 * A correção faz clientIp() ignorar o header e usar só
 * request.getRemoteAddr() (o IP real da conexão TCP, não controlável pelo
 * cliente). O teste abaixo chama forgotPassword() repetidamente variando o
 * X-Forwarded-For a cada vez e confirma que o limitador de IP agora É
 * acionado (porque o IP real, sempre o mesmo, é o que conta).
 */
class AuthControllerForgotPasswordTest {

    private AuthController controller;
    private PasswordResetLimiter passwordResetLimiter;

    @BeforeEach
    void setUp() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        LoginAttemptService loginAttemptService = mock(LoginAttemptService.class);
        PasswordResetRequestRepository passwordResetRequestRepository = mock(PasswordResetRequestRepository.class);
        passwordResetLimiter = new PasswordResetLimiter(); // implementação real, não mock

        when(userRepository.findByEmail(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.<User>empty());

        controller = new AuthController(
                userRepository, passwordEncoder, jwtUtil, loginAttemptService,
                passwordResetRequestRepository, passwordResetLimiter
        );
    }

    private HttpServletRequest requestComForwardedFor(String ip) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(ip);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1"); // IP real do atacante, sempre o mesmo
        return request;
    }

    @Test
    void variarXForwardedFor_naoContornaMaisOLimiteDe5PorHoraPorIp() {
        // CORRIGIDO: mesmo variando o X-Forwarded-For a cada chamada, o IP
        // real da conexão (10.0.0.1, via getRemoteAddr()) é sempre o mesmo,
        // então o limite de 5/hora por IP passa a ser respeitado.
        int bloqueios = 0;
        for (int i = 0; i < 20; i++) {
            String ipFalsoDiferenteACadaVez = "203.0.113." + i;
            AuthController.ForgotPasswordRequest body =
                    new AuthController.ForgotPasswordRequest("vitima" + i + "@atpromo.com");

            ResponseEntity<?> resposta = controller.forgotPassword(body, requestComForwardedFor(ipFalsoDiferenteACadaVez));

            if (resposta.getStatusCode().value() == 429) {
                bloqueios++;
            }
        }

        assertNotEquals(0, bloqueios,
                "CORRIGIDO: variar o X-Forwarded-For não deveria mais contornar o limite por IP — "
                        + "o IP real (getRemoteAddr()) é sempre o mesmo e deveria ser bloqueado em algum momento");
    }

    @Test
    void semForjarOHeader_oMesmoIpRealEhBloqueadoApos5Chamadas() {
        // Contraprova: se o X-Forwarded-For não existir (cliente direto), o
        // getRemoteAddr() real É usado e o limite funciona normalmente.
        HttpServletRequest requestSemHeaderForjado = mock(HttpServletRequest.class);
        when(requestSemHeaderForjado.getHeader("X-Forwarded-For")).thenReturn(null);
        when(requestSemHeaderForjado.getRemoteAddr()).thenReturn("10.0.0.1");

        int bloqueios = 0;
        for (int i = 0; i < 7; i++) {
            AuthController.ForgotPasswordRequest body =
                    new AuthController.ForgotPasswordRequest("vitima" + i + "@atpromo.com");
            ResponseEntity<?> resposta = controller.forgotPassword(body, requestSemHeaderForjado);
            if (resposta.getStatusCode().value() == 429) {
                bloqueios++;
            }
        }

        assertNotEquals(0, bloqueios, "Sem X-Forwarded-For forjado, o limite por IP deveria bloquear em algum momento");
    }
}
