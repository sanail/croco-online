package com.crocodile.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.regex.Pattern;

/**
 * RoomCode - Value Object for room identification
 * 
 * Immutable representation of a room code with validation.
 * Room codes must be alphanumeric without confusing characters.
 */
@Getter
@EqualsAndHashCode
public final class RoomCode {
    
    private static final Pattern VALID_CODE_PATTERN = Pattern.compile("^[A-HJ-NP-Z2-9]+$");
    private static final int EXPECTED_LENGTH = 6;
    
    private final String value;
    
    private RoomCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Room code cannot be null or blank");
        }
        
        String normalized = value.trim().toUpperCase();
        
        if (normalized.length() != EXPECTED_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Room code must be exactly %d characters, got %d", 
                    EXPECTED_LENGTH, normalized.length())
            );
        }
        
        if (!VALID_CODE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                "Room code must be alphanumeric without confusing characters (I, O, 0, 1)"
            );
        }
        
        this.value = normalized;
    }
    
    /**
     * Create a RoomCode from a string value
     * 
     * @param value the string representation
     * @return the RoomCode instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static RoomCode of(String value) {
        return new RoomCode(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}

