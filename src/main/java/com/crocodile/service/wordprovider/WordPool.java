package com.crocodile.service.wordprovider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * WordPool - Thread-safe pool manager for generated words
 * 
 * This component maintains separate pools of pre-generated words for each theme.
 * It supports concurrent access and provides methods to add, retrieve, and monitor
 * the pool state.
 * 
 * Key Features:
 * - Thread-safe operations using ConcurrentHashMap and ConcurrentLinkedQueue
 * - Separate pool for each theme
 * - Configurable threshold for refill detection
 * 
 * Responsibilities:
 * - Store words in theme-specific queues
 * - Provide thread-safe retrieval of words
 * - Monitor pool sizes and indicate when refill is needed
 */
@Component
@Slf4j
public class WordPool {

    private final ConcurrentHashMap<String, Queue<String>> pools = new ConcurrentHashMap<>();
    
    @Value("${game.llm.word-pool.min-threshold:5}")
    private int minThreshold;

    /**
     * Poll (retrieve and remove) a word from the pool for the given theme
     * 
     * @param theme the theme to get a word for
     * @return a word from the pool, or null if pool is empty
     */
    public String pollWord(String theme) {
        Queue<String> pool = pools.get(theme);
        if (pool == null || pool.isEmpty()) {
            log.debug("Word pool for theme '{}' is empty or doesn't exist", theme);
            return null;
        }
        
        String word = pool.poll();
        log.debug("Polled word '{}' from pool for theme '{}'. Remaining: {}", 
                  word, theme, pool.size());
        return word;
    }

    /**
     * Add multiple words to the pool for the given theme
     * 
     * @param theme the theme to add words for
     * @param words list of words to add
     */
    public void addWords(String theme, List<String> words) {
        if (words == null || words.isEmpty()) {
            log.warn("Attempted to add empty word list for theme '{}'", theme);
            return;
        }
        
        Queue<String> pool = pools.computeIfAbsent(theme, k -> {
            log.debug("Creating new word pool for theme '{}'", theme);
            return new ConcurrentLinkedQueue<>();
        });
        
        pool.addAll(words);
        log.info("Added {} words to pool for theme '{}'. New pool size: {}", 
                 words.size(), theme, pool.size());
    }

    /**
     * Add a single word to the pool for the given theme
     * 
     * @param theme the theme to add the word for
     * @param word the word to add
     */
    public void addWord(String theme, String word) {
        if (word == null || word.isBlank()) {
            log.warn("Attempted to add empty word for theme '{}'", theme);
            return;
        }
        
        Queue<String> pool = pools.computeIfAbsent(theme, k -> {
            log.debug("Creating new word pool for theme '{}'", theme);
            return new ConcurrentLinkedQueue<>();
        });
        
        pool.add(word);
        log.debug("Added word '{}' to pool for theme '{}'. New pool size: {}", 
                  word, theme, pool.size());
    }

    /**
     * Get the current size of the pool for the given theme
     * 
     * @param theme the theme to check
     * @return the number of words in the pool, or 0 if pool doesn't exist
     */
    public int getPoolSize(String theme) {
        Queue<String> pool = pools.get(theme);
        return pool == null ? 0 : pool.size();
    }

    /**
     * Check if the pool for the given theme needs refilling
     * A pool needs refilling when its size falls below the configured threshold
     * 
     * @param theme the theme to check
     * @return true if the pool size is below the threshold, false otherwise
     */
    public boolean needsRefill(String theme) {
        int size = getPoolSize(theme);
        boolean needs = size < minThreshold;
        
        if (needs) {
            log.debug("Pool for theme '{}' needs refill. Current size: {}, threshold: {}", 
                      theme, size, minThreshold);
        }
        
        return needs;
    }

    /**
     * Check if the pool for the given theme is empty
     * 
     * @param theme the theme to check
     * @return true if the pool is empty or doesn't exist, false otherwise
     */
    public boolean isEmpty(String theme) {
        return getPoolSize(theme) == 0;
    }

    /**
     * Clear all words from the pool for the given theme
     * 
     * @param theme the theme to clear
     */
    public void clearPool(String theme) {
        Queue<String> pool = pools.get(theme);
        if (pool != null) {
            int previousSize = pool.size();
            pool.clear();
            log.info("Cleared word pool for theme '{}'. Removed {} words", theme, previousSize);
        }
    }

    /**
     * Clear all pools
     */
    public void clearAllPools() {
        int totalWords = pools.values().stream().mapToInt(Queue::size).sum();
        pools.clear();
        log.info("Cleared all word pools. Removed {} total words", totalWords);
    }

    /**
     * Get the minimum threshold for pool refill
     * 
     * @return the minimum threshold
     */
    public int getMinThreshold() {
        return minThreshold;
    }
}

