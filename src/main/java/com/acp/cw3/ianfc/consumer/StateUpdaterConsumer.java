package com.acp.cw3.ianfc.consumer;

import com.acp.cw3.ianfc.model.TelemetryEvent;
import com.acp.cw3.ianfc.service.DeviceStateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StateUpdaterConsumer {

    private final DeviceStateService deviceStateService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics       = "${kafka.topic.telemetry-raw}",
            groupId      = "state-updater",
            concurrency  = "1"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            TelemetryEvent event = objectMapper.readValue(record.value(), TelemetryEvent.class);
            deviceStateService.updateState(event);
            deviceStateService.updateFsm(event);
            log.debug("StateUpdaterConsumer received: deviceId={} type={}",
                    event.getDeviceId(), event.getEventType());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("StateUpdaterConsumer failed to process record offset={}", record.offset(), e);
            ack.acknowledge(); // ack to avoid poison-pill blocking the partition
        }
    }
}
