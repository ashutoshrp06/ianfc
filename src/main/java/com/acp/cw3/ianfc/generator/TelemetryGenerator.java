package com.acp.cw3.ianfc.generator;

import com.acp.cw3.ianfc.model.TelemetryEvent;
import com.acp.cw3.ianfc.service.ScenarioEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetryGenerator {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ScenarioEngine scenarioEngine;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.telemetry-raw}")
    private String telemetryRawTopic;

    private final AtomicBoolean running   = new AtomicBoolean(false);
    private final AtomicLong   eventCount = new AtomicLong(0);

    @Scheduled(fixedDelayString = "${generator.interval.ms:500}")
    public void generate() {
        if (!running.get()) {
            return;
        }
        try {
            TelemetryEvent event = scenarioEngine.nextEvent();
            String payload = objectMapper.writeValueAsString(event);
            // Key on deviceId to preserve per-device ordering within a partition
            kafkaTemplate.send(telemetryRawTopic, event.getDeviceId(), payload);
            eventCount.incrementAndGet();
            log.debug("Produced event: deviceId={} type={}", event.getDeviceId(), event.getEventType());
        } catch (Exception e) {
            log.error("Failed to produce telemetry event", e);
        }
    }

    public void start(ScenarioEngine.ScenarioType scenarioType) {
        scenarioEngine.setScenario(scenarioType);
        eventCount.set(0);
        running.set(true);
        log.info("TelemetryGenerator started with scenario: {}", scenarioType);
    }

    public void stop() {
        running.set(false);
        log.info("TelemetryGenerator stopped. Total events produced: {}", eventCount.get());
    }

    public boolean isRunning() {
        return running.get();
    }

    public long getEventCount() {
        return eventCount.get();
    }
}
