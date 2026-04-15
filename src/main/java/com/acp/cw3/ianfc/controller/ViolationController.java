package com.acp.cw3.ianfc.controller;

import com.acp.cw3.ianfc.dto.ViolationDTO;
import com.acp.cw3.ianfc.model.IntentViolation;
import com.acp.cw3.ianfc.repository.IntentViolationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/violations")
@RequiredArgsConstructor
public class ViolationController {

    private final IntentViolationRepository violationRepository;

    @Transactional(readOnly = true)
    @GetMapping
    public ResponseEntity<List<ViolationDTO>> getAll() {
        List<ViolationDTO> dtos = violationRepository.findAll()
                .stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Transactional(readOnly = true)
    @GetMapping("/active")
    public ResponseEntity<List<ViolationDTO>> getActive() {
        List<ViolationDTO> dtos = violationRepository
                .findByStatusOrderByViolatedAtDesc("ACTIVE")
                .stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private ViolationDTO toDTO(IntentViolation v) {
        return ViolationDTO.builder()
                .violationId(v.getViolationId())
                .intentId(v.getIntent() != null ? v.getIntent().getIntentId() : null)
                .faultId(v.getFault() != null ? v.getFault().getFaultId() : null)
                .intentName(v.getIntentName())
                .observedValue(v.getObservedValue())
                .thresholdValue(v.getThresholdValue())
                .thresholdUnit(v.getThresholdUnit())
                .violatedAt(v.getViolatedAt())
                .resolvedAt(v.getResolvedAt())
                .status(v.getStatus())
                .build();
    }
}
