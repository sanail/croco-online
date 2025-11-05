package com.crocodile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomStateResponse {
    
    private String roomCode;
    private String theme;
    private String status;
    private String currentWord;
    private Long currentLeaderId;
    private String currentLeaderName;
    private List<PlayerDto> players;
    private Boolean hasWord;
}

