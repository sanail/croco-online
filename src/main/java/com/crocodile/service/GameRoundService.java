package com.crocodile.service;

import com.crocodile.dto.GuessResponse;
import com.crocodile.dto.NewWordResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * GameRoundService - Black Box for Game Round Mechanics
 * 
 * Responsibilities:
 * - Managing guess submissions and validation
 * - Handling correct guesses and scoring
 * - Word generation for rounds
 * - Winner assignment
 * - Game history recording
 * 
 * This service can be completely replaced without affecting other components.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameRoundService {

    private final RoomService roomService;
    private final PlayerService playerService;
    private final LeadershipService leadershipService;
    private final WordProviderFactory wordProviderFactory;
    private final GameHistoryRepository gameHistoryRepository;

    @Value("${game.score.points-per-win}")
    private int pointsPerWin;

    /**
     * Submit a guess for the current word
     * 
     * @param roomCode the room code
     * @param sessionId the player's session ID
     * @param guess the guessed word
     * @return result of the guess
     */
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

    /**
     * Manually assign a winner for the current round (leader only)
     * 
     * @param roomCode the room code
     * @param sessionId the leader's session ID
     * @param winnerId the ID of the winning player
     * @return result of the assignment
     */
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

    /**
     * Generate a new word for the current round (leader only)
     * 
     * @param roomCode the room code
     * @param sessionId the leader's session ID
     * @return the generated word
     */
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

    /**
     * Handle a correct guess - update scores, change leader, save history
     * 
     * @param room the room
     * @param winner the player who guessed correctly
     * @return response with winner information
     */
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
        playerService.addScore(winner.getId(), pointsPerWin);
        
        // Change leader using LeadershipService
        leadershipService.changeLeader(room, winner);
        
        // Clear current word
        room.setCurrentWord(null);
        roomService.updateRoom(room);
        
        log.info("Player {} won the round! New leader: {}", winner.getName(), winner.getName());
        
        return GuessResponse.builder()
            .correct(true)
            .message("Правильно! " + winner.getName() + " угадал(а) слово!")
            .newLeaderId(winner.getId())
            .newLeaderName(winner.getName())
            .build();
    }
}

