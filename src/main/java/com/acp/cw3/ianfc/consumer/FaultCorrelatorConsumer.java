package com.acp.cw3.ianfc.consumer;

import com.acp.cw3.ianfc.model.CorrelatedFault;
import com.acp.cw3.ianfc.model.TelemetryEvent;
import com.acp.cw3.ianfc.repository.CorrelatedIncidentRepository;
import com.acp.cw3.ianfc.service.CorrelationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FaultCorrelatorConsumer {

    private final CorrelationService correlationService;
    private final CorrelatedIncidentRepository incidentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.faults-correlated}")
    private String faultsCorrelatedTopic;

    @KafkaListener(
            topics      = "${kafka.topic.telemetry-raw}",
            groupId     = "fault-correlator",
            concurrency = "1"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            TelemetryEvent event = objectMapper.readValue(record.value(), TelemetryEvent.class);

            if (!isFaultEvent(event)) {
                ack.acknowledge();
                return;
            }

            correlationService.addToWindow(event);
            correlationService.pruneWindow(event.getDeviceId());
            long count = correlationService.windowCount(event.getDeviceId());

            if (count >= correlationService.getAlarmThreshold()
                    && correlationService.acquireLock(event.getDeviceId())) {
                CorrelatedFault fault = correlationService.correlate(event);
                incidentRepository.save(fault);
                String payload = objectMapper.writeValueAsString(fault);
                kafkaTemplate.send(faultsCorrelatedTopic, fault.getFaultId().toString(), payload);
                log.info("Correlated fault: faultId={} rootDevice={} suppressedCount={} affected={}",
                        fault.getFaultId(), fault.getRootCauseDeviceId(),
                        fault.getSuppressedAlarmCount(), fault.getAffectedDevices());
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("FaultCorrelatorConsumer failed to process record offset={}", record.offset(), e);
            ack.acknowledge();
        }
    }

    private boolean isFaultEvent(TelemetryEvent event) {
        if (event.getEventType() == null) return false;
        return switch (event.getEventType()) {
            case LINK_DOWN, BGP_SESSION_DOWN, LATENCY_SPIKE, PACKET_LOSS -> true;
            default -> false;
        };
    }
}