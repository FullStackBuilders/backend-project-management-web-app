package com.project.projectmanagementapplication.security;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtConfig {

    @Value("${jwt.secretKey}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.header}")
    private String header;

    public String getSecretKey() {
        return secretKey;
    }

    public String getHeader() {
        return header;
    }

    public long getExpiration() {
        return expiration;
    }

    public SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
}

