package com.crocodile.service.wordprovider.llm.yandexgpt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provider for Yandex Cloud IAM tokens with automatic refresh and caching
 * 
 * This component:
 * - Loads authorized key from JSON file
 * - Generates JWT using the private key
 * - Exchanges JWT for IAM token via Yandex IAM API
 * - Caches IAM token and refreshes before expiration
 * - Thread-safe token access
 * 
 * IAM tokens are valid for 12 hours but we refresh them 10 minutes before expiration.
 */
@Component
@ConditionalOnProperty(prefix = "game.llm.yandex-gpt", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class YandexIamTokenProvider {
    
    private static final String IAM_TOKEN_ENDPOINT = "https://iam.api.cloud.yandex.net/iam/v1/tokens";
    private static final Duration REFRESH_BEFORE_EXPIRY = Duration.ofMinutes(10);
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;
    
    private final RestTemplate restTemplate;
    private final YandexJwtGenerator jwtGenerator;
    private final ObjectMapper objectMapper;
    
    @Value("${game.llm.yandex-gpt.authorized-key-path:}")
    private String authorizedKeyPath;
    
    // Cached token data
    private volatile String cachedIamToken;
    private volatile Instant tokenExpiresAt;
    private YandexAuthorizedKey authorizedKey;
    
    // Lock for thread-safe token refresh
    private final ReentrantLock tokenRefreshLock = new ReentrantLock();
    
    /**
     * Initialize the provider by loading the authorized key file
     */
    @PostConstruct
    public void init() {
        if (authorizedKeyPath == null || authorizedKeyPath.isBlank()) {
            log.warn("Yandex GPT authorized key path is not configured. IAM token authentication will not be available.");
            return;
        }
        
        try {
            log.info("Loading Yandex Cloud authorized key from: {}", authorizedKeyPath);
            authorizedKey = loadAuthorizedKey(authorizedKeyPath);
            log.info("Authorized key loaded successfully for service account: {}", authorizedKey.getServiceAccountId());
        } catch (Exception e) {
            log.error("Failed to load authorized key from {}: {}", authorizedKeyPath, e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize Yandex IAM token provider: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get a valid IAM token, refreshing if necessary
     * 
     * @return valid IAM token
     * @throws IllegalStateException if token cannot be obtained
     */
    public String getToken() {
        if (authorizedKey == null) {
            throw new IllegalStateException("Authorized key not loaded. Check configuration.");
        }
        
        // Check if we need to refresh the token
        if (needsRefresh()) {
            tokenRefreshLock.lock();
            try {
                // Double-check after acquiring lock (another thread might have refreshed)
                if (needsRefresh()) {
                    refreshToken();
                }
            } finally {
                tokenRefreshLock.unlock();
            }
        }
        
        return cachedIamToken;
    }
    
    /**
     * Check if the token needs to be refreshed
     */
    private boolean needsRefresh() {
        if (cachedIamToken == null || tokenExpiresAt == null) {
            return true;
        }
        
        // Refresh if token expires in less than REFRESH_BEFORE_EXPIRY
        Instant refreshTime = tokenExpiresAt.minus(REFRESH_BEFORE_EXPIRY);
        return Instant.now().isAfter(refreshTime);
    }
    
    /**
     * Refresh the IAM token by generating a new JWT and exchanging it
     */
    private void refreshToken() {
        log.info("Refreshing Yandex IAM token...");
        
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Generate JWT
                String jwt = jwtGenerator.generateJwt(authorizedKey);
                
                // Exchange JWT for IAM token
                IamTokenResponse response = exchangeJwtForIamToken(jwt);
                
                // Cache the token
                cachedIamToken = response.getIamToken();
                tokenExpiresAt = parseExpirationTime(response.getExpiresAt());
                
                log.info("IAM token refreshed successfully. Expires at: {}", tokenExpiresAt);
                return;
                
            } catch (Exception e) {
                lastException = e;
                log.warn("Failed to refresh IAM token (attempt {}/{}): {}", attempt, MAX_RETRIES, e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        throw new IllegalStateException("Failed to refresh IAM token after " + MAX_RETRIES + " attempts", lastException);
    }
    
    /**
     * Exchange JWT for IAM token via Yandex IAM API
     */
    private IamTokenResponse exchangeJwtForIamToken(String jwt) {
        try {
            // Build simple JSON request body with jwt field
            // According to Yandex Cloud docs: https://yandex.cloud/docs/iam/operations/iam-token/create-for-sa
            JwtExchangeRequest request = new JwtExchangeRequest(jwt);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<JwtExchangeRequest> httpEntity = new HttpEntity<>(request, headers);
            
            log.debug("Exchanging JWT for IAM token at: {}", IAM_TOKEN_ENDPOINT);
            
            // Make request
            ResponseEntity<IamTokenResponse> response = restTemplate.postForEntity(
                IAM_TOKEN_ENDPOINT,
                httpEntity,
                IamTokenResponse.class
            );
            
            IamTokenResponse responseBody = response.getBody();
            if (responseBody == null || responseBody.getIamToken() == null) {
                throw new IllegalStateException("Empty response from IAM token endpoint");
            }
            
            log.debug("Successfully obtained IAM token, expires at: {}", responseBody.getExpiresAt());
            return responseBody;
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("Failed to exchange JWT for IAM token: {} - Response: {}", e.getMessage(), e.getResponseBodyAsString(), e);
            throw new IllegalStateException("Failed to obtain IAM token: " + e.getMessage(), e);
        } catch (RestClientException e) {
            log.error("Failed to exchange JWT for IAM token: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to obtain IAM token: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load authorized key from JSON file
     */
    private YandexAuthorizedKey loadAuthorizedKey(String filePath) throws IOException {
        File keyFile = new File(filePath);
        if (!keyFile.exists()) {
            throw new IOException("Authorized key file not found: " + filePath);
        }
        
        YandexAuthorizedKey key = objectMapper.readValue(keyFile, YandexAuthorizedKey.class);
        log.debug("Loaded authorized key for service account: {}", key.getServiceAccountId());
        return key;
    }
    
    /**
     * Parse expiration time from ISO 8601 format
     */
    private Instant parseExpirationTime(String expiresAt) {
        try {
            return ZonedDateTime.parse(expiresAt, DateTimeFormatter.ISO_DATE_TIME).toInstant();
        } catch (Exception e) {
            log.warn("Failed to parse expiration time '{}', using default TTL", expiresAt);
            // Default: 12 hours from now
            return Instant.now().plus(Duration.ofHours(12));
        }
    }
    
    /**
     * Force token refresh (useful for error recovery)
     */
    public void forceRefresh() {
        log.info("Forcing IAM token refresh");
        tokenRefreshLock.lock();
        try {
            cachedIamToken = null;
            tokenExpiresAt = null;
            refreshToken();
        } finally {
            tokenRefreshLock.unlock();
        }
    }
    
    // ==================== DTOs for IAM API ====================
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class JwtExchangeRequest {
        private String jwt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class IamTokenResponse {
        @JsonProperty("iamToken")
        private String iamToken;
        
        @JsonProperty("expiresAt")
        private String expiresAt;
    }
}

