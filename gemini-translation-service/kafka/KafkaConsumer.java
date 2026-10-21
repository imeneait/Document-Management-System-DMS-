package com.example.geminitranslationservice.kafka;

import com.example.geminitranslationservice.model.TranslationRequest;
import com.example.geminitranslationservice.model.TranslationResponse;
import com.example.geminitranslationservice.service.TranslationService;
import com.example.geminitranslationservice.store.TranslationResultStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final TranslationService translationService;
    private final TranslationResultStore resultStore;

    @KafkaListener(topics = "translation-requests", groupId = "translation-group")
    public void consume(TranslationRequest request) {
        log.info("Consuming translation request: {}", request);

        try {
            String result = translationService.translateTextSync(request.getText(), request.getTargetLanguage());
            TranslationResponse response = new TranslationResponse(
                    request.getRequestId(),
                    request.getText(),
                    result,
                    request.getTargetLanguage()
            );
            resultStore.saveResult(request.getRequestId(), response);
            log.info("Stored translation result for requestId: {}", request.getRequestId());
        } catch (Exception e) {
            log.error("Error translating request: {}", request.getRequestId(), e);
        }
    }
}
