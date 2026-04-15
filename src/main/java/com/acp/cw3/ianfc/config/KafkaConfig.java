package com.acp.cw3.ianfc.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topic.telemetry-raw}")
    private String telemetryRawTopic;

    @Value("${kafka.topic.faults-correlated}")
    private String faultsCorrelatedTopic;

    @Bean
    public NewTopic telemetryRawTopic() {
        return TopicBuilder.name(telemetryRawTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic faultsCorrelatedTopic() {
        return TopicBuilder.name(faultsCorrelatedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
