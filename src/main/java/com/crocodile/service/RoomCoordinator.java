package com.crocodile.service;

import com.crocodile.dto.CreateRoomResponse;
import com.crocodile.dto.JoinRoomResponse;
import com.crocodile.dto.PlayerDto;
import com.crocodile.dto.RoomStateResponse;
import com.crocodile.model.Player;
import com.crocodile.model.Room;
import com.crocodile.service.wordprovider.WordProvider;
import com.crocodile.service.wordprovider.WordProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RoomCoordinator - Black Box for Room Lifecycle Management
 * 
 * Responsibilities:
 * - Room creation
 * - Player joining rooms
 * - Room state aggregation and queries
 * - Theme management
 * 
 * This service can be completely replaced without affecting other components.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomCoordinator {

    private final RoomService roomService;
    private final PlayerService playerService;
    private final WordProviderFactory wordProviderFactory;

    /**
     * Create a new game room
     * 
     * @param theme the theme for word generation
     * @param wordProviderType the type of word provider to use
     * @return room creation response with code and URL
     */
    @Transactional
    public CreateRoomResponse createRoom(String theme, String wordProviderType) {
        Room room = roomService.createRoom(theme, wordProviderType);
        
        return CreateRoomResponse.builder()
            .roomCode(room.getCode())
            .roomUrl("/room/" + room.getCode())
            .theme(room.getTheme())
            .build();
    }

    /**
     * Join a player to a room
     * 
     * @param roomCode the room code
     * @param sessionId the player's session ID
     * @param playerName the player's name
     * @return join response with player information
     */
    @Transactional
    public JoinRoomResponse joinRoom(String roomCode, String sessionId, String playerName) {
        Room room = roomService.getRoomByCode(roomCode);
        
        long playerCount = playerService.getActivePlayerCount(room.getId());
        boolean isFirstPlayer = playerCount == 0;
        
        Player player = playerService.joinRoom(room.getId(), sessionId, playerName, isFirstPlayer);
        
        // Set as current leader in room if first player
        if (isFirstPlayer) {
            room.setCurrentLeaderId(player.getId());
            roomService.updateRoom(room);
        }
        
        return JoinRoomResponse.builder()
            .playerId(player.getId())
            .playerName(player.getName())
            .isLeader(player.getIsLeader())
            .sessionId(sessionId)
            .build();
    }

    /**
     * Get the current state of a room
     * 
     * @param roomCode the room code
     * @param sessionId the requesting player's session ID
     * @return complete room state with players and current word (if applicable)
     */
    public RoomStateResponse getRoomState(String roomCode, String sessionId) {
        Room room = roomService.getRoomByCode(roomCode);
        List<Player> players = playerService.getActivePlayers(room.getId());
        
        Player currentPlayer = playerService.getPlayerBySessionId(room.getId(), sessionId)
            .orElse(null);
        
        // Only show current word to the leader
        String currentWord = null;
        if (currentPlayer != null && currentPlayer.getIsLeader()) {
            currentWord = room.getCurrentWord();
        }
        
        Player leader = players.stream()
            .filter(Player::getIsLeader)
            .findFirst()
            .orElse(null);
        
        List<PlayerDto> playerDtos = players.stream()
            .map(this::convertToPlayerDto)
            .collect(Collectors.toList());
        
        return RoomStateResponse.builder()
            .roomCode(room.getCode())
            .theme(room.getTheme())
            .status(room.getStatus().name())
            .currentWord(currentWord)
            .currentLeaderId(leader != null ? leader.getId() : null)
            .currentLeaderName(leader != null ? leader.getName() : null)
            .players(playerDtos)
            .hasWord(room.getCurrentWord() != null)
            .build();
    }

    /**
     * Get supported themes for a word provider
     * 
     * @param wordProviderType the type of word provider
     * @return list of supported themes
     */
    public List<String> getSupportedThemes(String wordProviderType) {
        WordProvider wordProvider = wordProviderFactory.getProvider(wordProviderType);
        return wordProvider.getSupportedThemes();
    }

    /**
     * Convert Player entity to PlayerDto
     * 
     * @param player the player entity
     * @return player DTO
     */
    private PlayerDto convertToPlayerDto(Player player) {
        return PlayerDto.builder()
            .id(player.getId())
            .name(player.getName())
            .score(player.getScore())
            .isLeader(player.getIsLeader())
            .isActive(player.getIsActive())
            .build();
    }
}

