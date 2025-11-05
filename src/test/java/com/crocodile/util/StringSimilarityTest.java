package com.crocodile.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringSimilarityTest {

    @Test
    void testLevenshteinDistance_identicalStrings() {
        assertEquals(0, StringSimilarity.levenshteinDistance("hello", "hello"));
    }

    @Test
    void testLevenshteinDistance_oneCharDifference() {
        assertEquals(1, StringSimilarity.levenshteinDistance("hello", "hallo"));
    }

    @Test
    void testLevenshteinDistance_caseInsensitive() {
        assertEquals(0, StringSimilarity.levenshteinDistance("Hello", "hello"));
    }

    @Test
    void testLevenshteinDistance_withSpaces() {
        assertEquals(0, StringSimilarity.levenshteinDistance(" hello ", "hello"));
    }

    @Test
    void testIsSimilar_withinDistance() {
        assertTrue(StringSimilarity.isSimilar("кошка", "кошка", 1));
        assertTrue(StringSimilarity.isSimilar("кошка", "кошка", 1));
    }

    @Test
    void testIsSimilar_exceedsDistance() {
        assertFalse(StringSimilarity.isSimilar("кошка", "собака", 1));
    }

    @Test
    void testIsCorrectGuess_exactMatch() {
        assertTrue(StringSimilarity.isCorrectGuess("кошка", "кошка"));
        assertTrue(StringSimilarity.isCorrectGuess("Кошка", "кошка"));
        assertTrue(StringSimilarity.isCorrectGuess("кошка ", " кошка"));
    }

    @Test
    void testIsCorrectGuess_withTypo() {
        assertTrue(StringSimilarity.isCorrectGuess("крокодил", "крокодил"));
        assertTrue(StringSimilarity.isCorrectGuess("крокодил", "крокодиь")); // 1 char difference
    }

    @Test
    void testIsCorrectGuess_shortWords() {
        assertTrue(StringSimilarity.isCorrectGuess("кот", "кот"));
        assertFalse(StringSimilarity.isCorrectGuess("кот", "кит")); // Short words require exact match
    }

    @Test
    void testIsCorrectGuess_wrongGuess() {
        assertFalse(StringSimilarity.isCorrectGuess("кошка", "собака"));
        assertFalse(StringSimilarity.isCorrectGuess("слон", "тигр"));
    }
}

