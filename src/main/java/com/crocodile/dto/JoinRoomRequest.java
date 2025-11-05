package com.crocodile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomRequest {
    
    @NotBlank(message = "Player name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String playerName;
}

