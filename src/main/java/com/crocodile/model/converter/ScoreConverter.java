package com.crocodile.model.converter;

import com.crocodile.domain.Score;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for Score Value Object
 * 
 * Converts between Score domain object and Integer database representation.
 */
@Converter(autoApply = true)
public class ScoreConverter implements AttributeConverter<Score, Integer> {
    
    @Override
    public Integer convertToDatabaseColumn(Score score) {
        return score == null ? 0 : score.getValue();
    }
    
    @Override
    public Score convertToEntityAttribute(Integer dbData) {
        return dbData == null ? Score.zero() : Score.of(dbData);
    }
}

