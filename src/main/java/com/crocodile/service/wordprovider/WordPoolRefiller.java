package com.crocodile.service.wordprovider;

import com.crocodile.service.wordprovider.llm.LlmAdapter;
import com.crocodile.service.wordprovider.llm.LlmAdapterFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WordPoolRefiller - Handles asynchronous word pool refilling
 * 
 * This component is responsible for refilling word pools in the background
 * using async task execution. It prevents duplicate refill operations and
 * handles errors gracefully.
 * 
 * Note: Uses self-injection to ensure @Async methods are called through Spring proxy
 */
@Component
@Slf4j
public class WordPoolRefiller {

    private final LlmAdapterFactory llmAdapterFactory;
    private final WordPool wordPool;
    private final WordPoolRefiller self;
    
    @Value("${game.llm.word-pool.batch-size:20}")
    private int batchSize;
    
    // Track ongoing refill operations per theme to prevent duplicate refills
    private final ConcurrentHashMap<String, AtomicBoolean> refillInProgress = new ConcurrentHashMap<>();

    public WordPoolRefiller(LlmAdapterFactory llmAdapterFactory, 
                            WordPool wordPool,
                            @Lazy WordPoolRefiller self) {
        this.llmAdapterFactory = llmAdapterFactory;
        this.wordPool = wordPool;
        this.self = self;
    }

    /**
     * Trigger asynchronous pool refill if not already in progress
     * 
     * @param theme the theme to refill the pool for
     */
    public void triggerAsyncRefill(String theme) {
        AtomicBoolean refilling = refillInProgress.computeIfAbsent(theme, k -> new AtomicBoolean(false));
        
        // Only start refill if not already in progress
        if (refilling.compareAndSet(false, true)) {
            log.debug("Starting async refill for theme '{}'", theme);
            // Call through proxy to ensure @Async works
            self.refillPoolAsync(theme);
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
}
