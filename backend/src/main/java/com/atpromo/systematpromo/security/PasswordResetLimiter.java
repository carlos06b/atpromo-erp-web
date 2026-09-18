package com.atpromo.systematpromo.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PasswordResetLimiter {

    private static final long EMAIL_COOLDOWN_MS = 15 * 60 * 1000; // 15 min
    private static final int MAX_PER_IP_PER_HOUR = 5;
    private static final long IP_WINDOW_MS = 60 * 60 * 1000; // 1 hora

    private final ConcurrentHashMap<String, Instant> lastRequestByEmail = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, IpWindow> requestsByIp = new ConcurrentHashMap<>();

    public boolean isEmailInCooldown(String email) {
        Instant last = lastRequestByEmail.get(normalize(email));
        return last != null && last.plusMillis(EMAIL_COOLDOWN_MS).isAfter(Instant.now());
    }

    public void registerEmailRequest(String email) {
        lastRequestByEmail.put(normalize(email), Instant.now());
    }

    public boolean isIpBlocked(String ip) {
        IpWindow window = requestsByIp.get(ip);
        if (window == null) {
            return false;
        }
        if (Instant.now().isAfter(window.windowStart.plusMillis(IP_WINDOW_MS))) {
            requestsByIp.remove(ip);
            return false;
        }
        return window.count.get() >= MAX_PER_IP_PER_HOUR;
    }

    public void registerIpRequest(String ip) {
        requestsByIp.compute(ip, (key, window) -> {
            if (window == null || Instant.now().isAfter(window.windowStart.plusMillis(IP_WINDOW_MS))) {
                return new IpWindow();
            }
            window.count.incrementAndGet();
            return window;
        });
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static class IpWindow {
        final Instant windowStart = Instant.now();
        final AtomicInteger count = new AtomicInteger(1);
    }
}