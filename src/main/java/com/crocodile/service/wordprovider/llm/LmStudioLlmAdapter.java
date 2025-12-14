package com.crocodile.service.wordprovider.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * LM Studio LLM Adapter
 * 
 * Integrates with LM Studio's local LLM server using OpenAI-compatible API.
 * LM Studio provides a local endpoint for running LLMs with the same interface as OpenAI.
 * 
 * Black Box Principle:
 * - Implementation details are completely hidden behind the LlmAdapter interface
 * - Can be replaced with any other LLM provider without affecting the system
 * - Single responsibility: communicate with LM Studio API
 * 
 * Configuration:
 * - game.llm.lm-studio.url: LM Studio server URL
 * - game.llm.lm-studio.enabled: Enable/disable this adapter
 * - game.llm.lm-studio.model: Model name to use
 * - game.llm.lm-studio.temperature: Sampling temperature (0.0-2.0)
 * - game.llm.lm-studio.max-tokens: Maximum tokens to generate
 * - game.llm.lm-studio.timeout-seconds: Request timeout
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LmStudioLlmAdapter implements LlmAdapter {

    private final RestTemplate restTemplate;

    @Value("${game.llm.lm-studio.url:http://localhost:1234}")
    private String lmStudioUrl;
    
    @Value("${game.llm.lm-studio.enabled:false}")
    private boolean enabled;
    
    @Value("${game.llm.lm-studio.model:openai/gpt-oss-20b}")
    private String model;
    
    @Value("${game.llm.lm-studio.temperature:0.7}")
    private double temperature;
    
    @Value("${game.llm.lm-studio.max-tokens:10}")
    private int maxTokens;
    
    @Value("${game.llm.lm-studio.timeout-seconds:10}")
    private int timeoutSeconds;
    
    @Value("${game.llm.lm-studio.prompts.system}")
    private String systemPrompt;
    
    @Value("${game.llm.lm-studio.prompts.user-template}")
    private String userPromptTemplate;

    // Cache for availability check to avoid hammering the service
    private volatile Instant lastAvailabilityCheck = Instant.MIN;
    private volatile boolean lastAvailabilityResult = false;
    private static final Duration AVAILABILITY_CACHE_DURATION = Duration.ofSeconds(30);

    @Override
    public String generateWord(String theme) {
        log.info("LM Studio adapter generating word for theme: {}", theme);
        
        if (!isAvailable()) {
            throw new IllegalStateException("LM Studio is not available. Check configuration and service status.");
        }
        
        try {
            // Build the prompt for Russian word generation using configured templates
            String userPrompt = String.format(userPromptTemplate, theme);
            
            // Create the request
            ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                List.of(
                    new Message("system", systemPrompt),
                    new Message("user", userPrompt)
                ),
                temperature,
                maxTokens
            );
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ChatCompletionRequest> httpEntity = new HttpEntity<>(request, headers);
            
            // Make the request
            String endpoint = lmStudioUrl + "/v1/chat/completions";
            log.debug("Sending request to LM Studio: {}", endpoint);
            
            ResponseEntity<ChatCompletionResponse> response = restTemplate.postForEntity(
                endpoint,
                httpEntity,
                ChatCompletionResponse.class
            );
            
            // Extract the word from response
            ChatCompletionResponse responseBody = response.getBody();
            if (responseBody == null || responseBody.getChoices() == null || responseBody.getChoices().isEmpty()) {
                log.error("LM Studio returned empty response");
                throw new IllegalStateException("LM Studio returned empty response");
            }
            
            String generatedText = responseBody.getChoices().get(0).getMessage().getContent();
            if (generatedText == null || generatedText.isBlank()) {
                log.error("LM Studio returned empty text content");
                throw new IllegalStateException("LM Studio returned empty text content");
            }
            
            // Basic validation: trim whitespace
            String word = generatedText.trim();
            log.info("LM Studio generated word: {} for theme: {}", word, theme);
            
            return word;
            
        } catch (RestClientException e) {
            log.error("Failed to communicate with LM Studio: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to communicate with LM Studio: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during word generation: {}", e.getMessage(), e);
            throw new IllegalStateException("Unexpected error during word generation: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        // Check basic configuration first
        if (!enabled) {
            log.debug("LM Studio adapter is disabled in configuration");
            return false;
        }
        
        if (lmStudioUrl == null || lmStudioUrl.isBlank()) {
            log.warn("LM Studio URL is not configured");
            return false;
        }
        
        // Use cached result if available and recent
        Instant now = Instant.now();
        if (Duration.between(lastAvailabilityCheck, now).compareTo(AVAILABILITY_CACHE_DURATION) < 0) {
            log.debug("Using cached availability result: {}", lastAvailabilityResult);
            return lastAvailabilityResult;
        }
        
        // Perform actual health check by pinging the models endpoint
        try {
            String endpoint = lmStudioUrl + "/v1/models";
            log.debug("Checking LM Studio availability at: {}", endpoint);
            
            ResponseEntity<String> response = restTemplate.getForEntity(endpoint, String.class);
            boolean isAvailable = response.getStatusCode().is2xxSuccessful();
            
            // Update cache
            lastAvailabilityCheck = now;
            lastAvailabilityResult = isAvailable;
            
            log.debug("LM Studio availability check result: {}", isAvailable);
            return isAvailable;
            
        } catch (RestClientException e) {
            log.warn("LM Studio is not available: {}", e.getMessage());
            
            // Update cache with negative result
            lastAvailabilityCheck = now;
            lastAvailabilityResult = false;
            
            return false;
        }
    }

    @Override
    public String getType() {
        return "lm-studio";
    }
    
    // ==================== Inner DTO Classes for OpenAI-compatible API ====================
    
    /**
     * Request DTO for OpenAI-compatible chat completion API
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class ChatCompletionRequest {
        private String model;
        private List<Message> messages;
        private Double temperature;
        @JsonProperty("max_tokens")
        private Integer maxTokens;
    }
    
    /**
     * Message DTO for chat completion requests
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Message {
        private String role;
        private String content;
    }
    
    /**
     * Response DTO for OpenAI-compatible chat completion API
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class ChatCompletionResponse {
        private List<Choice> choices;
    }
    
    /**
     * Choice DTO representing a completion choice in the response
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Choice {
        private Message message;
        private Integer index;
    }
}
