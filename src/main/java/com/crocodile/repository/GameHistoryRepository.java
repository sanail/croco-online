package com.crocodile.repository;

import com.crocodile.model.GameHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {
    
    List<GameHistory> findByRoomIdOrderByStartTimeDesc(Long roomId);
    
    List<GameHistory> findTop10ByRoomIdOrderByStartTimeDesc(Long roomId);
}

