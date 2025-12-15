package com.crocodile.service.wordprovider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * WordProviderFactory - Factory for selecting word generation strategy
 * 
 * This factory manages two types of word providers:
 * - "database" - generates words from database
 * - "ai" - generates words using LLM/AI
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WordProviderFactory {

    private final List<WordProvider> wordProviders;

    /**
     * Get a word provider by type
     * @param type provider type ("database" or "ai")
     * @return the requested word provider
     * @throws IllegalArgumentException if provider type is unknown
     */
    public WordProvider getProvider(String type) {
        log.debug("Getting word provider for type: {}", type);
        
        Map<String, WordProvider> providerMap = wordProviders.stream()
            .collect(Collectors.toMap(WordProvider::getType, Function.identity()));
        
        WordProvider provider = providerMap.get(type);
        if (provider == null) {
            log.error("Unknown word provider type: {}. Available types: {}", type, providerMap.keySet());
            throw new IllegalArgumentException(
                String.format("Unknown word provider type: %s. Available types: %s", 
                             type, providerMap.keySet())
            );
        }
        
        log.info("Using word provider: {}", provider.getType());
        return provider;
    }

    /**
     * Get all available word provider types
     * Should return: ["database", "ai"]
     * @return list of available provider type identifiers
     */
    public List<String> getAvailableProviderTypes() {
        List<String> types = wordProviders.stream()
            .map(WordProvider::getType)
            .collect(Collectors.toList());
        
        log.debug("Available word provider types: {}", types);
        return types;
    }
}

