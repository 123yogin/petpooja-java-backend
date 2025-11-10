package com.example.restrosuite.security;

import java.io.IOException;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nimbusds.jwt.JWTClaimsSet;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CognitoTokenValidator cognitoTokenValidator;

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
                                    @org.springframework.lang.NonNull HttpServletResponse response,
                                    @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {
        // Skip JWT validation for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Skip JWT validation for auth endpoints (they are public)
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Skip JWT validation for Cognito test endpoints (they handle validation themselves)
        if (path != null && path.startsWith("/api/cognito/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            boolean authenticated = false;
            String validationError = null;
            
            // Try Cognito token validation first if enabled
            if (cognitoTokenValidator.isEnabled()) {
                try {
                    JWTClaimsSet cognitoClaims = cognitoTokenValidator.validateToken(token);
                    if (cognitoClaims != null) {
                        String email = cognitoTokenValidator.extractEmail(cognitoClaims);
                        String role = cognitoTokenValidator.extractRole(cognitoClaims);
                        
                        if (email != null && role != null) {
                            request.setAttribute("email", email);
                            request.setAttribute("role", role);
                            request.setAttribute("tokenType", "cognito");
                            
                            // Set up Spring Security authentication context
                            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
                            UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(email, null, Collections.singletonList(authority));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            
                            authenticated = true;
                            log.debug("Authenticated via Cognito token for user: {}", email);
                        } else {
                            validationError = "Failed to extract email or role from Cognito token";
                            log.warn("Cognito token validation: {}", validationError);
                        }
                    } else {
                        validationError = "Cognito token validation returned null";
                        log.warn("Cognito token validation failed for request: {}", request.getRequestURI());
                    }
                } catch (Exception e) {
                    validationError = "Cognito token validation exception: " + e.getMessage();
                    log.error("Exception during Cognito token validation", e);
                }
            } else {
                log.debug("Cognito validation is disabled, trying custom JWT");
            }
            
            // Fallback to custom JWT validation if Cognito validation failed or is disabled
            if (!authenticated) {
                try {
                    if (jwtUtil.validateToken(token)) {
                        String email = jwtUtil.extractEmail(token);
                        String role = jwtUtil.extractRole(token);
                        
                        request.setAttribute("email", email);
                        request.setAttribute("role", role);
                        request.setAttribute("tokenType", "custom");
                        
                        // Set up Spring Security authentication context
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(email, null, Collections.singletonList(authority));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        authenticated = true;
                        log.debug("Authenticated via custom JWT token for user: {}", email);
                    } else {
                        validationError = "Custom JWT token validation failed";
                        log.warn("Custom JWT token validation failed for request: {}", request.getRequestURI());
                    }
                } catch (JwtException e) {
                    validationError = "JWT validation exception: " + e.getMessage();
                    log.warn("JWT validation exception: {}", e.getMessage());
                } catch (Exception e) {
                    validationError = "Unexpected error during JWT validation: " + e.getMessage();
                    log.error("Unexpected error during JWT validation", e);
                }
            }
            
            // If token was provided but validation failed, return unauthorized
            if (!authenticated) {
                log.warn("Token validation failed for request: {} - Error: {}", request.getRequestURI(), validationError);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid or expired token\",\"message\":\"" + 
                    (validationError != null ? validationError : "Token validation failed") + "\"}");
                return;
            }
        } else {
            // No token provided - let Spring Security handle it (will return 403 if endpoint requires auth)
            log.debug("No Authorization header found for request: {}", request.getRequestURI());
        }
        filterChain.doFilter(request, response);
    }
}

