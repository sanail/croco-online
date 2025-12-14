package com.crocodile.util;

import com.crocodile.domain.WordValue;

public class StringSimilarity {

    /**
     * Calculate Levenshtein distance between two strings
     * @param s1 first string
     * @param s2 second string
     * @return distance value
     */
    public static int levenshteinDistance(String s1, String s2) {
        String a = s1.toLowerCase().trim();
        String b = s2.toLowerCase().trim();
        
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1),     // insertion
                    dp[i - 1][j - 1] + cost // substitution
                );
            }
        }

        return dp[a.length()][b.length()];
    }

    /**
     * Check if two strings are similar (allowing for typos)
     * @param s1 first string
     * @param s2 second string
     * @param maxDistance maximum allowed distance
     * @return true if strings are similar
     */
    public static boolean isSimilar(String s1, String s2, int maxDistance) {
        return levenshteinDistance(s1, s2) <= maxDistance;
    }

    /**
     * Check if guess matches the word (exact or with minor typos)
     * @param word the correct word
     * @param guess the player's guess
     * @return true if guess is correct
     */
    public static boolean isCorrectGuess(String word, String guess) {
        // Exact match (case insensitive)
        if (word.trim().equalsIgnoreCase(guess.trim())) {
            return true;
        }
        
        // Allow 1 character difference for words longer than 4 characters
        if (word.length() > 4) {
            return isSimilar(word, guess, 1);
        }
        
        return false;
    }
    
    /**
     * Check if guess matches the WordValue (exact or with minor typos)
     * 
     * @param word the WordValue object
     * @param guess the player's guess
     * @return true if guess is correct
     */
    public static boolean isCorrectGuess(WordValue word, String guess) {
        return isCorrectGuess(word.getValue(), guess);
    }
    
    /**
     * Capitalizes a word or phrase using title case: first letter of each word uppercase, rest lowercase.
     * Handles both Russian and Latin characters correctly.
     * For multi-word strings, each word is capitalized independently.
     * Multiple consecutive spaces are collapsed into single spaces.
     * Correctly handles words starting with non-letter characters (quotes, parentheses, etc.).
     * 
     * @param word the word or phrase to capitalize
     * @return capitalized word/phrase (null/empty preserved)
     */
    public static String capitalize(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        
        // Split by spaces to handle multi-word strings
        String[] words = word.split(" ");
        StringBuilder result = new StringBuilder();
        boolean firstWord = true;
        
        for (String currentWord : words) {
            // Skip empty strings (from multiple consecutive spaces)
            if (currentWord.isEmpty()) {
                continue;
            }
            
            // Add space before each word except the first
            if (!firstWord) {
                result.append(" ");
            }
            
            // Capitalize the word using helper method
            result.append(capitalizeWord(currentWord));
            
            firstWord = false;
        }
        
        return result.toString();
    }
    
    /**
     * Capitalizes a single word by finding the first letter and making it uppercase,
     * while making all other letters lowercase. Preserves non-letter prefixes
     * (quotes, parentheses, dashes, etc.) in their original positions.
     * 
     * Examples:
     * - "hello" → "Hello"
     * - "«млечный" → "«Млечный"
     * - "(яндекс)" → "(Яндекс)"
     * - "123abc" → "123Abc"
     * 
     * @param word a single word to capitalize
     * @return the word with first letter capitalized
     */
    private static String capitalizeWord(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        
        // Find the index of the first letter
        int firstLetterIndex = -1;
        for (int i = 0; i < word.length(); i++) {
            if (Character.isLetter(word.charAt(i))) {
                firstLetterIndex = i;
                break;
            }
        }
        
        // If no letters found, return as is
        if (firstLetterIndex == -1) {
            return word;
        }
        
        // Build the result:
        // 1. Prefix (non-letter characters before first letter)
        // 2. First letter (uppercase)
        // 3. Rest of the word (lowercase)
        StringBuilder result = new StringBuilder();
        
        // Add prefix (if any)
        if (firstLetterIndex > 0) {
            result.append(word.substring(0, firstLetterIndex));
        }
        
        // Add capitalized first letter
        result.append(Character.toUpperCase(word.charAt(firstLetterIndex)));
        
        // Add the rest in lowercase (if any)
        if (firstLetterIndex + 1 < word.length()) {
            result.append(word.substring(firstLetterIndex + 1).toLowerCase());
        }
        
        return result.toString();
    }
}

