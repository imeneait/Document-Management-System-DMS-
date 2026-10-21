package com.example.geminitranslationservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topic.translation-request}")
    private String translationRequestTopic;

    @Value("${kafka.topic.translation-response}")
    private String translationResponseTopic;

    // Create translation request topic
    @Bean
    public NewTopic translationRequestTopic() {
        return TopicBuilder.name(translationRequestTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Create translation response topic
    @Bean
    public NewTopic translationResponseTopic() {
        return TopicBuilder.name(translationResponseTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
