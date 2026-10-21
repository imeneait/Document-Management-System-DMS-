package com.example.geminitranslationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import com.example.geminitranslationservice.store.TranslationResultStore;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.geminitranslationservice.model.TranslationResponse;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;

@Service
@Slf4j
public class TranslationService {

    private final WebClient webClient;
    private final TranslationResultStore resultStore; // Added TranslationResultStore

    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.api.url}")
    private String apiUrl;

    // Constructor injection of TranslationResultStore
    public TranslationService(WebClient.Builder webClientBuilder, 
                              TranslationResultStore resultStore, // Inject TranslationResultStore here
                              @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent}") String apiUrl) {
        this.webClient = webClientBuilder
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB buffer
            .build();
        this.resultStore = resultStore; // Initialize the resultStore
        this.apiUrl = apiUrl;
    }

    @Async
    public CompletableFuture<String> translateText(String text, String targetLanguage, String requestId) {
        log.info("Starting async translation request for text length: {} to language: {}", 
                 text != null ? text.length() : 0, targetLanguage);
        try {
            String translatedText = translateTextSync(text, targetLanguage);
            
            // Save the result to TranslationResultStore after translation
            TranslationResponse response = new TranslationResponse(text, translatedText, targetLanguage, requestId);
            resultStore.saveResult(requestId, response);
            
            return CompletableFuture.completedFuture(translatedText);
        } catch (Exception e) {
            log.error("Async translation failed", e);
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    public String translateTextSync(String text, String targetLanguage) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Text to translate cannot be empty");
        }
        
        if (targetLanguage == null || targetLanguage.isEmpty()) {
            throw new IllegalArgumentException("Target language cannot be empty");
        }
        
        log.info("Starting sync translation request for text length: {} to language: {}", 
                 text.length(), targetLanguage);
        
        try {
            Map<String, Object> requestBody = buildRequestBody(text, targetLanguage);
            
            // Log the request URL (without exposing full API key)
            String maskedKey = apiKey != null && apiKey.length() > 5 ? 
                              apiKey.substring(0, 3) + "..." : "[null or invalid key]";
            log.debug("Sending request to URL: {}", apiUrl + "?key=" + maskedKey);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                         .path(apiUrl.startsWith("http") ? "" : apiUrl)
                         .queryParam("key", apiKey)
                         .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            String translatedText = extractTranslation(response);
            log.info("Translation completed, result length: {}", 
                     translatedText != null ? translatedText.length() : 0);
            return translatedText;
        } catch (WebClientResponseException e) {
            log.error("API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Gemini API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Translation failed", e);
            throw new RuntimeException("Translation failed: " + e.getMessage(), e);
        }
    }

    // Retry logic for handling API rate limits or temporary failures
    public String translateTextWithRetry(String text, String targetLanguage, int maxRetries) {
        int attempts = 0;
        long retryDelayMs = 1000; // Start with 1 second delay
        Exception lastException = null;
        
        while (attempts < maxRetries) {
            try {
                return translateTextSync(text, targetLanguage);
            } catch (Exception e) {
                lastException = e;
                attempts++;
                
                // Check if this is a retryable error
                boolean shouldRetry = isRetryableError(e);
                
                if (!shouldRetry || attempts >= maxRetries) {
                    log.error("Translation failed after {} attempts", attempts, e);
                    throw new RuntimeException("Translation failed after " + attempts + " attempts: " + e.getMessage(), e);
                }
                
                log.warn("Translation attempt {} failed, retrying in {} ms", attempts, retryDelayMs);
                try {
                    Thread.sleep(retryDelayMs);
                    // Exponential backoff with jitter
                    retryDelayMs = (long) (retryDelayMs * 1.5 + Math.random() * 500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
        
        // This should never be reached due to the exception in the catch block
        throw new RuntimeException("Translation failed after " + maxRetries + " attempts", lastException);
    }
    
    private boolean isRetryableError(Exception e) {
        // Determine which exceptions should trigger a retry
        if (e instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) e;
            int statusCode = wcre.getStatusCode().value();
            // Retry on server errors (5xx) and specific client errors
            return statusCode >= 500 || statusCode == 429; // 429 is Too Many Requests
        }
        
        // Consider connection timeouts and similar issues as retryable
        return e.getMessage() != null && 
               (e.getMessage().contains("timeout") || 
                e.getMessage().contains("connection") ||
                e.getMessage().contains("reset"));
    }
    
    private Map<String, Object> buildRequestBody(String text, String targetLanguage) {
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> part = new HashMap<>();
        
        // Create the prompt for translation
        part.put("text", "Translate the following text to " + targetLanguage + ": " + text);
        parts.add(part);
        content.put("parts", parts);
        content.put("role", "user"); // Explicitly set role to user
        contents.add(content);
        requestBody.put("contents", contents);
        
        // Add generation config to improve translation quality
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.2); // Low temperature for more deterministic translations
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 1024);
        requestBody.put("generationConfig", generationConfig);
        
        return requestBody;
    }
    
    @SuppressWarnings("unchecked")
    private String extractTranslation(Map<String, Object> response) {
        try {
            if (response == null) {
                throw new RuntimeException("Empty response received from API");
            }
            
            log.debug("Response structure keys: {}", response.keySet());
            
            if (!response.containsKey("candidates")) {
                // Check for error responses
                if (response.containsKey("error")) {
                    Map<String, Object> error = (Map<String, Object>) response.get("error");
                    String message = error.containsKey("message") ? error.get("message").toString() : "Unknown API error";
                    throw new RuntimeException("API error: " + message);
                }
                
                throw new RuntimeException("Response format unexpected: missing 'candidates' key");
            }
            
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                // Check for error in the prompt content
                if (response.containsKey("promptFeedback")) {
                    Map<String, Object> feedback = (Map<String, Object>) response.get("promptFeedback");
                    if (feedback.containsKey("blockReason")) {
                        throw new RuntimeException("Translation blocked: " + feedback.get("blockReason"));
                    }
                }
                
                throw new RuntimeException("No translation candidates returned");
            }
            
            Map<String, Object> candidate = candidates.get(0);
            
            // Check if the candidate has a finishReason other than STOP
            if (candidate.containsKey("finishReason") && 
                !"STOP".equals(candidate.get("finishReason").toString())) {
                log.warn("Translation did not complete normally, finish reason: {}", 
                         candidate.get("finishReason"));
            }
            
            if (!candidate.containsKey("content")) {
                throw new RuntimeException("Invalid response format: missing 'content' in candidate");
            }
            
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            
            if (!content.containsKey("parts")) {
                throw new RuntimeException("Invalid response format: missing 'parts' in content");
            }
            
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            
            if (parts == null || parts.isEmpty()) {
                throw new RuntimeException("No parts in translation response");
            }
            
            Map<String, Object> part = parts.get(0);
            
            if (!part.containsKey("text")) {
                throw new RuntimeException("Invalid response format: missing 'text' in part");
            }
            
            String translatedText = (String) part.get("text");
            
            // Clean up the translated text - remove any "Translated to X:" prefix if present
            return cleanTranslatedText(translatedText);
        } catch (ClassCastException e) {
            log.error("Error parsing response structure", e);
            throw new RuntimeException("Invalid response format: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error extracting translation from response", e);
            throw new RuntimeException("Error extracting translation: " + e.getMessage(), e);
        }
    }
    
    private String cleanTranslatedText(String text) {
        if (text == null) {
            return "";
        }
        
        // Remove potential prefixes like "Translated to Spanish:" or "Here's the translation:"
        String[] prefixes = {
            "Translated to ", 
            "Here's the translation:", 
            "Here is the translation:", 
            "Translation:"
        };
        
        String result = text.trim();
        
        for (String prefix : prefixes) {
            if (result.startsWith(prefix)) {
                int colonIndex = result.indexOf(':', prefix.length() - 1);
                if (colonIndex > 0) {
                    result = result.substring(colonIndex + 1).trim();
                } else {
                    // If no colon found but starts with prefix, try to find the end of the language name
                    int spaceIndex = prefix.length();
                    while (spaceIndex < result.length() && 
                           Character.isLetter(result.charAt(spaceIndex))) {
                        spaceIndex++;
                    }
                    
                    if (spaceIndex < result.length() && result.charAt(spaceIndex) == ':') {
                        result = result.substring(spaceIndex + 1).trim();
                    }
                }
            }
        }
        
        return result;
    }
}
