package com.crocodile.repository;

import com.crocodile.model.Room;
import com.crocodile.model.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    Optional<Room> findByCode(String code);
    
    boolean existsByCode(String code);
    
    List<Room> findByStatusAndLastActivityBefore(RoomStatus status, LocalDateTime lastActivity);
}

