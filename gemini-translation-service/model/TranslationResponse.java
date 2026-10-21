package com.example.geminitranslationservice.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranslationResponse {
    private String originalText;
    private String translatedText;
    private String targetLanguage;
    private String requestId;
    
    // Constructor matching your existing code, but adding requestId
    public TranslationResponse(String originalText, String translatedText, String targetLanguage) {
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.targetLanguage = targetLanguage;
        this.requestId = null;
    }
}
