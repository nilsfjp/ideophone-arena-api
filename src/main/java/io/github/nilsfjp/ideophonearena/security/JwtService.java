package io.github.nilsfjp.ideophonearena.security;

import io.github.nilsfjp.ideophonearena.model.AppUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] signingKey;
    private final long expirationMs;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret:ideophone-arena-development-secret-change-me}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs
    ) {
        this.objectMapper = objectMapper;
        this.signingKey = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMs = expirationMs;
    }

    public String generateToken(AppUser user) {
        Instant now = Instant.now();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getUsername());
        payload.put("role", user.getRole().name());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusMillis(expirationMs).getEpochSecond());

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String signingInput = encodedHeader + "." + encodedPayload;
        return signingInput + "." + sign(signingInput);
    }

    public String extractUsername(String token) {
        Object subject = parseClaims(token).get("sub");
        if (!(subject instanceof String username) || username.isBlank()) {
            throw new IllegalArgumentException("JWT subject is missing");
        }
        return username;
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isExpired(token);
    }

    private boolean isExpired(String token) {
        Object expiresAt = parseClaims(token).get("exp");
        if (!(expiresAt instanceof Number expirationEpochSeconds)) {
            throw new IllegalArgumentException("JWT expiration is missing");
        }
        return Instant.now().getEpochSecond() >= expirationEpochSeconds.longValue();
    }

    private Map<String, Object> parseClaims(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("JWT must have three parts");
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = sign(signingInput);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("JWT signature is invalid");
        }

        try {
            byte[] payloadBytes = URL_DECODER.decode(parts[1]);
            return objectMapper.readValue(payloadBytes, new TypeReference<>() {
            });
        } catch (IllegalArgumentException | JacksonException ex) {
            throw new IllegalArgumentException("JWT payload is invalid", ex);
        }
    }

    private String encodeJson(Map<String, Object> values) {
        try {
            return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(values));
        } catch (JacksonException ex) {
            throw new IllegalStateException("Could not serialize JWT", ex);
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            return URL_ENCODER.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not sign JWT", ex);
        }
    }
}
