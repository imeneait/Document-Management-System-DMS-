package com.example.document.model;

/**
 * Callback model for translation response from the Gemini Translation Service
 */
public class TranslationResponseCallback {
    private String documentId;
    private String language;
    private String translatedText;
    private String status;
    
    // Default constructor
    public TranslationResponseCallback() {
    }
    
    // Constructor with parameters
    public TranslationResponseCallback(String documentId, String language, String translatedText, String status) {
        this.documentId = documentId;
        this.language = language;
        this.translatedText = translatedText;
        this.status = status;
    }
    
    // Getters and setters
    public String getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getTranslatedText() {
        return translatedText;
    }
    
    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "TranslationResponseCallback{" +
                "documentId='" + documentId + '\'' +
                ", language='" + language + '\'' +
                ", translatedText='" + translatedText + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}