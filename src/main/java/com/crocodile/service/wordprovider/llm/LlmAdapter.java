package com.crocodile.service.wordprovider.llm;

import java.util.List;

/**
 * LlmAdapter - Black Box Interface for LLM Integration
 * 
 * This interface abstracts away the details of specific LLM providers.
 * Any LLM service (LM Studio, Yandex GPT, OpenAI, etc.) can be integrated
 * by implementing this interface.
 * 
 * Responsibilities:
 * - Generate words using AI/LLM for a given theme
 * - Support batch generation for optimization
 * - Report availability status
 * - Identify the LLM provider type
 */
public interface LlmAdapter {
    
    /**
     * Generate multiple words for the given theme using LLM (batch generation)
     * This method is more efficient for generating multiple words at once,
     * reducing the number of API calls to the LLM service.
     * 
     * @param theme the theme for word generation
     * @param count the number of words to generate
     * @return list of generated words
     */
    List<String> generateWords(String theme, int count);
    
    /**
     * Check if this LLM adapter is available and ready to use
     * This allows graceful degradation when a service is unavailable
     * @return true if the adapter is configured and available, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Get the type identifier of this LLM adapter
     * @return adapter type (e.g., "lm-studio", "yandex-gpt")
     */
    String getType();
}
