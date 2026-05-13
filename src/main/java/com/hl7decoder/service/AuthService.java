package com.hl7decoder.service;

import com.hl7decoder.api.dto.auth.AccountUpdateRequest;
import com.hl7decoder.api.dto.auth.LoginRequest;
import com.hl7decoder.api.dto.auth.PasswordResetRequest;
import com.hl7decoder.api.dto.auth.RegisterRequest;
import com.hl7decoder.model.auth.AuthCapabilitiesResponse;
import com.hl7decoder.model.auth.AuthResponse;
import com.hl7decoder.model.auth.UserRole;
import com.hl7decoder.persistence.AppUser;
import com.hl7decoder.persistence.AppUserRepository;
import com.hl7decoder.persistence.Organization;
import com.hl7decoder.persistence.OrganizationRepository;
import com.hl7decoder.security.AuthenticatedPrincipal;
import com.hl7decoder.security.TokenService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {
    private final AppUserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final int maxFailedAttempts;
    private final Duration lockoutDuration;
    private final int idleTimeoutMinutes;

    public AuthService(AppUserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       @Value("${app.security.max-failed-attempts:5}") int maxFailedAttempts,
                       @Value("${app.security.lockout-minutes:15}") int lockoutMinutes,
                       @Value("${app.security.idle-timeout-minutes:30}") int idleTimeoutMinutes) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockoutDuration = Duration.ofMinutes(lockoutMinutes);
        this.idleTimeoutMinutes = idleTimeoutMinutes;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("An account already exists for this email address.");
        }
        Organization organization = organizationRepository.save(new Organization(request.organizationName().trim()));
        AppUser user = userRepository.save(new AppUser(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                organization,
                Set.of(UserRole.ORGANIZATION_ADMIN, UserRole.CODER, UserRole.INTERFACE_ANALYST)
        ));
        return authResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));
        if (!user.isEnabled() || user.isLocked()) {
            throw new BadCredentialsException("Account is temporarily locked. Please try again later.");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            Instant lockedUntil = user.getFailedLoginCount() + 1 >= maxFailedAttempts ? Instant.now().plus(lockoutDuration) : null;
            user.recordFailure(lockedUntil);
            userRepository.save(user);
            throw new BadCredentialsException("Invalid email or password.");
        }
        user.clearFailures();
        userRepository.save(user);
        return authResponse(user);
    }

    @Transactional
    public AuthResponse resetPassword(PasswordResetRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new EntityNotFoundException("Account not found."));
        user.updatePasswordHash(passwordEncoder.encode(request.newPassword()));
        return authResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse current(AuthenticatedPrincipal principal) {
        AppUser user = userRepository.findByPublicId(principal.userId())
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found."));
        return authResponse(user);
    }

    @Transactional
    public AuthResponse update(AuthenticatedPrincipal principal, AccountUpdateRequest request) {
        AppUser user = userRepository.findByPublicId(principal.userId())
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found."));
        user.updateDisplayName(request.displayName());
        return authResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AppUser user(AuthenticatedPrincipal principal) {
        return userRepository.findByPublicId(principal.userId())
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found."));
    }

    public AuthCapabilitiesResponse capabilities() {
        return new AuthCapabilitiesResponse(
                true,
                true,
                true,
                true,
                java.util.Arrays.stream(UserRole.values()).map(Enum::name).toList(),
                "Bearer HMAC token or X-API-Key",
                idleTimeoutMinutes
        );
    }

    private AuthResponse authResponse(AppUser user) {
        TokenService.IssuedToken token = tokenService.issue(user);
        return new AuthResponse(
                token.token(),
                token.expiresAt(),
                new AuthResponse.AuthUser(
                        user.getPublicId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getOrganization().getPublicId(),
                        user.getOrganization().getName(),
                        user.getRoles().stream().toList()
                ),
                List.of("local_login", "api_keys", "oidc_ready", "mfa_ready", "rbac")
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
