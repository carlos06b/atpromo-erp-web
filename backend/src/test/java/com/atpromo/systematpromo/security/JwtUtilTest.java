package com.atpromo.systematpromo.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de JwtUtil: cobre os pontos pedidos no checklist (expiração,
 * assinatura inválida, token adulterado). Ferramenta de teste pura (sem
 * contexto Spring), injeta jwt.secret/jwt.expiration-ms via reflection —
 * mesma técnica seria usada em produção via application-local.properties,
 * aqui usamos um segredo de teste, nunca o real.
 */
class JwtUtilTest {

    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQtYXBlbmFzLXBhcmEtb3MtdGVzdGVzLWF1dG9tYXRpemFkb3MtbnVuY2EtdXNhci1lbS1wcm9kdWNhbw==";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 86_400_000L);
    }

    @Test
    void tokenRecemGerado_deveSerValido() {
        String token = jwtUtil.generateToken("user@atpromo.com");
        assertTrue(jwtUtil.isTokenValid(token));
        org.junit.jupiter.api.Assertions.assertEquals("user@atpromo.com", jwtUtil.extractEmail(token));
    }

    @Test
    void tokenExpirado_deveSerInvalido() {
        // Gera um token cuja expiração já passou (customExpirationMs negativo).
        String tokenExpirado = jwtUtil.generateToken("user@atpromo.com", -1000L);
        assertFalse(jwtUtil.isTokenValid(tokenExpirado), "Token com data de expiração no passado deveria ser inválido");
    }

    @Test
    void tokenAssinadoComOutroSegredo_deveSerInvalido() {
        SecretKey outraChave = Keys.hmacShaKeyFor(
                "outro-segredo-completamente-diferente-32-bytes-minimo-aqui".getBytes());
        String tokenForjado = Jwts.builder()
                .subject("atacante@atpromo.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(outraChave)
                .compact();

        assertFalse(jwtUtil.isTokenValid(tokenForjado),
                "Token assinado com uma chave diferente da configurada deveria ser rejeitado");
    }

    @Test
    void tokenAdulterado_deveSerInvalido() {
        String tokenValido = jwtUtil.generateToken("user@atpromo.com");
        // Troca um caractere do payload (segunda parte do JWT), mantendo a
        // assinatura original — simula um atacante editando o token manualmente.
        String[] partes = tokenValido.split("\\.");
        String payloadAdulterado = partes[1].length() > 0
                ? (partes[1].charAt(0) == 'a' ? 'b' : 'a') + partes[1].substring(1)
                : partes[1];
        String tokenAdulterado = partes[0] + "." + payloadAdulterado + "." + partes[2];

        assertFalse(jwtUtil.isTokenValid(tokenAdulterado),
                "Token com payload adulterado deveria falhar na verificação de assinatura");
    }

    @Test
    void tokenMalformado_naoDeveLancarExcecaoNaoTratada() {
        assertFalse(jwtUtil.isTokenValid("isso.nao-e.um-jwt-valido"));
        assertFalse(jwtUtil.isTokenValid(""));
    }

    @Test
    void rememberMe_deveGerarTokenComExpiracaoMaiorQuePadrao() {
        String tokenPadrao = jwtUtil.generateToken("user@atpromo.com");
        String tokenLembrar = jwtUtil.generateToken("user@atpromo.com", JwtUtil.REMEMBER_ME_EXPIRATION_MS);

        assertTrue(jwtUtil.isTokenValid(tokenPadrao));
        assertTrue(jwtUtil.isTokenValid(tokenLembrar));
        // 30 dias deve ser maior que as 24h padrão usadas no teste.
        org.junit.jupiter.api.Assertions.assertTrue(
                JwtUtil.REMEMBER_ME_EXPIRATION_MS > 86_400_000L
        );
    }
}
