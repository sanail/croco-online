package com.crocodile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoomResponse {
    
    private String roomCode;
    private String roomUrl;
    private String theme;
}

