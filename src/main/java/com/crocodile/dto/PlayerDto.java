package com.crocodile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerDto {
    
    private Long id;
    private String name;
    private Integer score;
    private Boolean isLeader;
    private Boolean isActive;
}

