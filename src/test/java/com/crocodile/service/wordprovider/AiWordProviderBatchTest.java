package com.crocodile.service.wordprovider;

import com.crocodile.service.wordprovider.llm.LlmAdapter;
import com.crocodile.service.wordprovider.llm.LlmAdapterFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for AiWordProvider with batch generation and pooling
 * 
 * Tests cover:
 * - Word retrieval from pool
 * - Synchronous generation when pool is empty
 * - Pool refill logic
 * - Error handling
 */
@ExtendWith(MockitoExtension.class)
class AiWordProviderBatchTest {

    @Mock
    private LlmAdapterFactory llmAdapterFactory;
    
    @Mock
    private LlmAdapter llmAdapter;
    
    @Mock
    private WordPool wordPool;
    
    private AiWordProvider aiWordProvider;
    
    private static final String TEST_THEME = "животные";
    private static final int BATCH_SIZE = 20;
    private static final int INITIAL_SIZE = 10;

    @BeforeEach
    void setUp() {
        aiWordProvider = new AiWordProvider(llmAdapterFactory, wordPool);
        
        // Set configuration values
        ReflectionTestUtils.setField(aiWordProvider, "batchSize", BATCH_SIZE);
        ReflectionTestUtils.setField(aiWordProvider, "initialSize", INITIAL_SIZE);
        
        // Default mock behavior - use lenient to avoid UnnecessaryStubbingException
        lenient().when(llmAdapterFactory.getActiveAdapter()).thenReturn(llmAdapter);
        lenient().when(llmAdapter.getType()).thenReturn("test-adapter");
    }

    @Test
    void testGenerateWord_retrievesFromPool() {
        // Pool has a word available
        when(wordPool.pollWord(TEST_THEME)).thenReturn("Кошка");
        when(wordPool.needsRefill(TEST_THEME)).thenReturn(false);
        
        String word = aiWordProvider.generateWord(TEST_THEME);
        
        assertEquals("Кошка", word);
        verify(wordPool).pollWord(TEST_THEME);
        verify(wordPool).needsRefill(TEST_THEME);
        verifyNoInteractions(llmAdapter); // Should not call LLM if pool has words
    }

    @Test
    void testGenerateWord_poolEmptyGeneratesSynchronously() {
        // Pool is empty
        when(wordPool.pollWord(TEST_THEME)).thenReturn(null);
        
        // LLM generates words
        List<String> generatedWords = Arrays.asList(
            "Кошка", "Собака", "Слон", "Тигр", "Лев",
            "Медведь", "Волк", "Лиса", "Заяц", "Белка"
        );
        when(llmAdapter.generateWords(TEST_THEME, INITIAL_SIZE)).thenReturn(generatedWords);
        when(wordPool.needsRefill(TEST_THEME)).thenReturn(true);
        
        String word = aiWordProvider.generateWord(TEST_THEME);
        
        assertEquals("Кошка", word); // First word from the list
        verify(llmAdapter).generateWords(TEST_THEME, INITIAL_SIZE);
        verify(wordPool).addWords(eq(TEST_THEME), anyList()); // Adds remaining words to pool
    }

    @Test
    void testGenerateWord_singleWordGenerated() {
        // Pool is empty
        when(wordPool.pollWord(TEST_THEME)).thenReturn(null);
        
        // LLM generates only one word
        List<String> generatedWords = List.of("Кошка");
        when(llmAdapter.generateWords(TEST_THEME, INITIAL_SIZE)).thenReturn(generatedWords);
        when(wordPool.needsRefill(TEST_THEME)).thenReturn(true);
        
        String word = aiWordProvider.generateWord(TEST_THEME);
        
        assertEquals("Кошка", word);
        verify(llmAdapter).generateWords(TEST_THEME, INITIAL_SIZE);
        verify(wordPool, never()).addWords(any(), anyList()); // No remaining words to add
    }

    @Test
    void testGenerateWord_triggersRefillWhenBelowThreshold() {
        // Pool has a word but needs refill
        when(wordPool.pollWord(TEST_THEME)).thenReturn("Кошка");
        when(wordPool.needsRefill(TEST_THEME)).thenReturn(true);
        
        String word = aiWordProvider.generateWord(TEST_THEME);
        
        assertEquals("Кошка", word);
        verify(wordPool).needsRefill(TEST_THEME);
        // Note: We can't easily verify async method call in unit test,
        // but we verify the logic is triggered
    }

    @Test
    void testGenerateWord_doesNotRefillWhenAboveThreshold() {
        // Pool has a word and doesn't need refill
        when(wordPool.pollWord(TEST_THEME)).thenReturn("Кошка");
        when(wordPool.needsRefill(TEST_THEME)).thenReturn(false);
        
        String word = aiWordProvider.generateWord(TEST_THEME);
        
        assertEquals("Кошка", word);
        verify(wordPool).needsRefill(TEST_THEME);
        verifyNoInteractions(llmAdapter);
    }

