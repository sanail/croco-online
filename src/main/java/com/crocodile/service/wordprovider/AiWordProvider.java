package com.crocodile.service.wordprovider;

import com.crocodile.service.wordprovider.llm.LlmAdapter;
import com.crocodile.service.wordprovider.llm.LlmAdapterFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AiWordProvider - WordProvider implementation using AI/LLM
 * 
 * This provider generates words using Large Language Models (LLMs).
 * The actual LLM used (LM Studio, Yandex GPT, etc.) is determined by
 * configuration and managed by LlmAdapterFactory.
 * 
 * Black Box Principle:
 * - Hides LLM selection complexity from the rest of the system
 * - Provides the same WordProvider interface as DatabaseWordProvider
 * - Can be swapped with other WordProvider implementations without changes
 * 
 * Responsibilities:
 * - Delegate word generation to the active LLM adapter
 * - Handle errors gracefully
 * - Log operations for debugging
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiWordProvider implements WordProvider {

    private final LlmAdapterFactory llmAdapterFactory;

    @Override
    public String generateWord(String theme) {
        log.info("AI provider generating word for theme: {}", theme);
        
        try {
            // Get the active LLM adapter from factory
            // The factory handles configuration checking and availability validation
            LlmAdapter adapter = llmAdapterFactory.getActiveAdapter();
            
            log.debug("Using LLM adapter: {}", adapter.getType());
            
            // Delegate word generation to the LLM adapter
            String word = adapter.generateWord(theme);
            
            log.info("Successfully generated word using AI: {} for theme: {}", word, theme);
            return word;
            
        } catch (IllegalStateException e) {
            // This happens when no LLM adapter is available or configured
            log.error("Failed to generate word using AI: {}", e.getMessage());
            throw new IllegalStateException(
                "AI word generation is not available. " + e.getMessage(), e
            );
        } catch (Exception e) {
            // Catch any other errors from LLM adapters
            log.error("Unexpected error during AI word generation for theme: {}", theme, e);
            throw new RuntimeException(
                "Failed to generate word using AI: " + e.getMessage(), e
            );
        }
    }

    @Override
    public String getType() {
        return "ai";
    }
}
