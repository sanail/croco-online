package com.crocodile.service;

import com.crocodile.exception.RoomNotFoundException;
import com.crocodile.model.Room;
import com.crocodile.model.RoomStatus;
import com.crocodile.repository.RoomRepository;
import com.crocodile.service.wordprovider.WordProvider;
import com.crocodile.service.wordprovider.WordProviderFactory;
import com.crocodile.util.RoomCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final WordProviderFactory wordProviderFactory;

    @Value("${game.room.code-length}")
    private int codeLength;

    @Value("${game.room.inactive-timeout-minutes}")
    private int inactiveTimeoutMinutes;

    @Transactional
    public Room createRoom(String theme, String wordProviderType) {
        String code = generateUniqueCode();
        
        WordProvider wordProvider = wordProviderFactory.getProvider(wordProviderType);
        
        Room room = Room.builder()
            .code(code)
            .theme(theme)
            .status(RoomStatus.ACTIVE)
            .wordProviderType(wordProviderType)
            .build();
        
        Room savedRoom = roomRepository.save(room);
        log.info("Created new room with code: {}, theme: {}", code, theme);
        
        return savedRoom;
    }

    public Room getRoomByCode(String code) {
        return roomRepository.findByCode(code)
            .orElseThrow(() -> new RoomNotFoundException("Room not found: " + code));
    }

    @Transactional
    public Room updateRoom(Room room) {
        return roomRepository.save(room);
    }

    @Transactional
    public void markRoomAsInactive(Long roomId) {
        roomRepository.findById(roomId).ifPresent(room -> {
            room.setStatus(RoomStatus.INACTIVE);
            roomRepository.save(room);
            log.info("Marked room {} as inactive", room.getCode());
        });
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void cleanupInactiveRooms() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(inactiveTimeoutMinutes);
        List<Room> inactiveRooms = roomRepository.findByStatusAndLastActivityBefore(
            RoomStatus.ACTIVE, threshold
        );
        
        inactiveRooms.forEach(room -> {
            room.setStatus(RoomStatus.INACTIVE);
            roomRepository.save(room);
            log.info("Auto-marked room {} as inactive due to inactivity", room.getCode());
        });
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = RoomCodeGenerator.generate(codeLength);
        } while (roomRepository.existsByCode(code));
        return code;
    }
}

