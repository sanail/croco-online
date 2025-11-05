package com.crocodile.repository;

import com.crocodile.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    
    List<Player> findByRoomIdAndIsActiveTrue(Long roomId);
    
    Optional<Player> findByRoomIdAndSessionId(Long roomId, String sessionId);
    
    Optional<Player> findByIdAndRoomId(Long id, Long roomId);
    
    long countByRoomIdAndIsActiveTrue(Long roomId);
    
    List<Player> findByRoomIdOrderByScoreDesc(Long roomId);
}

