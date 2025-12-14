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
    void testCapitalize_multiWordRussian() {
        assertEquals("Форрест Гамп", StringSimilarity.capitalize("форрест гамп"));
        assertEquals("Игра Престолов", StringSimilarity.capitalize("игра престолов"));
        assertEquals("Крёстный Отец", StringSimilarity.capitalize("крёстный отец"));
    }

    @Test
    void testCapitalize_multiWordLatin() {
        assertEquals("Star Wars", StringSimilarity.capitalize("star wars"));
        assertEquals("Harry Potter", StringSimilarity.capitalize("harry potter"));
        assertEquals("The Matrix", StringSimilarity.capitalize("the matrix"));
    }

    @Test
    void testCapitalize_multiWordMixedCase() {
        assertEquals("Форрест Гамп", StringSimilarity.capitalize("ФОРРЕСТ ГАМП"));
        assertEquals("Игра Престолов", StringSimilarity.capitalize("иГрА пРеСтОлОв"));
        assertEquals("Star Wars", StringSimilarity.capitalize("sTaR WaRs"));
    }

    @Test
    void testCapitalize_multipleSpaces() {
        // Multiple consecutive spaces should be preserved as single spaces between words
        assertEquals("Игра Престолов", StringSimilarity.capitalize("игра  престолов"));
    }

    @Test
    void testCapitalize_withQuotes() {
        // Russian quotes (кавычки-ёлочки)
        assertEquals("«Млечный", StringSimilarity.capitalize("«млечный"));
        assertEquals("«Война", StringSimilarity.capitalize("«война"));
        
        // Regular double quotes
        assertEquals("\"Война", StringSimilarity.capitalize("\"война"));
        assertEquals("\"Hello", StringSimilarity.capitalize("\"hello"));
        
        // Single quotes
        assertEquals("'Звездные", StringSimilarity.capitalize("'звездные"));
        assertEquals("'Star", StringSimilarity.capitalize("'star"));
    }

    @Test
    void testCapitalize_withParentheses() {
        assertEquals("(Яндекс)", StringSimilarity.capitalize("(яндекс)"));
        assertEquals("(Google)", StringSimilarity.capitalize("(google)"));
        assertEquals("(Компания", StringSimilarity.capitalize("(компания"));
    }

    @Test
    void testCapitalize_multiWordWithQuotes() {
        // The main test case from the user's requirement
        assertEquals("Галактика «Млечный Путь»", StringSimilarity.capitalize("Галактика «млечный Путь»"));
        assertEquals("Галактика «Млечный Путь»", StringSimilarity.capitalize("галактика «млечный путь»"));
        
        // Additional test cases
        assertEquals("Книга \"Война И Мир\"", StringSimilarity.capitalize("книга \"война и мир\""));
        assertEquals("Фильм 'Звездные Войны'", StringSimilarity.capitalize("фильм 'звездные войны'"));
        assertEquals("Компания (Яндекс) Работает", StringSimilarity.capitalize("компания (яндекс) работает"));
    }

    @Test
    void testCapitalize_withDash() {
        assertEquals("-Слово", StringSimilarity.capitalize("-слово"));
        assertEquals("—Тире", StringSimilarity.capitalize("—тире"));
        assertEquals("-Word", StringSimilarity.capitalize("-word"));
    }

    @Test
    void testCapitalize_withNumbers() {
        assertEquals("123Abc", StringSimilarity.capitalize("123abc"));
        assertEquals("007Агент", StringSimilarity.capitalize("007агент"));
        assertEquals("2024Год", StringSimilarity.capitalize("2024год"));
    }

    @Test
    void testCapitalize_onlyNonLetters() {
        // If there are no letters, return as is
        assertEquals("123", StringSimilarity.capitalize("123"));
        assertEquals("«»", StringSimilarity.capitalize("«»"));
        assertEquals("---", StringSimilarity.capitalize("---"));
    }
}
