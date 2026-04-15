package com.acp.cw3.ianfc.service;

import com.acp.cw3.ianfc.model.TelemetryEvent;
import com.acp.cw3.ianfc.model.enums.EventType;
import com.acp.cw3.ianfc.model.enums.Severity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class ScenarioEngine {

    // Fixed device topology - simulates a small network
    public static final String ROOT_DEVICE      = "router-edge-01";
    public static final List<String> ALL_DEVICES = List.of(
            "router-edge-01",
            "switch-access-01",
            "switch-access-02",
            "router-core-01",
            "switch-dist-01"
    );
    public static final String REGION = "EU-WEST-1";

    public enum ScenarioType {
        NORMAL,
        CASCADE_FAILURE,
        FLAP
    }

    private ScenarioType activeScenario = ScenarioType.NORMAL;
    // Tracks cascade step so each call advances the scripted sequence
    private int cascadeStep = 0;

    private final Random random = new Random();

    public void setScenario(ScenarioType type) {
        this.activeScenario = type;
        this.cascadeStep = 0;
        log.info("Scenario set to: {}", type);
    }

    public ScenarioType getActiveScenario() {
        return activeScenario;
    }

    /**
     * TODO Phase 1: implement full normal scenario (random healthy events).
     * TODO Phase 2: implement cascade and flap scenarios.
     *
     * Returns one TelemetryEvent appropriate for the current scenario.
     */
    public TelemetryEvent nextEvent() {
        return switch (activeScenario) {
            case NORMAL          -> buildNormalEvent();
            case CASCADE_FAILURE -> buildCascadeEvent();
            case FLAP            -> buildFlapEvent();
        };
    }

    private TelemetryEvent buildNormalEvent() {
        String device = ALL_DEVICES.get(random.nextInt(ALL_DEVICES.size()));
        EventType[] types = EventType.values();
        EventType eventType = types[random.nextInt(types.length)];

        Severity severity;
        Double metricValue = null;
        String metricUnit = "ms";

        switch (eventType) {
            case LINK_DOWN, BGP_SESSION_DOWN -> severity = Severity.MAJOR;
            case LATENCY_SPIKE -> {
                severity = Severity.MINOR;
                metricValue = 50.0 + random.nextDouble() * 150;
            }
            case LATENCY_NORMAL -> {
                severity = Severity.INFO;
                metricValue = 1.0 + random.nextDouble() * 19;
            }
            case PACKET_LOSS -> {
                severity = Severity.WARNING;
                metricValue = random.nextDouble() * 5;
                metricUnit = "percent";
            }
            default -> severity = Severity.INFO;
        }

        return TelemetryEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .deviceId(device)
                .deviceType("ROUTER")
                .eventType(eventType)
                .severity(severity)
                .metricValue(metricValue)
                .metricUnit(metricUnit)
                .sourceInterface("ge-0/0/0")
                .timestamp(Instant.now())
                .region(REGION)
                .build();
    }

    private TelemetryEvent buildCascadeEvent() {
        // Scripted sequence: root device gets 3 fault events (steps 0,2,4) to hit threshold=3
        // interleaved with secondary device faults. Generator fires every 500ms so
        // all 3 root-device hits land in ~2s, well within the 30s correlation window.
        String[] secondaryDevices = {"switch-access-01", "switch-access-02", "router-core-01", "switch-dist-01"};

        String deviceId;
        EventType eventType;
        Severity severity;

        int step = cascadeStep++ % 7;
        switch (step) {
            case 0 -> { deviceId = ROOT_DEVICE;             eventType = EventType.LINK_DOWN;         severity = Severity.CRITICAL; }
            case 1 -> { deviceId = secondaryDevices[0];     eventType = EventType.LINK_DOWN;         severity = Severity.MAJOR; }
            case 2 -> { deviceId = ROOT_DEVICE;             eventType = EventType.BGP_SESSION_DOWN;  severity = Severity.CRITICAL; }
            case 3 -> { deviceId = secondaryDevices[1];     eventType = EventType.LINK_DOWN;         severity = Severity.MAJOR; }
            case 4 -> { deviceId = ROOT_DEVICE;             eventType = EventType.LINK_DOWN;         severity = Severity.CRITICAL; }
            case 5 -> { deviceId = secondaryDevices[2];     eventType = EventType.BGP_SESSION_DOWN;  severity = Severity.MAJOR; }
            default -> { deviceId = secondaryDevices[3];    eventType = EventType.LINK_DOWN;         severity = Severity.MAJOR; }
        }

        return TelemetryEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .deviceId(deviceId)
                .deviceType("ROUTER")
                .eventType(eventType)
                .severity(severity)
                .metricUnit("ms")
                .sourceInterface("ge-0/0/0")
                .timestamp(Instant.now())
                .region(REGION)
                .build();
    }

    private TelemetryEvent buildFlapEvent() {
        // Stub: alternates LINK_UP / LINK_DOWN - full logic in Phase 2
        EventType type = (cascadeStep++ % 2 == 0) ? EventType.LINK_DOWN : EventType.LINK_UP;
        return TelemetryEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .deviceId(ROOT_DEVICE)
                .deviceType("ROUTER")
                .eventType(type)
                .severity(Severity.MAJOR)
                .metricUnit("ms")
                .timestamp(Instant.now())
                .region(REGION)
                .build();
    }
}
