package com.hl7decoder.security;

import com.hl7decoder.model.auth.ApiKeyResponse;
import com.hl7decoder.persistence.ApiKeyEntity;
import com.hl7decoder.persistence.ApiKeyRepository;
import com.hl7decoder.persistence.AppUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApiKeyService {
    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, PasswordEncoder passwordEncoder) {
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ApiKeyResponse create(AppUser owner, String name) {
        String raw = "hh_" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        ApiKeyEntity saved = apiKeyRepository.save(new ApiKeyEntity(sha256(raw), name == null || name.isBlank() ? "Default API key" : name.trim(), owner));
        return new ApiKeyResponse(saved.getPublicId(), saved.getName(), raw);
    }

    public Optional<AuthenticatedPrincipal> authenticate(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }
        return apiKeyRepository.findByKeyHash(sha256(rawKey))
                .filter(ApiKeyEntity::isActive)
                .map(key -> {
                    key.markUsed();
                    apiKeyRepository.save(key);
                    AppUser user = key.getOwner();
                    return new AuthenticatedPrincipal(user.getPublicId(), user.getEmail(), user.getOrganization().getPublicId(), user.getRoles(), "API_KEY");
                });
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            return passwordEncoder.encode(value);
        }
    }
}
