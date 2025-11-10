package com.example.restrosuite.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aws.cognito")
public class CognitoConfig {
    
    private boolean enabled = false;
    private String region = "us-east-1";
    private String userPoolId;
    private String clientId;
    private String clientSecret;
    private String jwkUrl;
    private String issuer;
    private AwsCredentials awsCredentials = new AwsCredentials();
    
    public static class AwsCredentials {
        private String accessKeyId;
        private String secretAccessKey;
        
        public String getAccessKeyId() {
            return accessKeyId;
        }
        
        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }
        
        public String getSecretAccessKey() {
            return secretAccessKey;
        }
        
        public void setSecretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
        }
        
        public boolean isConfigured() {
            return accessKeyId != null && !accessKeyId.isEmpty() 
                && secretAccessKey != null && !secretAccessKey.isEmpty();
        }
    }
    
    public AwsCredentials getAwsCredentials() {
        return awsCredentials;
    }
    
    public void setAwsCredentials(AwsCredentials awsCredentials) {
        this.awsCredentials = awsCredentials;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getUserPoolId() {
        return userPoolId;
    }

    public void setUserPoolId(String userPoolId) {
        this.userPoolId = userPoolId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getJwkUrl() {
        return jwkUrl;
    }

    public void setJwkUrl(String jwkUrl) {
        this.jwkUrl = jwkUrl;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * Constructs the JWK URL if not explicitly provided
     * Format: https://cognito-idp.{region}.amazonaws.com/{userPoolId}/.well-known/jwks.json
     */
    public String getJwkUrlOrDefault() {
        if (jwkUrl != null && !jwkUrl.isEmpty()) {
            return jwkUrl;
        }
        if (userPoolId != null && !userPoolId.isEmpty() && region != null) {
            return String.format("https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json", 
                region, userPoolId);
        }
        return null;
    }

    /**
     * Constructs the issuer URL if not explicitly provided
     * Format: https://cognito-idp.{region}.amazonaws.com/{userPoolId}
     */
    public String getIssuerOrDefault() {
        if (issuer != null && !issuer.isEmpty()) {
            return issuer;
        }
        if (userPoolId != null && !userPoolId.isEmpty() && region != null) {
            return String.format("https://cognito-idp.%s.amazonaws.com/%s", region, userPoolId);
        }
        return null;
    }
}

