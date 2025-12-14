package com.crocodile.service.wordprovider.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Yandex GPT LLM Adapter
 * 
 * Integrates with Yandex GPT API for word generation.
 * Requires API key and folder ID from Yandex Cloud.
 * 
 * Configuration:
 * - game.llm.yandex-gpt.api-key: Yandex Cloud API key
 * - game.llm.yandex-gpt.folder-id: Yandex Cloud folder ID
 * - game.llm.yandex-gpt.enabled: Enable/disable this adapter
 */
@Component
@Slf4j
public class YandexGptLlmAdapter implements LlmAdapter {

    @Value("${game.llm.yandex-gpt.api-key:}")
    private String apiKey;
    
    @Value("${game.llm.yandex-gpt.folder-id:}")
    private String folderId;
    
    @Value("${game.llm.yandex-gpt.enabled:false}")
    private boolean enabled;
    
    @Value("${game.llm.yandex-gpt.prompts.system}")
    private String systemPrompt;
    
    @Value("${game.llm.yandex-gpt.prompts.user-template}")
    private String userPromptTemplate;

    @Override
    public String generateWord(String theme) {
        log.info("Yandex GPT adapter generating word for theme: {}", theme);
        
        if (!isAvailable()) {
            throw new IllegalStateException("Yandex GPT is not available. Check configuration (API key and folder ID).");
        }
        
        // TODO: Implement actual Yandex GPT API integration
        // This would involve:
        // 1. Create HTTP client request to Yandex GPT endpoint
        // 2. Format request with API key, folder ID, and configured prompts:
        //    - System prompt: systemPrompt (from configuration)
        //    - User prompt: String.format(userPromptTemplate, theme)
        // 3. Parse response and extract the word
        // 4. Validate the word
        
        log.warn("Yandex GPT integration not yet implemented, using placeholder");
        throw new UnsupportedOperationException("Yandex GPT integration not implemented yet");
    }

    @Override
    public boolean isAvailable() {
        if (!enabled) {
            log.debug("Yandex GPT adapter is disabled in configuration");
            return false;
        }
        
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Yandex GPT API key is not configured");
            return false;
        }
        
        if (folderId == null || folderId.isBlank()) {
            log.warn("Yandex GPT folder ID is not configured");
            return false;
        }
        
        log.debug("Yandex GPT adapter is available - enabled: {}, has API key: {}, has folder ID: {}", 
                  enabled, !apiKey.isBlank(), !folderId.isBlank());
        
        return true;
    }

    @Override
    public String getType() {
        return "yandex-gpt";
    }
}
