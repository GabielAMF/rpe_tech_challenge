package com.rpe.clientmanager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class SqsConfig {

    /**
     * Messages are plain JSON. By default Spring Cloud AWS also adds a header with this service's Java class
     * name, which the consumer (another service with other classes) can't use; it is turned off here.
     */
    @Bean
    SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient, ObjectMapper objectMapper) {
        return SqsTemplate.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .configureDefaultConverter(converter -> {
                    converter.setObjectMapper(objectMapper);
                    converter.doNotSendPayloadTypeHeader();
                })
                .build();
    }
}