    @Test
    void testGenerateWord_llmReturnsEmpty() {
        // Pool is empty
        when(wordPool.pollWord(TEST_THEME)).thenReturn(null);
        
        // LLM returns empty list
        when(llmAdapter.generateWords(TEST_THEME, INITIAL_SIZE)).thenReturn(List.of());
        
        assertThrows(IllegalStateException.class, () -> {
            aiWordProvider.generateWord(TEST_THEME);
        });
    }

    @Test
    void testGenerateWord_llmReturnsNull() {
        // Pool is empty
        when(wordPool.pollWord(TEST_THEME)).thenReturn(null);
        
        // LLM returns null
        when(llmAdapter.generateWords(TEST_THEME, INITIAL_SIZE)).thenReturn(null);
        
        assertThrows(IllegalStateException.class, () -> {
            aiWordProvider.generateWord(TEST_THEME);
        });
    }

    @Test
    void testGenerateWord_llmAdapterNotAvailable() {
        // Pool is empty
        when(wordPool.pollWord(TEST_THEME)).thenReturn(null);
        
        // LLM adapter is not available
        when(llmAdapterFactory.getActiveAdapter())
            .thenThrow(new IllegalStateException("No LLM adapter available"));
        
        assertThrows(IllegalStateException.class, () -> {
            aiWordProvider.generateWord(TEST_THEME);
        });
    }

    @Test
    void testGenerateWord_llmThrowsException() {
        // Pool is empty
        when(wordPool.pollWord(TEST_THEME)).thenReturn(null);
        
        // LLM throws exception
        when(llmAdapter.generateWords(TEST_THEME, INITIAL_SIZE))
            .thenThrow(new RuntimeException("LLM service error"));
        
        assertThrows(RuntimeException.class, () -> {
            aiWordProvider.generateWord(TEST_THEME);
        });
    }

    @Test
    void testRefillPoolAsync_successfulRefill() {
        // Prepare mock for async refill
        List<String> batchWords = Arrays.asList(
            "Word1", "Word2", "Word3", "Word4", "Word5",
            "Word6", "Word7", "Word8", "Word9", "Word10",
            "Word11", "Word12", "Word13", "Word14", "Word15",
            "Word16", "Word17", "Word18", "Word19", "Word20"
        );
        when(llmAdapter.generateWords(TEST_THEME, BATCH_SIZE)).thenReturn(batchWords);
        
        // Call refill directly (synchronously for testing)
        aiWordProvider.refillPoolAsync(TEST_THEME);
        
        verify(llmAdapter).generateWords(TEST_THEME, BATCH_SIZE);
        verify(wordPool).addWords(TEST_THEME, batchWords);
    }

    @Test
    void testRefillPoolAsync_llmReturnsEmpty() {
        // LLM returns empty list
        when(llmAdapter.generateWords(TEST_THEME, BATCH_SIZE)).thenReturn(List.of());
        
        // Should handle gracefully without throwing
        assertDoesNotThrow(() -> {
            aiWordProvider.refillPoolAsync(TEST_THEME);
        });
        
        verify(wordPool, never()).addWords(any(), anyList());
    }

    @Test
    void testRefillPoolAsync_llmThrowsException() {
        // LLM throws exception
        when(llmAdapter.generateWords(TEST_THEME, BATCH_SIZE))
            .thenThrow(new RuntimeException("LLM service error"));
        
        // Should handle gracefully without throwing (logs error)
        assertDoesNotThrow(() -> {
            aiWordProvider.refillPoolAsync(TEST_THEME);
        });
        
        verify(wordPool, never()).addWords(any(), anyList());
    }

    @Test
    void testGetType_returnsAi() {
        assertEquals("ai", aiWordProvider.getType());
    }

    @Test
    void testGenerateWord_multipleThemesIndependent() {
        String theme1 = "животные";
        String theme2 = "профессии";
        
        when(wordPool.pollWord(theme1)).thenReturn("Кошка");
        when(wordPool.pollWord(theme2)).thenReturn("Врач");
        when(wordPool.needsRefill(anyString())).thenReturn(false);
        
        String word1 = aiWordProvider.generateWord(theme1);
        String word2 = aiWordProvider.generateWord(theme2);
        
        assertEquals("Кошка", word1);
        assertEquals("Врач", word2);
        verify(wordPool).pollWord(theme1);
        verify(wordPool).pollWord(theme2);
    }

    @Test
    void testGenerateWord_addsRemainingWordsToPool() {
        // Pool is empty
        when(wordPool.pollWord(TEST_THEME)).thenReturn(null);
        
        // LLM generates 10 words
        List<String> generatedWords = Arrays.asList(
            "Word1", "Word2", "Word3", "Word4", "Word5",
            "Word6", "Word7", "Word8", "Word9", "Word10"
        );
        when(llmAdapter.generateWords(TEST_THEME, INITIAL_SIZE)).thenReturn(generatedWords);
        when(wordPool.needsRefill(TEST_THEME)).thenReturn(true);
        
        String word = aiWordProvider.generateWord(TEST_THEME);
        
        assertEquals("Word1", word);
        
        // Verify that words 2-10 were added to pool
        verify(wordPool).addWords(eq(TEST_THEME), argThat(list -> 
            list.size() == 9 && 
            list.contains("Word2") && 
            list.contains("Word10") &&
            !list.contains("Word1")
        ));
    }
}

