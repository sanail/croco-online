package com.crocodile.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoomCodeGeneratorTest {

    @Test
    void testGenerate_correctLength() {
        String code = RoomCodeGenerator.generate(6);
        assertEquals(6, code.length());
    }

    @Test
    void testGenerate_differentLength() {
        String code = RoomCodeGenerator.generate(10);
        assertEquals(10, code.length());
    }

    @Test
    void testGenerate_onlyAllowedCharacters() {
        String code = RoomCodeGenerator.generate(100);
        String allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        
        for (char c : code.toCharArray()) {
            assertTrue(allowedChars.indexOf(c) >= 0, 
                "Character " + c + " is not in allowed set");
        }
    }

    @Test
    void testGenerate_uniqueness() {
        Set<String> codes = new HashSet<>();
        int iterations = 1000;
        
        for (int i = 0; i < iterations; i++) {
            String code = RoomCodeGenerator.generate(6);
            codes.add(code);
        }
        
        // Should generate mostly unique codes
        assertTrue(codes.size() > iterations * 0.95, 
            "Generated codes are not unique enough");
    }

    @Test
    void testGenerate_noConfusingCharacters() {
        String code = RoomCodeGenerator.generate(1000);
        
        // Should not contain easily confused characters: I, O, 0, 1
        assertFalse(code.contains("I"));
        assertFalse(code.contains("O"));
        assertFalse(code.contains("0"));
        assertFalse(code.contains("1"));
    }
}

