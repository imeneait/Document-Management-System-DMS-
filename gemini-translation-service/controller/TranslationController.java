package com.example.geminitranslationservice.controller;

import com.example.geminitranslationservice.kafka.KafkaProducer;
import com.example.geminitranslationservice.store.TranslationResultStore;
import com.example.geminitranslationservice.model.TranslationRequest;
import com.example.geminitranslationservice.model.TranslationResponse;
import com.example.geminitranslationservice.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/translate")
@RequiredArgsConstructor
@Slf4j
public class TranslationController {

    private final TranslationService translationService;
    private final KafkaProducer kafkaProducer;
    private final TranslationResultStore resultStore; // Inject TranslationResultStore

    @PostMapping
    public ResponseEntity<?> translate(@RequestBody TranslationRequest request) {
        if (request.getText() == null || request.getTargetLanguage() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing text or targetLanguage"));
        }

        if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
            request.setRequestId(UUID.randomUUID().toString());
        }

        kafkaProducer.sendTranslationRequest(request);

        return ResponseEntity.accepted().body(Map.of(
            "requestId", request.getRequestId(),
            "message", "Your translation request is being processed asynchronously"
        ));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<?> getTranslationResult(@PathVariable String requestId) {
        TranslationResponse result = resultStore.getResult(requestId);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("message", "Translation still processing or not found"));
        }
        return ResponseEntity.ok(result);
    }
    
    // Synchronous translation endpoint (optional, for direct translations)
    @PostMapping("/sync")
    public ResponseEntity<?> translateSync(@RequestBody TranslationRequest request) {
        try {
            log.info("Received synchronous translation request: {}", request);
            
            // Validate request
            if (request.getText() == null || request.getText().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Text to translate cannot be empty"));
            }
            
            if (request.getTargetLanguage() == null || request.getTargetLanguage().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Target language cannot be empty"));
            }
            
            // Perform translation synchronously
            String translatedText = translationService.translateTextSync(request.getText(), request.getTargetLanguage());
            
            TranslationResponse response = new TranslationResponse(
                request.getText(),
                translatedText,
                request.getTargetLanguage(),
                request.getRequestId() != null ? request.getRequestId() : UUID.randomUUID().toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing translation request", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Translation failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("cause", e.getCause() != null ? e.getCause().getMessage() : "Unknown cause");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Server error");
        errorResponse.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
