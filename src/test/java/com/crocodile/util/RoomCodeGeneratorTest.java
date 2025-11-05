package com.crocodile.util;

import com.crocodile.domain.RoomCode;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoomCodeGeneratorTest {

    @Test
    void testGenerate_correctLength() {
        RoomCode code = RoomCodeGenerator.generate(6);
        assertEquals(6, code.getValue().length());
    }

    @Test
    void testGenerate_onlyAllowedCharacters() {
        // Generate multiple codes and check all characters
        for (int i = 0; i < 100; i++) {
            RoomCode roomCode = RoomCodeGenerator.generate(6);
            String code = roomCode.getValue();
            String allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
            
            for (char c : code.toCharArray()) {
                assertTrue(allowedChars.indexOf(c) >= 0, 
                    "Character " + c + " is not in allowed set");
            }
        }
    }
    
    @Test
    void testGenerate_validRoomCodeObject() {
        RoomCode code = RoomCodeGenerator.generate(6);
        assertNotNull(code);
        assertNotNull(code.getValue());
        assertEquals(6, code.getValue().length());
    }

    @Test
    void testGenerate_uniqueness() {
        Set<String> codes = new HashSet<>();
        int iterations = 1000;
        
        for (int i = 0; i < iterations; i++) {
            RoomCode code = RoomCodeGenerator.generate(6);
            codes.add(code.getValue());
        }
        
        // Should generate mostly unique codes
        assertTrue(codes.size() > iterations * 0.95, 
            "Generated codes are not unique enough");
    }

    @Test
    void testGenerate_noConfusingCharacters() {
        // Generate many codes and verify none contain confusing characters
        for (int i = 0; i < 1000; i++) {
            RoomCode roomCode = RoomCodeGenerator.generate(6);
            String code = roomCode.getValue();
            
            // Should not contain easily confused characters: I, O, 0, 1
            assertFalse(code.contains("I"), "Code contains 'I': " + code);
            assertFalse(code.contains("O"), "Code contains 'O': " + code);
            assertFalse(code.contains("0"), "Code contains '0': " + code);
            assertFalse(code.contains("1"), "Code contains '1': " + code);
        }
    }
}

