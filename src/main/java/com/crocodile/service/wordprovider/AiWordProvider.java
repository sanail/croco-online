package com.crocodile.service.wordprovider;

import com.crocodile.service.wordprovider.llm.LlmAdapter;
import com.crocodile.service.wordprovider.llm.LlmAdapterFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AiWordProvider - WordProvider implementation using AI/LLM with word pooling
 *
 * This provider generates words using Large Language Models (LLMs) and maintains
 * a pool of pre-generated words for each theme to optimize performance and reduce
 * the number of API calls to the LLM service.
 *
 * Key Features:
 * - Word pooling: Maintains a pool of pre-generated words per theme
 * - Batch generation: Generates multiple words in a single LLM API call
 * - Async refill: Automatically refills the pool in the background via WordPoolRefiller
 * - Thread-safe: Uses WordPool with concurrent data structures
 *
 * Responsibilities:
 * - Provide words from the pool when available
 * - Generate words synchronously when pool is empty
 * - Trigger async pool refill when size falls below threshold
 * - Handle errors gracefully
 * - Log operations for debugging
 */
@Component
@Slf4j
public class AiWordProvider implements WordProvider {

    private final LlmAdapterFactory llmAdapterFactory;
    private final WordPool wordPool;
    private final WordPoolRefiller wordPoolRefiller;
    
    @Value("${game.llm.word-pool.initial-size:10}")
    private int initialSize;

    public AiWordProvider(LlmAdapterFactory llmAdapterFactory, 
                          WordPool wordPool,
                          WordPoolRefiller wordPoolRefiller) {
        this.llmAdapterFactory = llmAdapterFactory;
        this.wordPool = wordPool;
        this.wordPoolRefiller = wordPoolRefiller;
    }

    @Override
    public String generateWord(String theme) {
        log.info("AI provider generating word for theme: {}", theme);
        
        try {
            // Try to get a word from the pool
            String word = wordPool.pollWord(theme);
            
            if (word != null) {
                log.debug("Retrieved word '{}' from pool for theme '{}'", word, theme);
                
                // Check if pool needs refill and trigger async refill
                if (wordPool.needsRefill(theme)) {
                    log.debug("Pool for theme '{}' needs refill, triggering async refill", theme);
                    wordPoolRefiller.triggerAsyncRefill(theme);
                }
                
                return word;
            }
            
            // Pool is empty - need to generate synchronously
            log.info("Pool for theme '{}' is empty, generating initial batch synchronously", theme);
            
            LlmAdapter adapter = llmAdapterFactory.getActiveAdapter();
            log.debug("Using LLM adapter: {}", adapter.getType());
            
            // Generate initial batch of words
            List<String> words = adapter.generateWords(theme, initialSize);
            
            if (words == null || words.isEmpty()) {
                throw new IllegalStateException("LLM returned empty word list");
            }
            
            // Take the first word and add the rest to the pool
            word = words.getFirst();
            
            if (words.size() > 1) {
                List<String> remainingWords = words.subList(1, words.size());
                wordPool.addWords(theme, remainingWords);
                log.info("Added {} words to pool for theme '{}' after initial generation", 
                         remainingWords.size(), theme);
            }
            
            // Trigger async refill to fill up the pool
            if (wordPool.needsRefill(theme)) {
                wordPoolRefiller.triggerAsyncRefill(theme);
            }
            
            log.info("Successfully generated word using AI: '{}' for theme: '{}'", word, theme);
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
