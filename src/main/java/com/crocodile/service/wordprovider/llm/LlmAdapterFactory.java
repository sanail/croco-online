package com.crocodile.service.wordprovider.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * LlmAdapterFactory - Factory for selecting active LLM adapter
 * 
 * This factory manages the selection of the active LLM adapter based on configuration.
 * It ensures that only one LLM adapter is active at a time, and that the selected
 * adapter is properly configured and available.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LlmAdapterFactory {

    @Value("${game.llm.active-provider:lm-studio}")
    private String activeProviderType;
    
    private final List<LlmAdapter> llmAdapters;

    /**
     * Get the currently active LLM adapter based on configuration
     * @return the active LLM adapter
     * @throws IllegalStateException if no adapter is configured or available
     */
    public LlmAdapter getActiveAdapter() {
        log.debug("Getting active LLM adapter, configured type: {}", activeProviderType);
        
        Map<String, LlmAdapter> adapterMap = llmAdapters.stream()
            .collect(Collectors.toMap(LlmAdapter::getType, Function.identity()));
        
        LlmAdapter adapter = adapterMap.get(activeProviderType);
        
        if (adapter == null) {
            log.error("No LLM adapter found for type: {}. Available types: {}", 
                     activeProviderType, adapterMap.keySet());
            throw new IllegalStateException(
                String.format("Unknown LLM adapter type: %s. Available types: %s", 
                             activeProviderType, adapterMap.keySet())
            );
        }
        
        if (!adapter.isAvailable()) {
            log.error("LLM adapter '{}' is not available. Check configuration and service status.", 
                     activeProviderType);
            throw new IllegalStateException(
                String.format("LLM adapter '%s' is not available. Check configuration and ensure the service is running.", 
                             activeProviderType)
            );
        }
        
        log.info("Active LLM adapter: {}", adapter.getType());
        return adapter;
    }

    /**
     * Get all available LLM adapter types
     * @return list of adapter type identifiers
     */
    public List<String> getAvailableAdapterTypes() {
        return llmAdapters.stream()
            .map(LlmAdapter::getType)
            .collect(Collectors.toList());
    }

    /**
     * Get list of currently available (configured and ready) adapters
     * @return list of available adapter type identifiers
     */
    public List<String> getReadyAdapterTypes() {
        return llmAdapters.stream()
            .filter(LlmAdapter::isAvailable)
            .map(LlmAdapter::getType)
            .collect(Collectors.toList());
    }
}
