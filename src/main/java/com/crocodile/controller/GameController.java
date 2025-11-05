package com.crocodile.controller;

import com.crocodile.domain.RoomCode;
import com.crocodile.dto.*;
import com.crocodile.service.RoomCoordinator;
import com.crocodile.service.GameRoundService;
import com.crocodile.service.LeadershipService;
import com.crocodile.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{roomCode}")
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final RoomCoordinator roomCoordinator;
    private final GameRoundService gameRoundService;
    private final LeadershipService leadershipService;
    private final SessionService sessionService;

    @PostMapping("/join")
    public ResponseEntity<JoinRoomResponse> joinRoom(
            @PathVariable String roomCode,
            @Valid @RequestBody JoinRoomRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        
        String sessionId = sessionService.getOrCreateSessionId(httpRequest, httpResponse);
        log.info("Player {} joining room {}", request.getPlayerName(), roomCode);
        
        RoomCode code = RoomCode.of(roomCode);
        JoinRoomResponse response = roomCoordinator.joinRoom(code, sessionId, request.getPlayerName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/guess")
    public ResponseEntity<GuessResponse> submitGuess(
            @PathVariable String roomCode,
            @Valid @RequestBody GuessRequest request,
            HttpServletRequest httpRequest) {
        
        String sessionId = sessionService.getSessionIdFromRequest(httpRequest)
            .orElseThrow(() -> new IllegalStateException("No session found"));
        
        log.info("Player submitting guess in room {}", roomCode);
        RoomCode code = RoomCode.of(roomCode);
        GuessResponse response = gameRoundService.submitGuess(code, sessionId, request.getGuess());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assign-winner")
    public ResponseEntity<GuessResponse> assignWinner(
            @PathVariable String roomCode,
            @Valid @RequestBody AssignWinnerRequest request,
            HttpServletRequest httpRequest) {
        
        String sessionId = sessionService.getSessionIdFromRequest(httpRequest)
            .orElseThrow(() -> new IllegalStateException("No session found"));
        
        log.info("Leader assigning winner in room {}", roomCode);
        RoomCode code = RoomCode.of(roomCode);
        GuessResponse response = gameRoundService.assignWinner(code, sessionId, request.getWinnerId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/new-word")
    public ResponseEntity<NewWordResponse> generateNewWord(
            @PathVariable String roomCode,
            HttpServletRequest httpRequest) {
        
        String sessionId = sessionService.getSessionIdFromRequest(httpRequest)
            .orElseThrow(() -> new IllegalStateException("No session found"));
        
        log.info("Generating new word for room {}", roomCode);
        RoomCode code = RoomCode.of(roomCode);
        NewWordResponse response = gameRoundService.generateNewWord(code, sessionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable String roomCode,
            HttpServletRequest httpRequest) {
        
        String sessionId = sessionService.getSessionIdFromRequest(httpRequest)
            .orElseThrow(() -> new IllegalStateException("No session found"));
        
        log.info("Player leaving room {}", roomCode);
        RoomCode code = RoomCode.of(roomCode);
        leadershipService.handlePlayerLeave(code, sessionId);
        return ResponseEntity.ok().build();
    }
}

