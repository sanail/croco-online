package com.crocodile.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignWinnerRequest {
    
    @NotNull(message = "Winner ID is required")
    private Long winnerId;
}

