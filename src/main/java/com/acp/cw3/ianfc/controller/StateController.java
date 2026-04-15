package com.acp.cw3.ianfc.controller;

import com.acp.cw3.ianfc.dto.DeviceStateDTO;
import com.acp.cw3.ianfc.service.DeviceStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/state")
@RequiredArgsConstructor
public class StateController {

    private final DeviceStateService deviceStateService;

    @GetMapping("/devices")
    public ResponseEntity<List<DeviceStateDTO>> getAllDevices() {
        List<DeviceStateDTO> dtos = deviceStateService.getAllDeviceStates().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<DeviceStateDTO> getDevice(@PathVariable String deviceId) {
        Map<Object, Object> state = deviceStateService.getDeviceState(deviceId);
        if (state.isEmpty()) throw new NoSuchElementException("Device not found: " + deviceId);
        return ResponseEntity.ok(toDto(state));
    }

    private DeviceStateDTO toDto(Map<Object, Object> m) {
        String deviceId = (String) m.get("deviceId");
        String latencyRaw = (String) m.get("latencyMs");
        String lossRaw    = (String) m.get("packetLossPercent");
        return DeviceStateDTO.builder()
                .deviceId(deviceId)
                .linkState((String) m.get("linkState"))
                .bgpState((String) m.get("bgpState"))
                .fsmState(deviceId != null ? deviceStateService.getFsmState(deviceId) : "UNKNOWN")
                .latencyMs(latencyRaw != null ? Double.parseDouble(latencyRaw) : null)
                .packetLossPercent(lossRaw != null ? Double.parseDouble(lossRaw) : null)
                .lastUpdated((String) m.get("lastUpdated"))
                .region((String) m.get("region"))
                .build();
    }
}