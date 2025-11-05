package com.crocodile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuessResponse {
    
    private Boolean correct;
    private String message;
    private Long newLeaderId;
    private String newLeaderName;
}

