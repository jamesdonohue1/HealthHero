package com.hl7decoder.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hl7decoder.model.auth.UserRole;
import com.hl7decoder.persistence.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TokenService {
    private final ObjectMapper objectMapper;
    private final String secret;
    private final Duration ttl;

    public TokenService(ObjectMapper objectMapper,
                        @Value("${app.security.jwt-secret:dev-change-me-healthcare-hero}") String secret,
                        @Value("${app.security.token-ttl-minutes:480}") long ttlMinutes) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public IssuedToken issue(AppUser user) {
        Instant expiresAt = Instant.now().plus(ttl);
        Map<String, Object> claims = Map.of(
                "sub", user.getPublicId().toString(),
                "email", user.getEmail(),
                "org", user.getOrganization().getPublicId().toString(),
                "roles", user.getRoles().stream().map(Enum::name).toList(),
                "exp", expiresAt.getEpochSecond()
        );
        String payload = base64(json(claims));
        String signature = sign(payload);
        return new IssuedToken(payload + "." + signature, expiresAt);
    }

    public Optional<AuthenticatedPrincipal> parse(String token) {
        if (token == null || !token.contains(".")) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.", 2);
        if (!constantTimeEquals(sign(parts[0]), parts[1])) {
            return Optional.empty();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8), Map.class);
            long exp = ((Number) claims.get("exp")).longValue();
            if (Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
                return Optional.empty();
            }
            @SuppressWarnings("unchecked")
            List<String> roleNames = (List<String>) claims.get("roles");
            Set<UserRole> roles = roleNames.stream().map(UserRole::valueOf).collect(Collectors.toSet());
            return Optional.of(new AuthenticatedPrincipal(
                    UUID.fromString(claims.get("sub").toString()),
                    claims.get("email").toString(),
                    UUID.fromString(claims.get("org").toString()),
                    roles,
                    "TOKEN"
            ));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String json(Map<String, Object> claims) {
        try {
            return objectMapper.writeValueAsString(claims);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to encode auth token", ex);
        }
    }

    private String base64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign auth token", ex);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigestUtil.equals(left, right);
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
