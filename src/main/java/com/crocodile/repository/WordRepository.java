package com.crocodile.repository;

import com.crocodile.model.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {
    
    List<Word> findByTheme(String theme);
    
    @Query(value = "SELECT DISTINCT theme FROM words ORDER BY theme", nativeQuery = true)
    List<String> findAllDistinctThemes();
    
    @Query(value = "SELECT * FROM words WHERE theme = :theme ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Word findRandomByTheme(@Param("theme") String theme);
}
