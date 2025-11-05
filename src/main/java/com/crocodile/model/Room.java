package com.crocodile.model;

import com.crocodile.domain.RoomCode;
import com.crocodile.domain.WordValue;
import com.crocodile.model.converter.RoomCodeConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    @Convert(converter = RoomCodeConverter.class)
    private RoomCode code;

    @Column(nullable = false, length = 100)
    private String theme;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status;

    @Column(name = "current_word", length = 255)
    private String currentWord;

    @Column(name = "current_leader_id")
    private Long currentLeaderId;

    @Column(name = "word_provider_type", length = 50)
    private String wordProviderType;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastActivity = LocalDateTime.now();
        if (status == null) {
            status = RoomStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastActivity = LocalDateTime.now();
    }
    
    /**
     * Get current word as WordValue domain object
     * 
     * @return WordValue or null if no current word
     */
    public WordValue getCurrentWordValue() {
        return currentWord == null ? null : WordValue.fromString(currentWord);
    }
    
    /**
     * Set current word from WordValue domain object
     * 
     * @param wordValue the word value to set
     */
    public void setCurrentWordValue(WordValue wordValue) {
        this.currentWord = wordValue == null ? null : wordValue.getValue();
    }
}

