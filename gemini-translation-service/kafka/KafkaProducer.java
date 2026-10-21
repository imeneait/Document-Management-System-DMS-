package com.example.geminitranslationservice.kafka;

import com.example.geminitranslationservice.model.TranslationRequest;
import com.example.geminitranslationservice.model.TranslationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.translation-request}")
    private String translationRequestTopic;

    @Value("${kafka.topic.translation-response}")
    private String translationResponseTopic;

    public CompletableFuture<SendResult<String, Object>> sendTranslationRequest(TranslationRequest request) {
        log.info("Sending translation request to Kafka: {}", request);
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                translationRequestTopic, 
                request.getRequestId(), // Use requestId as the key
                request
        );
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent translation request successfully: {} with offset: {}", 
                        request, result.getRecordMetadata().offset());
            } else {
                log.error("Unable to send translation request to Kafka: {}", request, ex);
            }
        });
        
        return future;
    }

    public CompletableFuture<SendResult<String, Object>> sendTranslationResponse(TranslationResponse response) {
        log.info("Sending translation response to Kafka: {}", response);
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                translationResponseTopic, 
                response.getRequestId(), // Use requestId as the key
                response
        );
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent translation response successfully: {} with offset: {}", 
                        response, result.getRecordMetadata().offset());
            } else {
                log.error("Unable to send translation response to Kafka: {}", response, ex);
            }
        });
        
        return future;
    }
}