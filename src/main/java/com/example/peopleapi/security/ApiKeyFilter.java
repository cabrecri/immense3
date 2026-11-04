package com.example.peopleapi.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Requires a valid {@code X-API-Key} header on every request except the H2 console (fail-closed).
 * Errors are delegated to the {@link HandlerExceptionResolver} so they are rendered by
 * the same {@code @RestControllerAdvice} as every other error response.
 */
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";

    /**
     * The only public paths: the H2 console, matched strictly. Every segment after the prefix may only contain
     * letters, digits, '_' and '-', with optional dot-separated parts. Anything unusual ('..', ';', '%', '\\',
     * '//') does not match and therefore requires the key, so URL tricks cannot spoof the exemption.
     */
    private static final Pattern PUBLIC_PATH =
            Pattern.compile("^/h2-console(?:/[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*)*/?$");

    private final byte[] expectedDigest;
    private final HandlerExceptionResolver resolver;

    public ApiKeyFilter(String expectedKey, HandlerExceptionResolver resolver) {
        if (expectedKey == null || expectedKey.isBlank()) {
            throw new IllegalStateException(
                    "The expected API key is not configured: set the PEOPLE_API_KEY environment variable");
        }
        this.expectedDigest = sha256(expectedKey);
        this.resolver = resolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
        return PUBLIC_PATH.matcher(path).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (provided == null || provided.isBlank()) {
            resolver.resolveException(request, response, null, new MissingApiKeyException());
            return;
        }
        // Constant-time comparison of fixed-length digests: neither the content nor the length leaks.
        if (!MessageDigest.isEqual(expectedDigest, sha256(provided))) {
            resolver.resolveException(request, response, null, new InvalidApiKeyException());
            return;
        }
        chain.doFilter(request, response);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
