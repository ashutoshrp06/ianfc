package com.acp.cw3.ianfc.controller;

import com.acp.cw3.ianfc.generator.TelemetryGenerator;
import com.acp.cw3.ianfc.service.ScenarioEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/generator")
@RequiredArgsConstructor
public class GeneratorController {

    private final TelemetryGenerator generator;

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(
            @RequestBody Map<String, String> body) {
        String scenarioStr = body.getOrDefault("scenarioType", "NORMAL");
        ScenarioEngine.ScenarioType scenario;
        try {
            scenario = ScenarioEngine.ScenarioType.valueOf(scenarioStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid scenarioType. Valid values: NORMAL, CASCADE_FAILURE, FLAP");
        }
        generator.start(scenario);
        return ResponseEntity.ok(Map.of(
                "running", true,
                "scenarioType", scenario.name()
        ));
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        generator.stop();
        return ResponseEntity.ok(Map.of(
                "running", false,
                "eventCount", generator.getEventCount()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "running", generator.isRunning(),
                "eventCount", generator.getEventCount()
        ));
    }
}
