package com.crocodile.util;

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
}

