package com.crocodile.model;

import com.crocodile.domain.PlayerId;
import com.crocodile.domain.Score;
import com.crocodile.model.converter.ScoreConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    @Convert(converter = ScoreConverter.class)
    @Builder.Default
    private Score score = Score.zero();

    @Column(name = "is_leader", nullable = false)
    @Builder.Default
    private Boolean isLeader = false;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        if (score == null) {
            score = Score.zero();
        }
    }
    
    /**
     * Get PlayerId value object for this player
     * 
     * @return PlayerId combining sessionId and id
     */
    public PlayerId getPlayerId() {
        return PlayerId.of(this.sessionId, this.id);
    }
    
    /**
     * Add points to player's score
     * 
     * @param points points to add
     */
    public void addScore(int points) {
        this.score = this.score.add(points);
    }
}

