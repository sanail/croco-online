package com.crocodile.service.wordprovider;

import java.util.List;

public interface WordProvider {
    
    /**
     * Generate a word for the given theme
     * @param theme the theme for word generation
     * @return generated word
     */
    String generateWord(String theme);
    
    /**
     * Get list of supported themes
     * @return list of theme names
     */
    List<String> getSupportedThemes();
    
    /**
     * Get the type identifier of this provider
     * @return provider type
     */
    String getType();
}

