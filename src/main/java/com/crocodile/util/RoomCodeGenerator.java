package com.crocodile.util;

import com.crocodile.domain.RoomCode;

import java.security.SecureRandom;

public class RoomCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate a random room code
     * 
     * @param length the length of the code to generate
     * @return RoomCode value object
     */
    public static RoomCode generate(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return RoomCode.of(code.toString());
    }
}

