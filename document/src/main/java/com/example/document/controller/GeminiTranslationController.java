package com.example.document.controller;

import com.example.document.model.GeminiTranslationRequest;
import com.example.document.model.GeminiTranslationResponse;
import com.example.document.service.GeminiTranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gemini")
public class GeminiTranslationController {

    @Autowired
    private GeminiTranslationService geminiTranslationService;

    @PostMapping("/translate")
    public ResponseEntity<?> translateDocumentTitle(
            @RequestBody GeminiTranslationRequest request) {
        
        try {
            if (request.getDocumentId() == null || request.getTargetLanguage() == null) {
                return ResponseEntity.badRequest()
                    .body("Both documentId and targetLanguage are required");
            }
            
            String translatedTitle = geminiTranslationService.translateDocumentTitle(
                    request.getDocumentId(), 
                    request.getTargetLanguage());
            
            GeminiTranslationResponse response = new GeminiTranslationResponse();
            response.setDocumentId(request.getDocumentId());
            response.setTargetLanguage(request.getTargetLanguage());
            response.setTranslatedTitle(translatedTitle);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Error translating document title: " + e.getMessage());
        }
    }
}