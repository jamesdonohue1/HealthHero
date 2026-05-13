package com.hl7decoder.api;

import com.hl7decoder.api.dto.auth.AccountUpdateRequest;
import com.hl7decoder.api.dto.auth.ApiKeyCreateRequest;
import com.hl7decoder.api.dto.auth.LoginRequest;
import com.hl7decoder.api.dto.auth.PasswordResetRequest;
import com.hl7decoder.api.dto.auth.RegisterRequest;
import com.hl7decoder.model.auth.ApiKeyResponse;
import com.hl7decoder.model.auth.AuthCapabilitiesResponse;
import com.hl7decoder.model.auth.AuthResponse;
import com.hl7decoder.persistence.AppUser;
import com.hl7decoder.security.ApiKeyService;
import com.hl7decoder.security.AuthenticatedPrincipal;
import com.hl7decoder.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final ApiKeyService apiKeyService;

    public AuthController(AuthService authService, ApiKeyService apiKeyService) {
        this.authService = authService;
        this.apiKeyService = apiKeyService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public java.util.Map<String, Object> logout() {
        return java.util.Map.of("ok", true);
    }

    @PostMapping("/password-reset")
    public AuthResponse resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        return authService.resetPassword(request);
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return authService.current(requirePrincipal(principal));
    }

    @PatchMapping("/me")
    public AuthResponse update(@AuthenticationPrincipal AuthenticatedPrincipal principal, @Valid @RequestBody AccountUpdateRequest request) {
        return authService.update(requirePrincipal(principal), request);
    }

    @PostMapping("/api-keys")
    public ApiKeyResponse createApiKey(@AuthenticationPrincipal AuthenticatedPrincipal principal, @RequestBody ApiKeyCreateRequest request) {
        AppUser user = authService.user(requirePrincipal(principal));
        return apiKeyService.create(user, request.name());
    }

    @GetMapping("/capabilities")
    public AuthCapabilitiesResponse capabilities() {
        return authService.capabilities();
    }

    private AuthenticatedPrincipal requirePrincipal(AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw new org.springframework.security.authentication.BadCredentialsException("Authentication required.");
        }
        return principal;
    }
}
