package com.crocodile.service.themeprovider;

import java.util.List;

/**
 * ThemeProvider - Black Box for Theme Management
 * 
 * This interface completely hides the source of themes.
 * Implementation can be replaced without affecting other components.
 * 
 * Responsibilities:
 * - Provide list of available themes for room creation
 * 
 * This interface is intentionally minimal to be easily replaceable.
 */
public interface ThemeProvider {
    
    /**
     * Get list of available themes
     * 
     * @return list of theme names
     */
    List<String> getAvailableThemes();
}

