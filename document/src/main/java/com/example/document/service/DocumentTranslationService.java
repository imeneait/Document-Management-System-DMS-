package com.example.document.service;

import com.example.document.entity.Document;
import com.example.document.model.DocumentTranslationRequest;
import com.example.document.model.TranslationResponseCallback;
import com.example.document.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class DocumentTranslationService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${translation.service.url:http://gemini-translation-service:8080}")
    private String translationServiceUrl;

    /**
     * Request translation of a document title
     * @param request Document translation request containing document ID and target language
     * @return True if request was successfully sent
     */
    public boolean requestTitleTranslation(DocumentTranslationRequest request) {
        // Verify document exists
        Document document = documentRepository.findById(Long.parseLong(request.getDocumentId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found with ID: " + request.getDocumentId()));

        try {
            // Prepare translation request
            Map<String, Object> translationRequest = new HashMap<>();
            translationRequest.put("documentId", document.getId().toString());
            translationRequest.put("text", document.getTitle());
            translationRequest.put("targetLanguage", request.getTargetLanguage());
            translationRequest.put("callbackUrl", "/api/documents/translations/callback");

            // Send translation request to Gemini Translation Service
            restTemplate.postForObject(
                    translationServiceUrl + "/api/translate",
                    translationRequest,
                    Void.class
            );
            
            return true;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to request translation: " + e.getMessage(), e);
        }
    }

    /**
     * Update a document with its translated title
     * @param callback Translation response containing document ID, language, and translated text
     * @return Updated document
     */
    public Document updateTranslatedTitle(TranslationResponseCallback callback) {
        try {
            // Find the document
            Optional<Document> documentOpt = documentRepository.findById(Long.parseLong(callback.getDocumentId()));
            
            if (documentOpt.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found with ID: " + callback.getDocumentId());
            }
            
            Document document = documentOpt.get();
            
            // Add the translated title
            if (document.getTranslatedTitles() == null) {
                document.setTranslatedTitles(new HashMap<>());
            }
            
            document.getTranslatedTitles().put(callback.getLanguage(), callback.getTranslatedText());
            
            // Save and return the updated document
            return documentRepository.save(document);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid document ID format: " + callback.getDocumentId());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to update translated title: " + e.getMessage(), e);
        }
    }
}