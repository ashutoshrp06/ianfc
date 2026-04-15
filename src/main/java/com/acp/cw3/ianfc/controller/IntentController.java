package com.acp.cw3.ianfc.controller;

import com.acp.cw3.ianfc.dto.IntentDTO;
import com.acp.cw3.ianfc.model.Intent;
import com.acp.cw3.ianfc.repository.IntentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/intents")
@RequiredArgsConstructor
public class IntentController {

    private final IntentRepository intentRepository;

    @GetMapping
    public ResponseEntity<List<IntentDTO>> getAll() {
        List<IntentDTO> dtos = intentRepository.findAll()
                .stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{intentId}")
    public ResponseEntity<IntentDTO> getOne(@PathVariable UUID intentId) {
        Intent intent = intentRepository.findById(intentId)
                .orElseThrow(() -> new NoSuchElementException("Intent not found: " + intentId));
        return ResponseEntity.ok(toDTO(intent));
    }

    @PostMapping
    public ResponseEntity<IntentDTO> create(@Valid @RequestBody IntentDTO dto) {
        Intent intent = fromDTO(dto);
        Intent saved = intentRepository.save(intent);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
    }

    @PutMapping("/{intentId}")
    public ResponseEntity<IntentDTO> update(
            @PathVariable UUID intentId,
            @Valid @RequestBody IntentDTO dto) {
        Intent existing = intentRepository.findById(intentId)
                .orElseThrow(() -> new NoSuchElementException("Intent not found: " + intentId));
        existing.setName(dto.getName());
        existing.setIntentType(dto.getIntentType());
        existing.setTargetEntity(dto.getTargetEntity());
        existing.setTargetRegion(dto.getTargetRegion());
        existing.setThresholdValue(dto.getThresholdValue());
        existing.setThresholdUnit(dto.getThresholdUnit());
        if (dto.getSeverity() != null) existing.setSeverity(dto.getSeverity());
        if (dto.getActive() != null)   existing.setActive(dto.getActive());
        return ResponseEntity.ok(toDTO(intentRepository.save(existing)));
    }

    @DeleteMapping("/{intentId}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID intentId) {
        Intent existing = intentRepository.findById(intentId)
                .orElseThrow(() -> new NoSuchElementException("Intent not found: " + intentId));
        existing.setActive(false);
        intentRepository.save(existing);
        return ResponseEntity.noContent().build();
    }

    // ---- Mappers ----

    private IntentDTO toDTO(Intent i) {
        return IntentDTO.builder()
                .intentId(i.getIntentId())
                .name(i.getName())
                .intentType(i.getIntentType())
                .targetEntity(i.getTargetEntity())
                .targetRegion(i.getTargetRegion())
                .thresholdValue(i.getThresholdValue())
                .thresholdUnit(i.getThresholdUnit())
                .severity(i.getSeverity())
                .active(i.getActive())
                .createdAt(i.getCreatedAt())
                .build();
    }

    private Intent fromDTO(IntentDTO dto) {
        return Intent.builder()
                .name(dto.getName())
                .intentType(dto.getIntentType())
                .targetEntity(dto.getTargetEntity())
                .targetRegion(dto.getTargetRegion())
                .thresholdValue(dto.getThresholdValue())
                .thresholdUnit(dto.getThresholdUnit())
                .severity(dto.getSeverity())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
    }
}
