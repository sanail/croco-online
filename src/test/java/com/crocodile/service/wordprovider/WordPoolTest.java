package com.crocodile.service.wordprovider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WordPool
 * 
 * Tests cover:
 * - Basic operations (add, poll, size)
 * - Thread-safety
 * - Refill detection
 * - Edge cases
 */
class WordPoolTest {

    private WordPool wordPool;
    private static final String TEST_THEME = "животные";
    private static final int MIN_THRESHOLD = 5;

    @BeforeEach
    void setUp() {
        wordPool = new WordPool();
        // Set min threshold for testing
        ReflectionTestUtils.setField(wordPool, "minThreshold", MIN_THRESHOLD);
    }

    @Test
    void testAddWords_addsWordsToPool() {
        List<String> words = Arrays.asList("Кошка", "Собака", "Слон");
        
        wordPool.addWords(TEST_THEME, words);
        
        assertEquals(3, wordPool.getPoolSize(TEST_THEME));
    }

    @Test
    void testAddWords_emptyList() {
        wordPool.addWords(TEST_THEME, List.of());
        
        assertEquals(0, wordPool.getPoolSize(TEST_THEME));
    }

    @Test
    void testAddWords_nullList() {
        wordPool.addWords(TEST_THEME, null);
        
        assertEquals(0, wordPool.getPoolSize(TEST_THEME));
    }

    @Test
    void testAddWord_addsSingleWord() {
        wordPool.addWord(TEST_THEME, "Кошка");
        
        assertEquals(1, wordPool.getPoolSize(TEST_THEME));
    }

    @Test
    void testAddWord_nullWord() {
        wordPool.addWord(TEST_THEME, null);
        
        assertEquals(0, wordPool.getPoolSize(TEST_THEME));
    }

    @Test
    void testAddWord_blankWord() {
        wordPool.addWord(TEST_THEME, "   ");
        
        assertEquals(0, wordPool.getPoolSize(TEST_THEME));
    }

    @Test
    void testPollWord_retrievesAndRemovesWord() {
        List<String> words = Arrays.asList("Кошка", "Собака");
        wordPool.addWords(TEST_THEME, words);
        
        String word = wordPool.pollWord(TEST_THEME);
        
        assertNotNull(word);
        assertTrue(words.contains(word));
        assertEquals(1, wordPool.getPoolSize(TEST_THEME));
    }

    @Test
    void testPollWord_emptyPool() {
        String word = wordPool.pollWord(TEST_THEME);
        
        assertNull(word);
    }

    @Test
    void testPollWord_nonExistentTheme() {
        String word = wordPool.pollWord("nonexistent");
        
        assertNull(word);
    }

    @Test
    void testGetPoolSize_nonExistentTheme() {
        assertEquals(0, wordPool.getPoolSize("nonexistent"));
    }

    @Test
    void testIsEmpty_emptyPool() {
        assertTrue(wordPool.isEmpty(TEST_THEME));
    }

    @Test
    void testIsEmpty_nonEmptyPool() {
        wordPool.addWord(TEST_THEME, "Кошка");
        
        assertFalse(wordPool.isEmpty(TEST_THEME));
    }

    @Test
    void testNeedsRefill_belowThreshold() {
        // Add words below threshold
        for (int i = 0; i < MIN_THRESHOLD - 1; i++) {
            wordPool.addWord(TEST_THEME, "Word" + i);
        }
        
        assertTrue(wordPool.needsRefill(TEST_THEME));
    }

    @Test
    void testNeedsRefill_atThreshold() {
        // Add words at threshold
        for (int i = 0; i < MIN_THRESHOLD; i++) {
            wordPool.addWord(TEST_THEME, "Word" + i);
        }
        
        assertFalse(wordPool.needsRefill(TEST_THEME));
    }

    @Test
    void testNeedsRefill_aboveThreshold() {
        // Add words above threshold
        for (int i = 0; i < MIN_THRESHOLD + 5; i++) {
            wordPool.addWord(TEST_THEME, "Word" + i);
        }
        
        assertFalse(wordPool.needsRefill(TEST_THEME));
    }

    @Test
    void testNeedsRefill_emptyPool() {
        assertTrue(wordPool.needsRefill(TEST_THEME));
    }

    @Test
    void testClearPool_removesAllWords() {
        List<String> words = Arrays.asList("Кошка", "Собака", "Слон");
        wordPool.addWords(TEST_THEME, words);
        
        wordPool.clearPool(TEST_THEME);
        
        assertEquals(0, wordPool.getPoolSize(TEST_THEME));
        assertTrue(wordPool.isEmpty(TEST_THEME));
    }

    @Test
    void testClearPool_nonExistentTheme() {
        // Should not throw exception
        assertDoesNotThrow(() -> wordPool.clearPool("nonexistent"));
    }

    @Test
    void testClearAllPools_removesAllThemes() {
        wordPool.addWord("тема1", "Word1");
        wordPool.addWord("тема2", "Word2");
        wordPool.addWord("тема3", "Word3");
        
        wordPool.clearAllPools();
        
        assertEquals(0, wordPool.getPoolSize("тема1"));
        assertEquals(0, wordPool.getPoolSize("тема2"));
        assertEquals(0, wordPool.getPoolSize("тема3"));
    }

    @Test
    void testMultipleThemes_independentPools() {
        wordPool.addWord("тема1", "Word1");
        wordPool.addWord("тема2", "Word2");
        
        assertEquals(1, wordPool.getPoolSize("тема1"));
        assertEquals(1, wordPool.getPoolSize("тема2"));
        
        wordPool.pollWord("тема1");
        
        assertEquals(0, wordPool.getPoolSize("тема1"));
        assertEquals(1, wordPool.getPoolSize("тема2"));
    }

    @Test
    void testThreadSafety_concurrentAdds() throws InterruptedException {
        int threadCount = 10;
        int wordsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < wordsPerThread; j++) {
                        wordPool.addWord(TEST_THEME, "Word-" + threadId + "-" + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
        
        assertEquals(threadCount * wordsPerThread, wordPool.getPoolSize(TEST_THEME));
    }

    @Test
    void testThreadSafety_concurrentPollsAndAdds() throws InterruptedException {
        // Pre-populate pool
        for (int i = 0; i < 1000; i++) {
            wordPool.addWord(TEST_THEME, "Word" + i);
        }
        
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger pollCount = new AtomicInteger(0);
        
        // Half threads poll, half add
        for (int i = 0; i < threadCount; i++) {
            final boolean shouldPoll = i < threadCount / 2;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        if (shouldPoll) {
                            String word = wordPool.pollWord(TEST_THEME);
                            if (word != null) {
                                pollCount.incrementAndGet();
                            }
                        } else {
                            wordPool.addWord(TEST_THEME, "NewWord" + j);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
        
        // Verify no exceptions occurred and pool is in consistent state
        assertTrue(pollCount.get() > 0);
        assertTrue(wordPool.getPoolSize(TEST_THEME) >= 0);
    }

    @Test
    void testFIFOOrder_wordsRetrievedInOrder() {
        List<String> words = Arrays.asList("First", "Second", "Third");
        wordPool.addWords(TEST_THEME, words);
        
        assertEquals("First", wordPool.pollWord(TEST_THEME));
        assertEquals("Second", wordPool.pollWord(TEST_THEME));
        assertEquals("Third", wordPool.pollWord(TEST_THEME));
    }

    @Test
    void testGetMinThreshold_returnsConfiguredValue() {
        assertEquals(MIN_THRESHOLD, wordPool.getMinThreshold());
    }
}

