package com.crocodile.service.wordprovider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class YandexGptWordProvider implements WordProvider {

    @Override
    public String generateWord(String theme) {
        log.info("YandexGPT word provider called for theme: {}", theme);
        // TODO: Implement YandexGPT integration
        throw new UnsupportedOperationException("YandexGPT integration not implemented yet");
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
        return "yandex-gpt";
    }
}

