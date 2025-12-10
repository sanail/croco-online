package com.crocodile.service.wordprovider;

/**
 * WordProvider - Black Box for Word Generation
 * 
 * This interface is now focused solely on word generation.
 * Theme management has been separated into ThemeProvider.
 * 
 * Responsibilities:
 * - Generate words for a given theme
 * - Identify provider type
 */
public interface WordProvider {
    
    /**
     * Generate a word for the given theme
     * @param theme the theme for word generation
     * @return generated word
     */
    String generateWord(String theme);
    
    /**
     * Get the type identifier of this provider
     * @return provider type
     */
    String getType();
}

