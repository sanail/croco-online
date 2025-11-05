package com.crocodile.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * PlayerId - Value Object for player identification
 * 
 * Immutable representation combining sessionId (external identifier)
 * and internal database id. SessionId is always present, id may be null
 * for players not yet persisted.
 */
@Getter
@EqualsAndHashCode
public final class PlayerId {
    
    private final String sessionId;
    private final Long id;
    
    private PlayerId(String sessionId, Long id) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID cannot be null or blank");
        }
        
        this.sessionId = sessionId.trim();
        this.id = id;
    }
    
    /**
     * Create a PlayerId with both session ID and database ID
     * 
     * @param sessionId the session identifier
     * @param id the database identifier
     * @return the PlayerId instance
     * @throws IllegalArgumentException if sessionId is invalid
     */
    public static PlayerId of(String sessionId, Long id) {
        return new PlayerId(sessionId, id);
    }
    
    /**
     * Create a PlayerId from only a session ID (for new players)
     * 
     * @param sessionId the session identifier
     * @return the PlayerId instance with null id
     * @throws IllegalArgumentException if sessionId is invalid
     */
    public static PlayerId fromSession(String sessionId) {
        return new PlayerId(sessionId, null);
    }
    
    /**
     * Check if this player has a database ID
     * 
     * @return true if id is not null
     */
    public boolean hasId() {
        return id != null;
    }
    
    /**
     * Create a new PlayerId with the given database ID
     * 
     * @param newId the database identifier
     * @return new PlayerId instance
     */
    public PlayerId withId(Long newId) {
        return new PlayerId(this.sessionId, newId);
    }
    
    @Override
    public String toString() {
        return String.format("PlayerId{sessionId='%s', id=%d}", sessionId, id);
    }
}

