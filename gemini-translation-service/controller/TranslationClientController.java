package com.example.geminitranslationservice.controller;

import com.example.geminitranslationservice.model.TranslationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/translations")
@RequiredArgsConstructor
@Slf4j
public class TranslationClientController {

    // Store SseEmitters for clients waiting for responses by requestId
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    // Store completed translations by requestId for retrieval
    private final Map<String, TranslationResponse> completedTranslations = new ConcurrentHashMap<>();

    @GetMapping(value = "/subscribe/{requestId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToTranslation(@PathVariable String requestId) {
        log.info("Client subscribing to translation results for requestId: {}", requestId);
        
        // Check if we already have the translation result
        if (completedTranslations.containsKey(requestId)) {
            TranslationResponse response = completedTranslations.get(requestId);
            SseEmitter emitter = new SseEmitter(5000L); // 5 seconds timeout
            
            try {
                emitter.send(SseEmitter.event()
                        .name("translation")
                        .data(response));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            
            return emitter;
        }
        
        // Create a new emitter with a timeout
        SseEmitter emitter = new SseEmitter(60000L); // 1 minute timeout
        
        // Store the emitter with requestId as key
        emitters.put(requestId, emitter);
        
        // Remove emitter on completion or timeout
        emitter.onCompletion(() -> {
            log.info("SSE emitter completed for requestId: {}", requestId);
            emitters.remove(requestId);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE emitter timed out for requestId: {}", requestId);
            emitters.remove(requestId);
        });
        
        emitter.onError(e -> {
            log.error("SSE emitter error for requestId: {}", requestId, e);
            emitters.remove(requestId);
        });
        
        return emitter;
    }
    
    @KafkaListener(topics = "${kafka.topic.translation-response}", groupId = "${spring.kafka.consumer.group-id}-client")
    public void consumeTranslationResponse(TranslationResponse response) {
        log.info("Received translation response from Kafka: {}", response);
        
        String requestId = response.getRequestId();
        if (requestId == null) {
            log.warn("Received translation response without requestId: {}", response);
            return;
        }
        
        // Store the completed translation
        completedTranslations.put(requestId, response);
        
        // Check if any client is waiting for this translation
        SseEmitter emitter = emitters.get(requestId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("translation")
                        .data(response));
                emitter.complete();
            } catch (IOException e) {
                log.error("Error sending SSE event for requestId: {}", requestId, e);
                emitter.completeWithError(e);
            } finally {
                emitters.remove(requestId);
            }
        }
    }
    
    @GetMapping("/{requestId}")
    public TranslationResponse getTranslationResult(@PathVariable String requestId) {
        log.info("Retrieving translation result for requestId: {}", requestId);
        
        TranslationResponse response = completedTranslations.get(requestId);
        if (response == null) {
            log.warn("Translation result not found for requestId: {}", requestId);
            return new TranslationResponse(null, "Translation not found or not completed yet", null, requestId);
        }
        
        return response;
    }
}