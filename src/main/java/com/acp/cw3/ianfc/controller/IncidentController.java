package com.acp.cw3.ianfc.controller;

import com.acp.cw3.ianfc.dto.IncidentDTO;
import com.acp.cw3.ianfc.model.CorrelatedFault;
import com.acp.cw3.ianfc.repository.CorrelatedIncidentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Slf4j
public class IncidentController {

    private final CorrelatedIncidentRepository incidentRepository;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<IncidentDTO>> getAll() {
        List<IncidentDTO> dtos = incidentRepository.findAll()
                .stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/active")
    public ResponseEntity<List<IncidentDTO>> getActive() {
        List<IncidentDTO> dtos = incidentRepository
                .findByStatusOrderByDetectedAtDesc("ACTIVE")
                .stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{faultId}")
    public ResponseEntity<IncidentDTO> getOne(@PathVariable UUID faultId) {
        CorrelatedFault fault = incidentRepository.findById(faultId)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + faultId));
        return ResponseEntity.ok(toDTO(fault));
    }

    private IncidentDTO toDTO(CorrelatedFault f) {
        List<String> affected = Collections.emptyList();
        if (f.getAffectedDevices() != null) {
            try {
                affected = objectMapper.readValue(
                        f.getAffectedDevices(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Could not parse affectedDevices JSON for fault {}", f.getFaultId());
            }
        }
        return IncidentDTO.builder()
                .faultId(f.getFaultId())
                .rootCauseDeviceId(f.getRootCauseDeviceId())
                .rootCauseEventType(f.getRootCauseEventType())
                .rootCauseEventId(f.getRootCauseEventId())
                .affectedDevices(affected)
                .suppressedAlarmCount(f.getSuppressedAlarmCount())
                .correlationWindowMs(f.getCorrelationWindowMs())
                .detectedAt(f.getDetectedAt())
                .resolvedAt(f.getResolvedAt())
                .status(f.getStatus())
                .build();
    }
}
