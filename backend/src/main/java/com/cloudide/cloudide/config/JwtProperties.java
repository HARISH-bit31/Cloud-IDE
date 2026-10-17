package com.cloudide.cloudide.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Secret key for signing JWT tokens (HMAC-SHA256).
     * Minimum 256 bits (32 bytes). Must be supplied via environment/configuration.
     */
    private String secret;

    /**
     * Token expiration time in milliseconds (default 24h = 86400000 ms).
     */
    private long expiration = 86400000L;

    @PostConstruct
    public void validate() {
        if (secret == null || secret.isBlank() || "replace-with-a-long-random-secret".equalsIgnoreCase(secret)) {
            throw new IllegalStateException("FATAL: JWT_SECRET is not configured. Cloud IDE requires a valid JWT secret.");
        }
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
}
