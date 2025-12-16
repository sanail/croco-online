package com.crocodile.service.wordprovider.llm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Yandex GPT LLM Adapter
 * 
 * Integrates with Yandex GPT API for word generation using IAM token authentication.
 * Uses JWT-based authentication with automatic token refresh.
 *
 * Configuration:
 * - game.llm.yandex-gpt.authorized-key-path: Path to authorized key JSON file
 * - game.llm.yandex-gpt.folder-id: Yandex Cloud folder ID
 * - game.llm.yandex-gpt.enabled: Enable/disable this adapter
 * - game.llm.yandex-gpt.model: Model name to use
 * - game.llm.yandex-gpt.temperature: Sampling temperature (0.0-1.0)
 * - game.llm.yandex-gpt.max-tokens: Maximum tokens to generate
 * - game.llm.yandex-gpt.url: Yandex Cloud API endpoint
 */
@Component
@Slf4j
public class YandexGptLlmAdapter implements LlmAdapter {

    private final RestTemplate restTemplate;
    private final YandexIamTokenProvider iamTokenProvider;
    
    @Value("${game.llm.yandex-gpt.folder-id:}")
    private String folderId;
    
    @Value("${game.llm.yandex-gpt.enabled:false}")
    private boolean enabled;

    @Value("${game.llm.yandex-gpt.model:yandexgpt-lite}")
    private String model;

    @Value("${game.llm.yandex-gpt.temperature:0.7}")
    private double temperature;

    @Value("${game.llm.yandex-gpt.max-tokens:1000}")
    private int maxTokens;

    @Value("${game.llm.yandex-gpt.url:https://llm.api.cloud.yandex.net/foundationModels/v1/completion}")
    private String yandexGptUrl;
    
    @Value("${game.llm.yandex-gpt.prompts.system}")
    private String systemPrompt;
    
    @Value("${game.llm.yandex-gpt.prompts.user-template}")
    private String userPromptTemplate;

    // Cache for availability check to avoid hammering the service
    private volatile Instant lastAvailabilityCheck = Instant.MIN;
    private volatile boolean lastAvailabilityResult = false;
    private static final Duration AVAILABILITY_CACHE_DURATION = Duration.ofSeconds(30);
    
    // Constructor with optional IAM token provider
    public YandexGptLlmAdapter(RestTemplate restTemplate, 
                                @org.springframework.beans.factory.annotation.Autowired(required = false) 
                                YandexIamTokenProvider iamTokenProvider) {
        this.restTemplate = restTemplate;
        this.iamTokenProvider = iamTokenProvider;
    }

    @Override
    public List<String> generateWords(String theme, int count) {
        log.info("Yandex GPT adapter generating {} words for theme: {}", count, theme);
        
        if (!isAvailable()) {
            throw new IllegalStateException("Yandex GPT is not available. Check configuration (authorized key path and folder ID).");
        }
        
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive, got: " + count);
        }
        
        try {
            // Build the prompt for Russian word generation using configured templates
            String userPrompt = String.format(userPromptTemplate, count, theme);
            
            // Calculate max tokens based on count (roughly 20 tokens per word/phrase + buffer)
            int dynamicMaxTokens = Math.max(maxTokens, count * 20 + 50);
            
            // Strip quotes from folderId if present (common when env vars have quoted values)
            String cleanFolderId = folderId.replaceAll("^\"|\"$", "");
            
            // Construct modelUri in the format: gpt://{folderId}/{model}/latest
            String modelUri = String.format("gpt://%s/%s/latest", cleanFolderId, model);
            
            // Create the request
            YandexCompletionRequest request = new YandexCompletionRequest(
                modelUri,
                new CompletionOptions(false, temperature, dynamicMaxTokens),
                List.of(
                    new Message("system", systemPrompt),
                    new Message("user", userPrompt)
                )
            );
            
            // Set headers with IAM token authorization
            // Yandex Cloud IAM Token format: "Authorization: Bearer <IAM_token>"
            String iamToken = iamTokenProvider.getToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + iamToken);
            HttpEntity<YandexCompletionRequest> httpEntity = new HttpEntity<>(request, headers);
            
            // Make the request
            log.debug("Sending batch request to Yandex GPT: {} (requesting {} words)", yandexGptUrl, count);

            ResponseEntity<YandexCompletionResponse> response = restTemplate.postForEntity(
                yandexGptUrl,
                httpEntity,
                YandexCompletionResponse.class
            );

            // Extract the words from response
            YandexCompletionResponse responseBody = response.getBody();
            if (responseBody == null || responseBody.getResult() == null ||
                responseBody.getResult().getAlternatives() == null ||
                responseBody.getResult().getAlternatives().isEmpty()) {
                log.error("Yandex GPT returned empty response");
                throw new IllegalStateException("Yandex GPT returned empty response");
            }

