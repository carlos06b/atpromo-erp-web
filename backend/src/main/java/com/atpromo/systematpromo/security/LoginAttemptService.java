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

    public boolean isBlocked(String email) {
        String key = normalize(email);
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

    public long minutesRemaining(String email) {
        Attempt attempt = attempts.get(normalize(email));
        if (attempt == null || attempt.lockedUntil == null) {
            return 0;
        }
        long secondsLeft = Instant.now().until(attempt.lockedUntil, ChronoUnit.SECONDS);
        return Math.max(1, (secondsLeft + 59) / 60);
    }

    public void registerFailure(String email) {
        String key = normalize(email);
        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt());
        attempt.count++;
        if (attempt.count >= MAX_ATTEMPTS) {
            attempt.lockedUntil = Instant.now().plusMillis(LOCK_DURATION_MS);
        }
    }

    public void registerSuccess(String email) {
        attempts.remove(normalize(email));
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static class Attempt {
        int count = 0;
        Instant lockedUntil;
    }
}