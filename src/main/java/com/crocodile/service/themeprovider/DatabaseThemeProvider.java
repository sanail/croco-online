package com.crocodile.service.themeprovider;

import com.crocodile.repository.ThemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DatabaseThemeProvider - Implementation that retrieves themes from database
 * 
 * This is a black box implementation of ThemeProvider.
 * Can be completely replaced without affecting other components.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseThemeProvider implements ThemeProvider {

    private final ThemeRepository themeRepository;

    @Override
    public List<String> getAvailableThemes() {
        log.debug("Retrieving themes from database");
        return themeRepository.findAllThemeNames();
    }
}

