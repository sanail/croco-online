package com.crocodile.model.converter;

import com.crocodile.domain.RoomCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for RoomCode Value Object
 * 
 * Converts between RoomCode domain object and String database representation.
 */
@Converter(autoApply = true)
public class RoomCodeConverter implements AttributeConverter<RoomCode, String> {
    
    @Override
    public String convertToDatabaseColumn(RoomCode roomCode) {
        return roomCode == null ? null : roomCode.getValue();
    }
    
    @Override
    public RoomCode convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RoomCode.of(dbData);
    }
}

