package com.acp.cw3.ianfc.consumer;

import com.acp.cw3.ianfc.model.CorrelatedFault;
import com.acp.cw3.ianfc.model.Intent;
import com.acp.cw3.ianfc.model.IntentViolation;
import com.acp.cw3.ianfc.repository.CorrelatedIncidentRepository;
import com.acp.cw3.ianfc.repository.IntentRepository;
import com.acp.cw3.ianfc.repository.IntentViolationRepository;
import com.acp.cw3.ianfc.service.IntentService;
import com.acp.cw3.ianfc.service.ViolationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IntentVerifierConsumer {

    private final IntentRepository intentRepository;
    private final IntentViolationRepository violationRepository;
    private final CorrelatedIncidentRepository incidentRepository;
    private final IntentService intentService;
    private final ViolationService violationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics      = "${kafka.topic.faults-correlated}",
            groupId     = "intent-verifier",
            concurrency = "1"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            CorrelatedFault fault = objectMapper.readValue(record.value(), CorrelatedFault.class);

            // Use a managed JPA reference to avoid detached entity issues on the FK
            CorrelatedFault managedFault = incidentRepository.getReferenceById(fault.getFaultId());

            List<Intent> activeIntents = intentRepository
                    .findByActiveTrueAndTargetEntity(fault.getRootCauseDeviceId());

            for (Intent intent : activeIntents) {
                if (intentService.isViolated(intent, fault)) {
                    double observed = intentService.extractObservedValue(intent, fault);
                    IntentViolation violation = violationService.create(intent, managedFault, observed);
                    IntentViolation saved = violationRepository.save(violation);
                    violationService.indexActiveViolation(
                            intent.getIntentId().toString(),
                            saved.getViolationId().toString()
                    );
                    log.info("Intent violated: intentId={} violationId={} faultId={} device={}",
                            intent.getIntentId(), saved.getViolationId(),
                            fault.getFaultId(), fault.getRootCauseDeviceId());
                }
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("IntentVerifierConsumer failed to process record offset={}", record.offset(), e);
            ack.acknowledge();
        }
    }
}