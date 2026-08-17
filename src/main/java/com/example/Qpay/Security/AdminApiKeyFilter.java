package com.example.Qpay.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Protects every /api/v1/admin/** route with a single shared secret key.
 * The admin dashboard sends this key in the "X-Admin-Key" header on every request.
 *
 * Set ADMIN_API_KEY as an environment variable on Render (and locally in
 * application-local.properties) — pick any long random string.
 */
@Component
public class AdminApiKeyFilter extends OncePerRequestFilter {

    @Value("${admin.api-key:}")
    private String adminApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/api/v1/admin/")) {
            String providedKey = request.getHeader("X-Admin-Key");

            if (adminApiKey == null || adminApiKey.isBlank() || providedKey == null || !providedKey.equals(adminApiKey)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Invalid or missing admin key\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}