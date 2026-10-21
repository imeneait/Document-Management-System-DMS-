package com.example.geminitranslationservice.store;

import com.example.geminitranslationservice.model.TranslationResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class TranslationResultStore {
    private final ConcurrentHashMap<String, TranslationResponse> store = new ConcurrentHashMap<>();

    public void saveResult(String requestId, TranslationResponse response) {
        store.put(requestId, response);  // Save the translation result using requestId as the key
    }

    public TranslationResponse getResult(String requestId) {
        return store.get(requestId);  // Retrieve the translation result by requestId
    }

    public boolean exists(String requestId) {
        return store.containsKey(requestId);  // Check if a result exists for the given requestId
    }
}
