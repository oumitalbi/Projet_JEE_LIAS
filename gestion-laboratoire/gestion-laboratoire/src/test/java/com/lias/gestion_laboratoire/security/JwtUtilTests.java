package com.lias.gestion_laboratoire.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTests {

    @Test
    void tokenCarriesCurrentSessionVersion() {
        JwtUtil jwtUtil = new JwtUtil("ZmFrZS1leGFtcGxlLWtleS1vbmx5LWRvLW5vdC11c2UtMzItYnl0ZXMh");
        String token = jwtUtil.generateToken("membre@example.com", "MEMBRE_PERMANENT", 4);

        assertEquals(4, jwtUtil.extractSessionVersion(token));
        assertTrue(jwtUtil.validateToken(token, "membre@example.com"));
    }
}
