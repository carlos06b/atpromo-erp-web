package com.atpromo.systematpromo.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000;

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    // ACHADO H4: a chave do bloqueio era só o email normalizado, sem IP nem
    // qualquer outro fator - qualquer pessoa sem credencial alguma, sabendo
    // só o email de um colega, conseguia errar a senha 5 vezes de propósito
    // e trancar o login dele por 15 min, de qualquer lugar. Agora a chave
    // combina email + IP: o bloqueio passa a valer para "este email tentado
    // a partir deste IP", então um atacante trancando a conta da vítima a
    // partir do PRÓPRIO IP não impede a vítima de logar normalmente a
    // partir do IP dela. Isso não piora a proteção contra força bruta (o
    // mesmo IP ainda é bloqueado após 5 tentativas), só deixa de ser trivial
    // trancar remotamente a conta de outra pessoa.
    public boolean isBlocked(String email, String ip) {
        String key = key(email, ip);
        Attempt attempt = attempts.get(key);

        if (attempt == null || attempt.lockedUntil == null) {
            return false;
        }

        if (Instant.now().isBefore(attempt.lockedUntil)) {
            return true;
        }

        attempts.remove(key);
        return false;
    }

    public long minutesRemaining(String email, String ip) {
        Attempt attempt = attempts.get(key(email, ip));
        if (attempt == null || attempt.lockedUntil == null) {
            return 0;
        }
        long secondsLeft = Instant.now().until(attempt.lockedUntil, ChronoUnit.SECONDS);
        return Math.max(1, (secondsLeft + 59) / 60);
    }

    public void registerFailure(String email, String ip) {
        String key = key(email, ip);
        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt());
        attempt.count++;
        if (attempt.count >= MAX_ATTEMPTS) {
            attempt.lockedUntil = Instant.now().plusMillis(LOCK_DURATION_MS);
        }
    }

    public void registerSuccess(String email, String ip) {
        attempts.remove(key(email, ip));
    }

    private String key(String email, String ip) {
        return normalize(email) + "|" + (ip == null ? "" : ip.trim());
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static class Attempt {
        int count = 0;
        Instant lockedUntil;
    }
}