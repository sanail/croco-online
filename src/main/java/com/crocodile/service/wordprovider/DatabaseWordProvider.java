package com.crocodile.service.wordprovider;

import com.crocodile.model.Word;
import com.crocodile.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseWordProvider implements WordProvider {

    private final WordRepository wordRepository;

    @Override
    public String generateWord(String theme) {
        log.info("Generating word from database for theme: {}", theme);
        Word word = wordRepository.findRandomByTheme(theme);
        
        if (word == null) {
            log.warn("No words found for theme: {}, using fallback", theme);
            return getFallbackWord(theme);
        }
        
        return word.getWord();
    }

    @Override
    public String getType() {
        return "database";
    }

    private String getFallbackWord(String theme) {
        // Fallback words if database is empty
        return switch (theme.toLowerCase()) {
            case "животные" -> "Кошка";
            case "профессии" -> "Врач";
            case "предметы быта" -> "Стул";
            case "фильмы и сериалы" -> "Титаник";
            case "еда и напитки" -> "Пицца";
            case "спорт" -> "Футбол";
            case "города и страны" -> "Москва";
            default -> "Слово";
        };
    }
}
