package com.crocodile.service;

import com.crocodile.domain.RoomCode;
import com.crocodile.exception.InvalidOperationException;
import com.crocodile.model.Player;
import com.crocodile.model.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LeadershipService - Black Box for Leader Management
 * 
 * Responsibilities:
 * - Handling leader changes when players leave
 * - Assigning new leaders after rounds
 * - Managing leader state transitions
 * 
 * This service can be completely replaced without affecting other components.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeadershipService {

    private final PlayerService playerService;
    private final RoomService roomService;

    /**
     * Handle a player leaving the room, including leader transitions
     * 
     * @param roomCode the room code
     * @param sessionId the player's session ID
     */
    @Transactional
    public void handlePlayerLeave(RoomCode roomCode, String sessionId) {
        Room room = roomService.getRoomByCode(roomCode);
        Player player = playerService.getPlayerBySessionId(room.getId(), sessionId)
            .orElseThrow(() -> new InvalidOperationException("Player not found in room"));
        
        boolean wasLeader = player.getIsLeader();
        playerService.leaveRoom(player.getId());
        
        // If leader left, assign new leader
        if (wasLeader) {
            Player newLeader = playerService.assignNewLeader(room.getId(), player.getId());
            if (newLeader != null) {
                room.setCurrentLeaderId(newLeader.getId());
                roomService.updateRoom(room);
                log.info("New leader assigned: {}", newLeader.getName());
            } else {
                // No players left, mark room as inactive
                roomService.markRoomAsInactive(room.getId());
            }
        }
        
        // Check if no players left
        long activePlayerCount = playerService.getActivePlayerCount(room.getId());
        if (activePlayerCount == 0) {
            roomService.markRoomAsInactive(room.getId());
        }
    }

    /**
     * Change the leader of a room (used after correct guesses)
     * 
     * @param room the room
     * @param newLeader the player to become leader
     */
    @Transactional
    public void changeLeader(Room room, Player newLeader) {
        // Remove leader status from old leader
        Player oldLeader = playerService.getActivePlayers(room.getId()).stream()
            .filter(Player::getIsLeader)
            .findFirst()
            .orElse(null);
        
        if (oldLeader != null) {
            playerService.setLeader(oldLeader.getId(), false);
        }
        
        // Set new leader
        playerService.setLeader(newLeader.getId(), true);
        room.setCurrentLeaderId(newLeader.getId());
        roomService.updateRoom(room);
        
        log.info("Leader changed from {} to {}", 
            oldLeader != null ? oldLeader.getName() : "none", 
            newLeader.getName());
    }
}

