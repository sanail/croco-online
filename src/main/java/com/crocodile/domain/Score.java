package com.crocodile.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Score - Value Object for player scores
 * 
 * Immutable representation of a player's score with validation.
 * Scores must be non-negative and support addition operations.
 */
@Getter
@EqualsAndHashCode
public final class Score {
    
    private final int value;
    
    private Score(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Score cannot be negative, got: " + value);
        }
        this.value = value;
    }
    
    /**
     * Create a Score with the given value
     * 
     * @param value the score value (must be non-negative)
     * @return the Score instance
     * @throws IllegalArgumentException if value is negative
     */
    public static Score of(int value) {
        return new Score(value);
    }
    
    /**
     * Create a Score with value 0
     * 
     * @return Score with value 0
     */
    public static Score zero() {
        return new Score(0);
    }
    
    /**
     * Add points to this score
     * 
     * @param points the points to add (can be negative to subtract)
     * @return new Score instance with added points
     * @throws IllegalArgumentException if result would be negative
     */
    public Score add(int points) {
        return new Score(this.value + points);
    }
    
    /**
     * Check if this score is zero
     * 
     * @return true if value is 0
     */
    public boolean isZero() {
        return value == 0;
    }
    
    /**
     * Compare this score with another
     * 
     * @param other the other score
     * @return negative if less, 0 if equal, positive if greater
     */
    public int compareTo(Score other) {
        return Integer.compare(this.value, other.value);
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

