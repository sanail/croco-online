package com.crocodile.service;

import com.crocodile.domain.Score;
import com.crocodile.exception.PlayerNotFoundException;
import com.crocodile.model.Player;
import com.crocodile.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {

    private final PlayerRepository playerRepository;

    @Value("${game.score.points-per-win}")
    private int pointsPerWin;

    @Transactional
    public Player joinRoom(Long roomId, String sessionId, String playerName, boolean isFirstPlayer) {
        Optional<Player> existingPlayer = playerRepository.findByRoomIdAndSessionId(roomId, sessionId);
        
        if (existingPlayer.isPresent()) {
            Player player = existingPlayer.get();
            player.setName(playerName);
            player.setIsActive(true);
            log.info("Player {} rejoined room", playerName);
            return playerRepository.save(player);
        }
        
        Player newPlayer = Player.builder()
            .roomId(roomId)
            .sessionId(sessionId)
            .name(playerName)
            .score(Score.zero())
            .isLeader(isFirstPlayer)
            .isActive(true)
            .build();
        
        Player saved = playerRepository.save(newPlayer);
        log.info("Player {} joined room as {}", playerName, isFirstPlayer ? "leader" : "player");
        
        return saved;
    }

    public List<Player> getActivePlayers(Long roomId) {
        return playerRepository.findByRoomIdAndIsActiveTrue(roomId);
    }

    public Player getPlayerById(Long playerId, Long roomId) {
        return playerRepository.findByIdAndRoomId(playerId, roomId)
            .orElseThrow(() -> new PlayerNotFoundException("Player not found"));
    }

    public Optional<Player> getPlayerBySessionId(Long roomId, String sessionId) {
        return playerRepository.findByRoomIdAndSessionId(roomId, sessionId);
    }

    public long getActivePlayerCount(Long roomId) {
        return playerRepository.countByRoomIdAndIsActiveTrue(roomId);
    }

    @Transactional
    public void setLeader(Long playerId, boolean isLeader) {
        playerRepository.findById(playerId).ifPresent(player -> {
            player.setIsLeader(isLeader);
            playerRepository.save(player);
            log.info("Player {} leader status set to {}", player.getName(), isLeader);
        });
    }

    @Transactional
    public void addScore(Long playerId, int points) {
        playerRepository.findById(playerId).ifPresent(player -> {
            player.addScore(points);
            playerRepository.save(player);
            log.info("Added {} points to player {}. New score: {}", points, player.getName(), player.getScore());
        });
    }

    @Transactional
    public void addWinScore(Long playerId) {
        addScore(playerId, pointsPerWin);
    }
    
    @Transactional
    public void setScore(Long playerId, Score score) {
        playerRepository.findById(playerId).ifPresent(player -> {
            player.setScore(score);
            playerRepository.save(player);
            log.info("Set score for player {} to {}", player.getName(), score);
        });
    }

    @Transactional
    public void leaveRoom(Long playerId) {
        playerRepository.findById(playerId).ifPresent(player -> {
            player.setIsActive(false);
            playerRepository.save(player);
            log.info("Player {} left the room", player.getName());
        });
    }

    @Transactional
    public Player assignNewLeader(Long roomId, Long currentLeaderId) {
        List<Player> activePlayers = getActivePlayers(roomId);
        
        // Remove current leader from consideration
        activePlayers.removeIf(p -> p.getId().equals(currentLeaderId));
        
        if (activePlayers.isEmpty()) {
            log.warn("No active players to assign as leader in room");
            return null;
        }
        
        // Assign first active player as new leader
        Player newLeader = activePlayers.getFirst();
        setLeader(newLeader.getId(), true);
        
        return newLeader;
    }

    public List<Player> getLeaderboard(Long roomId) {
        return playerRepository.findByRoomIdOrderByScoreDesc(roomId);
    }
}

