package com.crocodile.controller;

import com.crocodile.dto.CreateRoomRequest;
import com.crocodile.dto.CreateRoomResponse;
import com.crocodile.dto.RoomStateResponse;
import com.crocodile.service.GameService;
import com.crocodile.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final GameService gameService;
    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<CreateRoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        log.info("Creating room with theme: {}", request.getTheme());
        CreateRoomResponse response = gameService.createRoom(request.getTheme(), request.getWordProviderType());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{code}/state")
    public ResponseEntity<RoomStateResponse> getRoomState(
            @PathVariable String code,
            HttpServletRequest request) {
        
        String sessionId = sessionService.getSessionIdFromRequest(request).orElse("");
        RoomStateResponse response = gameService.getRoomState(code, sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/themes")
    public ResponseEntity<List<String>> getSupportedThemes(
            @RequestParam(defaultValue = "database") String provider) {
        List<String> themes = gameService.getSupportedThemes(provider);
        return ResponseEntity.ok(themes);
    }
}

