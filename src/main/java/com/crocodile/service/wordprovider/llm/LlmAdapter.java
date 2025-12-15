package com.crocodile.service.wordprovider.llm;

/**
 * LlmAdapter - Black Box Interface for LLM Integration
 * 
 * This interface abstracts away the details of specific LLM providers.
 * Any LLM service (LM Studio, Yandex GPT, OpenAI, etc.) can be integrated
 * by implementing this interface.
 * 
 * Responsibilities:
 * - Generate words using AI/LLM for a given theme
 * - Report availability status
 * - Identify the LLM provider type
 */
public interface LlmAdapter {
    
    /**
     * Generate a word for the given theme using LLM
     * @param theme the theme for word generation
     * @return generated word
     */
    String generateWord(String theme);
    
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
