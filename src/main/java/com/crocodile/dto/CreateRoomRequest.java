package com.crocodile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {
    
    @NotBlank(message = "Theme is required")
    private String theme;
    
    @NotBlank(message = "Word provider type is required")
    @Pattern(regexp = "database|ai", message = "Word provider type must be 'database' or 'ai'")
    private String wordProviderType = "database";
    
    /**
     * Indicates if the theme is custom (user-entered) or from predefined list
     */
    private boolean customTheme = false;
}

