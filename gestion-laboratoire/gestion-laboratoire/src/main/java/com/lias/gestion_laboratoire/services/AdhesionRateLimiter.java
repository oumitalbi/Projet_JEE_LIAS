package com.lias.gestion_laboratoire.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AdhesionRateLimiter {
    private static final int MAX_REQUESTS_PER_WINDOW = 5;
    private static final int MAX_TRACKED_ADDRESSES = 10_000;
    private static final long WINDOW_MILLIS = Duration.ofHours(1).toMillis();
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public void verifyAndConsume(String clientAddress) {
        if (clientAddress == null || clientAddress.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adresse réseau non disponible.");
        }
        long now = System.currentTimeMillis();
        Window window = windows.compute(clientAddress, (address, current) ->
                current == null || now - current.startedAt >= WINDOW_MILLIS
                        ? new Window(now, new AtomicInteger()) : current);
        if (window.count.incrementAndGet() > MAX_REQUESTS_PER_WINDOW) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de candidatures depuis cette adresse. Réessayez plus tard.");
        }
        if (windows.size() > MAX_TRACKED_ADDRESSES) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_MILLIS);
            if (windows.size() > MAX_TRACKED_ADDRESSES) {
                windows.remove(clientAddress);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Le service de candidature est temporairement saturé.");
            }
        }
    }

    private record Window(long startedAt, AtomicInteger count) {}
}
