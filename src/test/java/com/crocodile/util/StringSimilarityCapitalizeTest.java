package com.crocodile.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify word capitalization logic in StringSimilarity utility.
 * Tests the requirement that words should always have the first letter uppercase
 * and the rest lowercase.
 */
class StringSimilarityCapitalizeTest {

    @Test
    void testCapitalize_allLowercase() {
        assertEquals("Кошка", StringSimilarity.capitalize("кошка"));
    }

    @Test
    void testCapitalize_allUppercase() {
        assertEquals("Собака", StringSimilarity.capitalize("СОБАКА"));
    }

    @Test
    void testCapitalize_mixedCase() {
        assertEquals("Слон", StringSimilarity.capitalize("сЛоН"));
    }

    @Test
    void testCapitalize_alreadyFormatted() {
        assertEquals("Тигр", StringSimilarity.capitalize("Тигр"));
    }

    @Test
    void testCapitalize_singleCharacter() {
        assertEquals("А", StringSimilarity.capitalize("а"));
        assertEquals("Я", StringSimilarity.capitalize("я"));
    }

    @Test
    void testCapitalize_latin() {
        assertEquals("Computer", StringSimilarity.capitalize("COMPUTER"));
        assertEquals("Mouse", StringSimilarity.capitalize("mouse"));
    }

    @Test
    void testCapitalize_empty() {
        assertEquals("", StringSimilarity.capitalize(""));
    }

    @Test
    void testCapitalize_null() {
        assertNull(StringSimilarity.capitalize(null));
    }

    @Test
    void testCapitalize_withSpaces() {
        // Note: In actual implementation, trim() is called before capitalize
        // But capitalize itself doesn't trim, so this test shows that behavior
        assertEquals(" кошка", StringSimilarity.capitalize(" кошка"));
    }
}
