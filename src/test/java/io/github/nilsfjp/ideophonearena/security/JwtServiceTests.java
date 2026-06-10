package io.github.nilsfjp.ideophonearena.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.nilsfjp.ideophonearena.model.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class JwtServiceTests {

    private static final String SECRET = "unit-test-secret-not-used-anywhere-else";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private JwtService jwtService(long expirationMs) {
        return new JwtService(objectMapper, SECRET, expirationMs);
    }

    private UserDetails userDetails(String username) {
        return User.withUsername(username).password("ignored").authorities("ROLE_USER").build();
    }

    @Test
    void validTokenRoundTripsUsernameAndPassesValidation() {
        JwtService service = jwtService(ONE_HOUR_MS);
        String token = service.generateToken(new AppUser("player", "player@example.test", "hash"));

        assertEquals("player", service.extractUsername(token));
        assertTrue(service.isTokenValid(token, userDetails("player")));
    }

    @Test
    void tokenForAnotherUserIsRejected() {
        JwtService service = jwtService(ONE_HOUR_MS);
        String token = service.generateToken(new AppUser("player", "player@example.test", "hash"));

        assertFalse(service.isTokenValid(token, userDetails("someone-else")));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService service = jwtService(-1000L);
        String token = service.generateToken(new AppUser("player", "player@example.test", "hash"));

        assertFalse(service.isTokenValid(token, userDetails("player")));
    }

    @Test
    void tamperedPayloadIsRejected() {
        JwtService service = jwtService(ONE_HOUR_MS);
        String token = service.generateToken(new AppUser("player", "player@example.test", "hash"));
        String[] parts = token.split("\\.");
        char flipped = parts[1].charAt(0) == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + flipped + parts[1].substring(1) + "." + parts[2];

        assertThrows(IllegalArgumentException.class, () -> service.isTokenValid(tampered, userDetails("player")));
    }

    @Test
    void tamperedSignatureIsRejected() {
        JwtService service = jwtService(ONE_HOUR_MS);
        String token = service.generateToken(new AppUser("player", "player@example.test", "hash"));
        String[] parts = token.split("\\.");
        char flipped = parts[2].charAt(0) == 'A' ? 'B' : 'A';
        String tampered = parts[0] + "." + parts[1] + "." + flipped + parts[2].substring(1);

        assertThrows(IllegalArgumentException.class, () -> service.extractUsername(tampered));
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        String token = jwtService(ONE_HOUR_MS).generateToken(new AppUser("player", "player@example.test", "hash"));
        JwtService other = new JwtService(objectMapper, "a-completely-different-secret-value", ONE_HOUR_MS);

        assertThrows(IllegalArgumentException.class, () -> other.extractUsername(token));
    }

    @Test
    void blankSecretFailsFastAtConstruction() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new JwtService(objectMapper, " ", ONE_HOUR_MS)
        );
        assertTrue(exception.getMessage().contains("app.jwt.secret"));
    }
}
