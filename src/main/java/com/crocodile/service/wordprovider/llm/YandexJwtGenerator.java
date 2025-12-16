package com.crocodile.service.wordprovider.llm;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Generator for JWT tokens used in Yandex Cloud IAM authentication
 * 
 * Creates signed JWT tokens according to Yandex Cloud requirements:
 * - Algorithm: RS256 (RSA with SHA-256)
 * - Header: kid (key ID)
 * - Claims: aud, iss, iat, exp
 * 
 * The generated JWT is then exchanged for an IAM token via Yandex IAM API.
 */
@Component
@Slf4j
public class YandexJwtGenerator {
    
    private static final String IAM_TOKEN_ENDPOINT = "https://iam.api.cloud.yandex.net/iam/v1/tokens";
    private static final int JWT_EXPIRATION_SECONDS = 3600; // 1 hour (maximum allowed by Yandex)
    
    /**
     * Generate a JWT token for Yandex Cloud IAM authentication
     * 
     * @param authorizedKey the authorized key containing service account ID, key ID, and private key
     * @return signed JWT token as a string
     * @throws IllegalStateException if JWT generation fails
     */
    public String generateJwt(YandexAuthorizedKey authorizedKey) {
        try {
            log.debug("Generating JWT for service account: {}", authorizedKey.getServiceAccountId());
            
            // Parse the private key from PEM format
            PrivateKey privateKey = parsePrivateKey(authorizedKey.getPrivateKey());
            
            // Calculate token expiration time (current time + 1 hour)
            Instant now = Instant.now();
            Instant expiration = now.plusSeconds(JWT_EXPIRATION_SECONDS);
            
            // Build and sign the JWT
            // Note: Yandex Cloud expects:
            // - 'aud' as a STRING, not an array
            // - 'alg' as PS256 (RSA-PSS with SHA-256), not RS256
            String jwt = Jwts.builder()
                .header()
                    .add("kid", authorizedKey.getId())
                    .add("typ", "JWT")
                    .and()
                .claim("aud", IAM_TOKEN_ENDPOINT)  // Single string, not array
                .issuer(authorizedKey.getServiceAccountId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(privateKey, Jwts.SIG.PS256)  // PS256, not RS256!
                .compact();
            
            log.debug("JWT generated successfully, expires at: {}", expiration);
            return jwt;
            
        } catch (Exception e) {
            log.error("Failed to generate JWT: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to generate JWT for Yandex Cloud authentication: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse a private key from PEM format string
     * 
     * Handles Yandex Cloud format which includes a comment line before the key:
     * "PLEASE DO NOT REMOVE THIS LINE! Yandex.Cloud SA Key ID <ajexxxxx>\n-----BEGIN PRIVATE KEY-----"
     * 
     * @param pemKey private key in PEM format (PKCS#8), may contain Yandex Cloud comment
     * @return PrivateKey object
     * @throws Exception if parsing fails
     */
    private PrivateKey parsePrivateKey(String pemKey) throws Exception {
        // Remove Yandex Cloud comment line if present
        String cleanedKey = pemKey;
        if (pemKey.contains("PLEASE DO NOT REMOVE THIS LINE!")) {
            // Extract everything starting from "-----BEGIN PRIVATE KEY-----"
            int beginIndex = pemKey.indexOf("-----BEGIN PRIVATE KEY-----");
            if (beginIndex >= 0) {
                cleanedKey = pemKey.substring(beginIndex);
            }
        }
        
        // Remove PEM header/footer and whitespace
        String privateKeyPEM = cleanedKey
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
        
        // Decode base64
        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        
        // Generate private key
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        
        return keyFactory.generatePrivate(keySpec);
    }
}

