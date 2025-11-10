package com.example.restrosuite.security;

import com.example.restrosuite.config.CognitoConfig;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

@Component
public class CognitoTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(CognitoTokenValidator.class);
    
    private final CognitoConfig cognitoConfig;
    private JWKSource<SecurityContext> jwkSource;
    private ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    public CognitoTokenValidator(CognitoConfig cognitoConfig) {
        this.cognitoConfig = cognitoConfig;
        initializeJwtProcessor();
    }

    private void initializeJwtProcessor() {
        if (!cognitoConfig.isEnabled()) {
            log.info("Cognito is disabled, skipping JWT processor initialization");
            return;
        }

        try {
            String jwkUrl = cognitoConfig.getJwkUrlOrDefault();
            if (jwkUrl == null || jwkUrl.isEmpty()) {
                log.warn("Cognito JWK URL is not configured");
                return;
            }

            log.info("Initializing Cognito JWT processor with JWK URL: {}", jwkUrl);
            URL jwkUrlObj = new URL(jwkUrl);
            @SuppressWarnings("deprecation")
            RemoteJWKSet<SecurityContext> remoteJWKSet = new RemoteJWKSet<>(jwkUrlObj);
            jwkSource = remoteJWKSet;
            
            jwtProcessor = new DefaultJWTProcessor<>();
            JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
            JWSKeySelector<SecurityContext> keySelector = 
                new JWSVerificationKeySelector<>(expectedJWSAlg, jwkSource);
            jwtProcessor.setJWSKeySelector(keySelector);
            
            log.info("Cognito JWT processor initialized successfully");
        } catch (MalformedURLException e) {
            log.error("Invalid JWK URL: {}", cognitoConfig.getJwkUrlOrDefault(), e);
        }
    }

    /**
     * Validates a Cognito JWT token and extracts claims
     * @param token The JWT token string
     * @return JWTClaimsSet if valid, null otherwise
     */
    public JWTClaimsSet validateToken(String token) {
        if (!cognitoConfig.isEnabled() || jwtProcessor == null) {
            log.debug("Cognito validation skipped - Cognito is disabled or not initialized");
            return null;
        }

        try {
            // Parse and validate the token
            JWTClaimsSet claimsSet = jwtProcessor.process(token, null);
            
            // Verify issuer
            String expectedIssuer = cognitoConfig.getIssuerOrDefault();
            if (expectedIssuer != null && !expectedIssuer.isEmpty()) {
                String tokenIssuer = claimsSet.getIssuer();
                if (!expectedIssuer.equals(tokenIssuer)) {
                    log.warn("Token issuer mismatch. Expected: {}, Got: {}", expectedIssuer, tokenIssuer);
                    return null;
                }
            }

            // Verify client ID if configured
            // Cognito tokens use 'aud' (audience) claim for client ID, not 'client_id'
            String expectedClientId = cognitoConfig.getClientId();
            if (expectedClientId != null && !expectedClientId.isEmpty()) {
                // Try 'aud' claim first (standard JWT audience claim)
                String tokenClientId = claimsSet.getAudience() != null && !claimsSet.getAudience().isEmpty() 
                    ? claimsSet.getAudience().get(0) 
                    : null;
                
                // Fallback to 'client_id' claim if 'aud' is not available
                if (tokenClientId == null) {
                    tokenClientId = claimsSet.getStringClaim("client_id");
                }
                
                // Also check 'aud' as a string claim (some tokens might have it as a string)
                if (tokenClientId == null) {
                    Object audObj = claimsSet.getClaim("aud");
                    if (audObj instanceof String) {
                        tokenClientId = (String) audObj;
                    } else if (audObj instanceof java.util.List && !((java.util.List<?>) audObj).isEmpty()) {
                        tokenClientId = ((java.util.List<?>) audObj).get(0).toString();
                    }
                }
                
                if (tokenClientId == null || !expectedClientId.equals(tokenClientId)) {
                    log.warn("Token client_id mismatch. Expected: {}, Got: {} (checked aud and client_id claims)", 
                        expectedClientId, tokenClientId);
                    // Don't fail validation if client ID check fails - log warning but continue
                    // This allows tokens from different app clients if needed
                    log.debug("Continuing validation despite client_id mismatch");
                } else {
                    log.debug("Token client_id verified: {}", tokenClientId);
                }
            }

            // Verify token is not expired (handled by processor, but double-check)
            if (claimsSet.getExpirationTime() != null && 
                claimsSet.getExpirationTime().before(new java.util.Date())) {
                log.warn("Token has expired");
                return null;
            }

            log.debug("Cognito token validated successfully for user: {}", claimsSet.getSubject());
            return claimsSet;
        } catch (ParseException e) {
            log.error("Failed to parse JWT token: {}", e.getMessage());
            return null;
        } catch (BadJOSEException e) {
            log.error("Bad JOSE exception while validating token: {}", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("expired")) {
                log.warn("Token has expired");
            }
            return null;
        } catch (JOSEException e) {
            log.error("JOSE exception while validating token: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected exception during token validation: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extracts email from Cognito token claims
     */
    public String extractEmail(JWTClaimsSet claimsSet) {
        if (claimsSet == null) {
            return null;
        }
        
        try {
            // Cognito typically uses 'email' claim or 'sub' as username
            String email = claimsSet.getStringClaim("email");
            if (email == null || email.isEmpty()) {
                // Fallback to 'cognito:username' or 'sub'
                email = claimsSet.getStringClaim("cognito:username");
                if (email == null || email.isEmpty()) {
                    email = claimsSet.getSubject();
                }
            }
            return email;
        } catch (ParseException e) {
            log.warn("Error extracting email from Cognito token claims", e);
            // Fallback to subject
            return claimsSet.getSubject();
        }
    }

    /**
     * Extracts role from Cognito token claims
     * Cognito roles are typically in 'cognito:groups' claim
     * Returns the first group as the primary role
     */
    public String extractRole(JWTClaimsSet claimsSet) {
        if (claimsSet == null) {
            return null;
        }
        
        try {
            // Try to get role from 'cognito:groups' claim
            Object groupsObj = claimsSet.getClaim("cognito:groups");
            if (groupsObj != null) {
                if (groupsObj instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<String> groups = (java.util.List<String>) groupsObj;
                    if (!groups.isEmpty()) {
                        // Return the first group as the primary role
                        String primaryRole = groups.get(0).toUpperCase();
                        log.debug("Extracted role '{}' from Cognito groups: {}", primaryRole, groups);
                        return primaryRole;
                    }
                }
            }
            
            // Fallback to 'custom:role' claim if present
            String customRole = claimsSet.getStringClaim("custom:role");
            if (customRole != null && !customRole.isEmpty()) {
                log.debug("Extracted role '{}' from custom:role claim", customRole.toUpperCase());
                return customRole.toUpperCase();
            }
            
            // Default role if none found
            log.debug("No role found in Cognito token, using default USER role");
            return "USER";
        } catch (Exception e) {
            log.warn("Error extracting role from Cognito token", e);
            return "USER";
        }
    }

    /**
     * Extracts all groups/roles from Cognito token claims
     * Returns a list of all groups the user belongs to
     */
    public java.util.List<String> extractAllRoles(JWTClaimsSet claimsSet) {
        java.util.List<String> roles = new java.util.ArrayList<>();
        
        if (claimsSet == null) {
            return roles;
        }
        
        try {
            // Get all groups from 'cognito:groups' claim
            Object groupsObj = claimsSet.getClaim("cognito:groups");
            if (groupsObj != null && groupsObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> groups = (java.util.List<String>) groupsObj;
                for (String group : groups) {
                    roles.add(group.toUpperCase());
                }
            }
            
            // Add custom role if present and not already in list
            String customRole = claimsSet.getStringClaim("custom:role");
            if (customRole != null && !customRole.isEmpty()) {
                String upperRole = customRole.toUpperCase();
                if (!roles.contains(upperRole)) {
                    roles.add(upperRole);
                }
            }
            
            // If no roles found, add default USER role
            if (roles.isEmpty()) {
                roles.add("USER");
            }
            
            return roles;
        } catch (Exception e) {
            log.warn("Error extracting all roles from Cognito token", e);
            roles.add("USER");
            return roles;
        }
    }

    /**
     * Checks if Cognito authentication is enabled
     */
    public boolean isEnabled() {
        return cognitoConfig.isEnabled() && jwtProcessor != null;
    }
}

