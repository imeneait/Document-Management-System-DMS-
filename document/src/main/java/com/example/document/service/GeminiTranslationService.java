package com.example.document.service;

import com.example.document.entity.Document;
import com.example.document.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GeminiTranslationService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public String translateDocumentTitle(String documentId, String targetLanguage) {
        Document document = findDocumentById(documentId);
        if (document == null) {
            throw new RuntimeException("Document not found with ID: " + documentId);
        }
        
        String originalTitle = document.getTitle();
        if (originalTitle == null || originalTitle.isEmpty()) {
            throw new RuntimeException("Document has no title to translate");
        }
        
        // Create request body for Gemini API according to the expected format
        Map<String, Object> requestBody = new HashMap<>();
        
        // Create contents array
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        
        // Create parts array
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", "Translate the following text to " + targetLanguage + ": " + originalTitle);
        parts.add(part);
        
        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        // Make request to Gemini API
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        Map<String, Object> response = restTemplate.postForObject(geminiApiUrl, request, Map.class);
        
        // Extract translated text from response
        String translatedTitle = extractTranslatedTextFromResponse(response);
        
        return translatedTitle;
    }
    
    private Document findDocumentById(String documentId) {
        // Try various ID types since we don't know the exact type from the repository
        try {
            // Try as Long
            Long longId = Long.parseLong(documentId);
            Optional<Document> docOpt = documentRepository.findById(longId);
            if (docOpt.isPresent()) {
                return docOpt.get();
            }
        } catch (Exception ignored) {}
        
        try {
            // Try as Integer
            Integer intId = Integer.parseInt(documentId);
            Optional<Document> docOpt = documentRepository.findById(intId);
            if (docOpt.isPresent()) {
                return docOpt.get();
            }
        } catch (Exception ignored) {}
        
        try {
            // Try as String directly
            Optional<Document> docOpt = documentRepository.findById(documentId);
            if (docOpt.isPresent()) {
                return docOpt.get();
            }
        } catch (Exception ignored) {}
        
        // Try using custom query methods if available
        try {
            List<Document> documents = documentRepository.findAll();
            for (Document doc : documents) {
                if (documentId.equals(doc.getId().toString())) {
                    return doc;
                }
            }
        } catch (Exception ignored) {}
        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private String extractTranslatedTextFromResponse(Map<String, Object> response) {
        try {
            // Navigate through the response structure
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new RuntimeException("No candidates found in Gemini API response");
            }
            
            Map<String, Object> candidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            
            if (parts == null || parts.isEmpty()) {
                throw new RuntimeException("No parts found in Gemini API response");
            }
            
            Map<String, Object> part = parts.get(0);
            String translatedText = (String) part.get("text");
            
            if (translatedText == null) {
                throw new RuntimeException("No text found in Gemini API response");
            }
            
            return translatedText;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini API response: " + e.getMessage(), e);
        }
    }
}