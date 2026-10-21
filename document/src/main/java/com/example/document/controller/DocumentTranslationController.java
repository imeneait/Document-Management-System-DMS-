package com.example.document.controller;

import com.example.document.entity.Document;
import com.example.document.model.DocumentTranslationRequest;
import com.example.document.model.TranslationResponseCallback;
import com.example.document.service.DocumentTranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents/translations")
public class DocumentTranslationController {

    @Autowired
    private DocumentTranslationService documentTranslationService;
    
    /**
     * Endpoint to request translation of a document title
     * @param request Document translation request with document ID and target language
     * @return HTTP 202 Accepted response
     */
    @PostMapping
    public ResponseEntity<String> requestTranslation(@RequestBody DocumentTranslationRequest request) {
        documentTranslationService.requestTitleTranslation(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Translation request for document " + request.getDocumentId() + 
                      " to " + request.getTargetLanguage() + " has been submitted");
    }
    
    /**
     * Callback endpoint for the translation service to send translated title
     * @param callback Translation response containing document ID, language, and translated text
     * @return HTTP 200 OK with updated document
     */
    @PostMapping("/callback")
    public ResponseEntity<Document> translationCallback(@RequestBody TranslationResponseCallback callback) {
        Document updatedDocument = documentTranslationService.updateTranslatedTitle(callback);
        return ResponseEntity.ok(updatedDocument);
    }
    
    /**
     * Endpoint to get translation status or all translations for a document
     * @param documentId ID of the document
     * @return HTTP 200 OK with translation status/info
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<Document> getDocumentTranslations(@PathVariable String documentId) {
        // This endpoint could be implemented to retrieve all translations for a document
        // For now, we'll just return a placeholder response
        return ResponseEntity.ok().build();
    }
}