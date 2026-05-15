package com.hl7decoder.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final int limitPerMinute;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.api.rate-limit-per-minute:120}") int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/") || request.getRequestURI().startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }
        WindowCounter counter = counters.compute(key(request), (key, current) -> {
            long minute = Instant.now().getEpochSecond() / 60;
            if (current == null || current.minute != minute) {
                return new WindowCounter(minute);
            }
            return current;
        });
        int used = counter.count.incrementAndGet();
        response.setHeader("X-RateLimit-Limit", Integer.toString(limitPerMinute));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(Math.max(0, limitPerMinute - used)));
        if (used > limitPerMinute) {
            response.sendError(429, "Rate limit exceeded.");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String key(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            return "api-key:" + apiKey.hashCode();
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && !authorization.isBlank()) {
            return "auth:" + authorization.hashCode();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private static final class WindowCounter {
        private final long minute;
        private final AtomicInteger count = new AtomicInteger();

        private WindowCounter(long minute) {
            this.minute = minute;
        }
    }
}
