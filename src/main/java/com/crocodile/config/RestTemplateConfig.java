package com.crocodile.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate Configuration
 *
 * Provides a centralized configuration for RestTemplate used across the application.
 * This allows for consistent HTTP client behavior and future customization
 * (e.g., interceptors, error handlers, timeouts).
 *
 * Configuration:
 * - http.client.connect-timeout-seconds: Connection establishment timeout
 * - http.client.read-timeout-seconds: Response read timeout
 */
@Configuration
public class RestTemplateConfig {

    @Value("${http.client.connect-timeout-seconds:5}")
    private int connectTimeoutSeconds;
    
    @Value("${http.client.read-timeout-seconds:30}")
    private int readTimeoutSeconds;

    /**
     * Creates a RestTemplate bean with configured timeouts
     * 
     * Timeouts prevent indefinite blocking when:
     * - Server is unreachable (connect timeout)
     * - Server is slow to respond (read timeout)
     * 
     * @param builder RestTemplateBuilder with auto-configured defaults
     * @return configured RestTemplate instance with timeouts
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Configure request factory with timeouts
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        
        return builder
            .requestFactory(() -> requestFactory)
            .build();
    }
}

