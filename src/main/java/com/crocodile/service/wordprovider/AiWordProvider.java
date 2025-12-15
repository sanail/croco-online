package com.crocodile.service.wordprovider;

import com.crocodile.service.wordprovider.llm.LlmAdapter;
import com.crocodile.service.wordprovider.llm.LlmAdapterFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;

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
 * - Async refill: Automatically refills the pool in the background
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
    
    @Value("${game.llm.word-pool.batch-size:20}")
    private int batchSize;
    
    @Value("${game.llm.word-pool.initial-size:10}")
    private int initialSize;
    
    // Track ongoing refill operations per theme to prevent duplicate refills
    private final ConcurrentHashMap<String, AtomicBoolean> refillInProgress = new ConcurrentHashMap<>();

    public AiWordProvider(LlmAdapterFactory llmAdapterFactory, WordPool wordPool) {
        this.llmAdapterFactory = llmAdapterFactory;
        this.wordPool = wordPool;
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
                    triggerAsyncRefill(theme);
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
            word = words.get(0);
            
            if (words.size() > 1) {
                List<String> remainingWords = words.subList(1, words.size());
                wordPool.addWords(theme, remainingWords);
                log.info("Added {} words to pool for theme '{}' after initial generation", 
                         remainingWords.size(), theme);
            }
            
            // Trigger async refill to fill up the pool
            if (wordPool.needsRefill(theme)) {
                triggerAsyncRefill(theme);
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

    /**
     * Trigger asynchronous pool refill if not already in progress
     * 
     * @param theme the theme to refill the pool for
     */
    private void triggerAsyncRefill(String theme) {
        AtomicBoolean refilling = refillInProgress.computeIfAbsent(theme, k -> new AtomicBoolean(false));
        
        // Only start refill if not already in progress
        if (refilling.compareAndSet(false, true)) {
            log.debug("Starting async refill for theme '{}'", theme);
            refillPoolAsync(theme);
        } else {
            log.debug("Refill already in progress for theme '{}', skipping", theme);
        }
    }

    /**
     * Asynchronously refill the word pool for the given theme
     * This method runs in a background thread managed by wordPoolTaskExecutor
     * 
     * @param theme the theme to refill the pool for
     */
    @Async("wordPoolTaskExecutor")
    public void refillPoolAsync(String theme) {
        try {
            log.info("Async refill started for theme '{}'", theme);
            
            LlmAdapter adapter = llmAdapterFactory.getActiveAdapter();
            
            // Generate a batch of words
            List<String> words = adapter.generateWords(theme, batchSize);
            
            if (words != null && !words.isEmpty()) {
                wordPool.addWords(theme, words);
                log.info("Async refill completed for theme '{}': added {} words to pool", 
                         theme, words.size());
            } else {
                log.warn("Async refill for theme '{}' returned no words", theme);
            }
            
        } catch (Exception e) {
            log.error("Error during async pool refill for theme '{}': {}", theme, e.getMessage(), e);
        } finally {
            // Mark refill as complete
            AtomicBoolean refilling = refillInProgress.get(theme);
            if (refilling != null) {
                refilling.set(false);
            }
        }
    }

    @Override
    public String getType() {
        return "ai";
    }
}
