package com.crocodile.service;

import com.crocodile.dto.*;
import com.crocodile.exception.InvalidOperationException;
import com.crocodile.model.GameHistory;
import com.crocodile.model.Player;
import com.crocodile.model.Room;
import com.crocodile.repository.GameHistoryRepository;
import com.crocodile.service.wordprovider.WordProvider;
import com.crocodile.service.wordprovider.WordProviderFactory;
import com.crocodile.util.StringSimilarity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final RoomService roomService;
    private final PlayerService playerService;
    private final WordProviderFactory wordProviderFactory;
    private final GameHistoryRepository gameHistoryRepository;

    @Transactional
    public CreateRoomResponse createRoom(String theme, String wordProviderType) {
        Room room = roomService.createRoom(theme, wordProviderType);
        
        return CreateRoomResponse.builder()
            .roomCode(room.getCode())
            .roomUrl("/room/" + room.getCode())
            .theme(room.getTheme())
            .build();
    }

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

    public RoomStateResponse getRoomState(String roomCode, String sessionId) {
        Room room = roomService.getRoomByCode(roomCode);
        List<Player> players = playerService.getActivePlayers(room.getId());
        
        Player currentPlayer = playerService.getPlayerBySessionId(room.getId(), sessionId)
            .orElse(null);
        
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

    @Transactional
    public GuessResponse submitGuess(String roomCode, String sessionId, String guess) {
        Room room = roomService.getRoomByCode(roomCode);
        Player player = playerService.getPlayerBySessionId(room.getId(), sessionId)
            .orElseThrow(() -> new InvalidOperationException("Player not found in room"));
        
        if (player.getIsLeader()) {
            throw new InvalidOperationException("Leader cannot submit guesses");
        }
        
        if (room.getCurrentWord() == null) {
            throw new InvalidOperationException("No active word to guess");
        }
        
        boolean isCorrect = StringSimilarity.isCorrectGuess(room.getCurrentWord(), guess);
        
        if (isCorrect) {
            return handleCorrectGuess(room, player);
        }
        
        return GuessResponse.builder()
            .correct(false)
            .message("Неправильно, попробуйте ещё раз!")
            .build();
    }

    @Transactional
    public GuessResponse assignWinner(String roomCode, String sessionId, Long winnerId) {
        Room room = roomService.getRoomByCode(roomCode);
        Player leader = playerService.getPlayerBySessionId(room.getId(), sessionId)
            .orElseThrow(() -> new InvalidOperationException("Player not found in room"));
        
        if (!leader.getIsLeader()) {
            throw new InvalidOperationException("Only leader can assign winner");
        }
        
        Player winner = playerService.getPlayerById(winnerId, room.getId());
        
        if (winner.getIsLeader()) {
            throw new InvalidOperationException("Cannot assign leader as winner");
        }
        
        return handleCorrectGuess(room, winner);
    }

    @Transactional
    public NewWordResponse generateNewWord(String roomCode, String sessionId) {
        Room room = roomService.getRoomByCode(roomCode);
        Player player = playerService.getPlayerBySessionId(room.getId(), sessionId)
            .orElseThrow(() -> new InvalidOperationException("Player not found in room"));
        
        if (!player.getIsLeader()) {
            throw new InvalidOperationException("Only leader can generate new word");
        }
        
        WordProvider wordProvider = wordProviderFactory.getProvider(room.getWordProviderType());
        String newWord = wordProvider.generateWord(room.getTheme());
        
        room.setCurrentWord(newWord);
        roomService.updateRoom(room);
        
        log.info("Generated new word for room {}", room.getCode());
        
        return NewWordResponse.builder()
            .word(newWord)
            .message("Новое слово сгенерировано!")
            .build();
    }

    @Transactional
    public void leaveRoom(String roomCode, String sessionId) {
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

    public List<String> getSupportedThemes(String wordProviderType) {
        WordProvider wordProvider = wordProviderFactory.getProvider(wordProviderType);
        return wordProvider.getSupportedThemes();
    }

    private GuessResponse handleCorrectGuess(Room room, Player winner) {
        // Save game history
        if (room.getCurrentWord() != null) {
            GameHistory history = GameHistory.builder()
                .roomId(room.getId())
                .word(room.getCurrentWord())
                .leaderId(room.getCurrentLeaderId())
                .winnerId(winner.getId())
                .endTime(LocalDateTime.now())
                .build();
            gameHistoryRepository.save(history);
        }
        
        // Add score to winner
        playerService.addWinScore(winner.getId());
        
        // Change leader
        Player oldLeader = playerService.getActivePlayers(room.getId()).stream()
            .filter(Player::getIsLeader)
            .findFirst()
            .orElse(null);
        
        if (oldLeader != null) {
            playerService.setLeader(oldLeader.getId(), false);
        }
        
        playerService.setLeader(winner.getId(), true);
        room.setCurrentLeaderId(winner.getId());
        room.setCurrentWord(null); // Clear current word
        roomService.updateRoom(room);
        
        log.info("Player {} won the round! New leader: {}", winner.getName(), winner.getName());
        
        return GuessResponse.builder()
            .correct(true)
            .message("Правильно! " + winner.getName() + " угадал(а) слово!")
            .newLeaderId(winner.getId())
            .newLeaderName(winner.getName())
            .build();
    }

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

