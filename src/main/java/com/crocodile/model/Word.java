package com.crocodile.model;

import com.crocodile.domain.WordValue;
import jakarta.persistence.*;
import lombok.*;

/**
 * Word - JPA Entity for storing words in the database
 * 
 * This is the persistence layer representation, separate from
 * the domain WordValue object used in business logic.
 */
@Entity
@Table(name = "words")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String word;

    @Column(nullable = false, length = 100)
    private String theme;

    @Column(length = 10)
    private String locale;
    
    /**
     * Convert this entity to a domain WordValue
     * 
     * @return WordValue domain object
     */
    public WordValue toWordValue() {
        return WordValue.of(this.word, this.theme);
    }
}

