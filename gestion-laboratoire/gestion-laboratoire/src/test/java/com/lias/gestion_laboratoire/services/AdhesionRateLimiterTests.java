package com.lias.gestion_laboratoire.services;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AdhesionRateLimiterTests {
    @Test
    void limitsPublicSubmissionsPerAddress() {
        AdhesionRateLimiter rateLimiter = new AdhesionRateLimiter();
        for (int request = 0; request < 5; request++) {
            rateLimiter.verifyAndConsume("192.0.2.5");
        }

        assertThrows(ResponseStatusException.class, () -> rateLimiter.verifyAndConsume("192.0.2.5"));
    }
}
