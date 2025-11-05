package com.crocodile.service.wordprovider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class LmStudioWordProvider implements WordProvider {

    @Override
    public String generateWord(String theme) {
        log.info("LM Studio word provider called for theme: {}", theme);
        // TODO: Implement LM Studio integration
        throw new UnsupportedOperationException("LM Studio integration not implemented yet");
    }

    @Override
    public List<String> getSupportedThemes() {
        return Arrays.asList(
            "Животные",
            "Профессии",
            "Предметы быта",
            "Фильмы и сериалы",
            "Еда и напитки",
            "Спорт",
            "Города и страны"
        );
    }

    @Override
    public String getType() {
        return "lm-studio";
    }
}

