package com.crocodile.repository;

import com.crocodile.model.Theme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThemeRepository extends JpaRepository<Theme, Long> {
    
    /**
     * Find theme by name
     * @param name theme name
     * @return theme if found
     */
    Optional<Theme> findByName(String name);
    
    /**
     * Get all theme names ordered alphabetically
     * @return list of theme names
     */
    @Query("SELECT t.name FROM Theme t ORDER BY t.name")
    List<String> findAllThemeNames();
}

