package org.example.fms.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.fms.core.util.ResponseUtil;

import java.io.IOException;

/**
 * Protects all API routes by requiring a valid JWT in the Authorization header.
 */
@WebFilter("/api/v1/*")
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();

        // 1. Bypass authentication for login route
        if (path.endsWith("/api/v1/auth/login")) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Extract Authorization header
        String authHeader = req.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ResponseUtil.sendUnauthorized(res, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);

        try {
            // 3. Validate Token
            Claims claims = JwtUtil.validateTokenAndGetClaims(token);

            // 4. Set user info in request attributes for downstream servlets to use for
            // specific RBAC checks
            req.setAttribute("userId", claims.getSubject());
            req.setAttribute("userRole", claims.get("role"));

            // Continue the chain
            chain.doFilter(request, response);

        } catch (JwtException e) {
            ResponseUtil.sendUnauthorized(res, "Token expired or invalid: " + e.getMessage());
        } catch (Exception e) {
            ResponseUtil.sendUnauthorized(res, "Authentication failed.");
        }
    }

    @Override
    public void destroy() {
    }
}