            Alternative firstAlternative = responseBody.getResult().getAlternatives().get(0);
            if (firstAlternative.getMessage() == null ||
                firstAlternative.getMessage().getText() == null ||
                firstAlternative.getMessage().getText().isBlank()) {
                log.error("Yandex GPT returned empty text content");
                throw new IllegalStateException("Yandex GPT returned empty text content");
            }

            String generatedText = firstAlternative.getMessage().getText();

            // Parse the response - split by newlines and clean up
            List<String> words = new ArrayList<>();
            String[] lines = generatedText.split("\\r?\\n");

            for (String line : lines) {
                String trimmed = line.trim();
                // Skip empty lines and lines that look like numbering
                if (!trimmed.isEmpty() && !trimmed.matches("^\\d+\\.?\\s*$")) {
                    // Remove leading numbers if present (e.g., "1. Кошка" -> "Кошка")
                    String cleaned = trimmed.replaceFirst("^\\d+\\.?\\s*", "");
                    // Remove quotes if present
                    cleaned = cleaned.replaceAll("^\"|\"$", "").trim();

                    if (!cleaned.isEmpty()) {
                        words.add(cleaned);
                    }
                }
            }

            // If we didn't get enough words from newline splitting, try comma separation
            if (words.size() < count && generatedText.contains(",")) {
                words.clear();
                String[] parts = generatedText.split(",");
                for (String part : parts) {
                    String cleaned = part.trim().replaceFirst("^\\d+\\.?\\s*", "");
                    cleaned = cleaned.replaceAll("^\"|\"$", "").trim();
                    if (!cleaned.isEmpty()) {
                        words.add(cleaned);
                    }
                }
            }

            if (words.isEmpty()) {
                log.error("Failed to parse any words from Yandex GPT response: {}", generatedText);
                throw new IllegalStateException("Failed to parse words from Yandex GPT response");
            }

            log.info("Yandex GPT generated {} words for theme: {} (requested: {})", words.size(), theme, count);
            log.debug("Generated words: {}", words);

            return words;

        } catch (RestClientException e) {
            log.error("Failed to communicate with Yandex GPT: {}", e.getMessage(), e);
            
            // If 401 Unauthorized, try to force refresh the IAM token
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                log.warn("Received 401 Unauthorized, forcing IAM token refresh");
                try {
                    iamTokenProvider.forceRefresh();
                } catch (Exception refreshException) {
                    log.error("Failed to refresh IAM token: {}", refreshException.getMessage());
                }
            }
            
            throw new IllegalStateException("Failed to communicate with Yandex GPT: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during batch word generation: {}", e.getMessage(), e);
            throw new IllegalStateException("Unexpected error during batch word generation: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        // Check basic configuration first
        if (!enabled) {
            log.debug("Yandex GPT adapter is disabled in configuration");
            return false;
        }
        
        if (iamTokenProvider == null) {
            log.warn("Yandex GPT IAM token provider is not available. Check authorized key configuration.");
            return false;
        }
        
        if (folderId == null || folderId.isBlank()) {
            log.warn("Yandex GPT folder ID is not configured");
            return false;
        }
        
        // Use cached result if available and recent
        Instant now = Instant.now();
        if (Duration.between(lastAvailabilityCheck, now).compareTo(AVAILABILITY_CACHE_DURATION) < 0) {
            log.debug("Using cached availability result: {}", lastAvailabilityResult);
            return lastAvailabilityResult;
        }

        // For Yandex GPT, we consider it available if properly configured
        // Real connectivity check would require making an actual API call,
        // which we'll do on first generateWords() call
        log.debug("Yandex GPT adapter is available - enabled: {}, has IAM provider: {}, has folder ID: {}", 
                  enabled, (iamTokenProvider != null), !folderId.isBlank());
        
        // Update cache
        lastAvailabilityCheck = now;
        lastAvailabilityResult = true;
        
        return true;
    }

    @Override
    public String getType() {
        return "yandex-gpt";
    }

    // ==================== Inner DTO Classes for Yandex GPT API ====================

    /**
     * Request DTO for Yandex GPT completion API
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class YandexCompletionRequest {
        private String modelUri;
        private CompletionOptions completionOptions;
        private List<Message> messages;
    }

    /**
     * Completion options for Yandex GPT API
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class CompletionOptions {
        private boolean stream;
        private Double temperature;
        private Integer maxTokens;
    }

    /**
     * Message DTO for Yandex GPT requests
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Message {
        private String role;
        private String text;
    }

    /**
     * Response DTO for Yandex GPT completion API
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class YandexCompletionResponse {
        private Result result;
    }

    /**
     * Result DTO containing completion alternatives
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Result {
        private List<Alternative> alternatives;
        private Usage usage;
        private String modelVersion;
    }

    /**
     * Alternative DTO representing a completion choice in the response
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Alternative {
        private Message message;
        private String status;
    }
    
    /**
     * Usage statistics DTO
     * Ignores unknown properties like completionTokensDetails to be forward-compatible
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    static class Usage {
        private String inputTextTokens;
        private String completionTokens;
        private String totalTokens;
    }
}
