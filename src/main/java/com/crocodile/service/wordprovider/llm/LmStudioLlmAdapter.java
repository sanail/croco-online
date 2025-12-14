package com.crocodile.service.wordprovider.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * LM Studio LLM Adapter
 * 
 * Integrates with LM Studio's local LLM server.
 * LM Studio provides a local OpenAI-compatible API for running LLMs.
 * 
 * Configuration:
 * - game.llm.lm-studio.url: LM Studio server URL
 * - game.llm.lm-studio.enabled: Enable/disable this adapter
 */
@Component
@Slf4j
public class LmStudioLlmAdapter implements LlmAdapter {

    @Value("${game.llm.lm-studio.url:http://localhost:1234}")
    private String lmStudioUrl;
    
    @Value("${game.llm.lm-studio.enabled:false}")
    private boolean enabled;

    @Override
    public String generateWord(String theme) {
        log.info("LM Studio adapter generating word for theme: {}", theme);
        
        if (!isAvailable()) {
            throw new IllegalStateException("LM Studio is not available. Check configuration and service status.");
        }
        
        // TODO: Implement actual LM Studio API integration
        // This would involve:
        // 1. Create HTTP client request to LM Studio endpoint
        // 2. Format prompt: "Generate a single word in Russian for the theme: {theme}"
        // 3. Parse response and extract the word
        // 4. Validate the word (single word, appropriate language)
        
        log.warn("LM Studio integration not yet implemented, using placeholder");
        throw new UnsupportedOperationException("LM Studio integration not implemented yet");
    }

    @Override
    public boolean isAvailable() {
        if (!enabled) {
            log.debug("LM Studio adapter is disabled in configuration");
            return false;
        }
        
        if (lmStudioUrl == null || lmStudioUrl.isBlank()) {
            log.warn("LM Studio URL is not configured");
            return false;
        }
        
        // TODO: Add actual availability check
        // Could ping the LM Studio endpoint to verify it's running
        log.debug("LM Studio adapter availability check - enabled: {}, url: {}", enabled, lmStudioUrl);
        
        return true;
    }

    @Override
    public String getType() {
        return "lm-studio";
    }
}
