package com.crocodile.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * WordValue - Value Object for game words
 * 
 * Immutable representation of a word with its theme.
 * Used in domain logic, separate from the database entity.
 */
@Getter
@EqualsAndHashCode
public final class WordValue {
    
    private final String value;
    private final String theme;
    
    private WordValue(String value, String theme) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Word value cannot be null or blank");
        }
        if (theme == null || theme.isBlank()) {
            throw new IllegalArgumentException("Word theme cannot be null or blank");
        }
        
        this.value = value.trim();
        this.theme = theme.trim();
    }
    
    /**
     * Create a WordValue with value and theme
     * 
     * @param value the word text
     * @param theme the word's theme
     * @return the WordValue instance
     * @throws IllegalArgumentException if value or theme is invalid
     */
    public static WordValue of(String value, String theme) {
        return new WordValue(value, theme);
    }
    
    /**
     * Create a WordValue with only the value (theme defaults to empty)
     * Used for simple word representation without theme context
     * 
     * @param value the word text
     * @return the WordValue instance
     */
    public static WordValue fromString(String value) {
        return new WordValue(value, "");
    }
    
    @Override
    public String toString() {
        return value;
    }
}

