package com.crocodile.service.wordprovider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WordProviderFactory {

    private final List<WordProvider> wordProviders;

    public WordProvider getProvider(String type) {
        Map<String, WordProvider> providerMap = wordProviders.stream()
            .collect(Collectors.toMap(WordProvider::getType, Function.identity()));
        
        WordProvider provider = providerMap.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown word provider type: " + type);
        }
        
        return provider;
    }

    public List<String> getAvailableProviderTypes() {
        return wordProviders.stream()
            .map(WordProvider::getType)
            .collect(Collectors.toList());
    }
}

