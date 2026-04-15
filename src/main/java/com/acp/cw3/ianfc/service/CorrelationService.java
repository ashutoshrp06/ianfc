package com.acp.cw3.ianfc.service;

import com.acp.cw3.ianfc.model.CorrelatedFault;
import com.acp.cw3.ianfc.model.TelemetryEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorrelationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String ALARM_WINDOW_KEY = "alarms:window:";
    private static final String CORR_LOCK_KEY    = "correlation:lock:";
    private static final String DEVICE_FSM_KEY   = "device:fsm:";

    @Value("${correlation.window.seconds:30}")
    private long windowSeconds;

    @Value("${correlation.alarm.threshold:3}")
    private int alarmThreshold;

    public void addToWindow(TelemetryEvent event) {
        redisTemplate.opsForZSet().add(
                ALARM_WINDOW_KEY + event.getDeviceId(),
                event.getEventId(),
                event.getTimestamp().toEpochMilli()
        );
        log.debug("Added to window: deviceId={} eventId={}", event.getDeviceId(), event.getEventId());
    }

    public void pruneWindow(String deviceId) {
        long cutoff = System.currentTimeMillis() - getWindowMillis();
        redisTemplate.opsForZSet().removeRangeByScore(ALARM_WINDOW_KEY + deviceId, 0, cutoff);
    }

    public long windowCount(String deviceId) {
        Long count = redisTemplate.opsForZSet().zCard(ALARM_WINDOW_KEY + deviceId);
        return count != null ? count : 0L;
    }

    // Returns true if lock was acquired (first correlation in this window bucket)
    public boolean acquireLock(String deviceId) {
        long bucket = System.currentTimeMillis() / getWindowMillis();
        String key = CORR_LOCK_KEY + deviceId + ":" + bucket;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(windowSeconds * 2));
        return Boolean.TRUE.equals(acquired);
    }

    public CorrelatedFault correlate(TelemetryEvent rootEvent) {
        long count = windowCount(rootEvent.getDeviceId());

        // Collect all other devices currently DEGRADED or FAILED
        List<String> affected = new ArrayList<>();
        Set<String> fsmKeys = redisTemplate.keys(DEVICE_FSM_KEY + "*");
        if (fsmKeys != null) {
            for (String key : fsmKeys) {
                String deviceId = key.replace(DEVICE_FSM_KEY, "");
                if (deviceId.equals(rootEvent.getDeviceId())) continue;
                String state = redisTemplate.opsForValue().get(key);
                if ("DEGRADED".equals(state) || "FAILED".equals(state)) {
                    affected.add(deviceId);
                }
            }
        }

        String affectedJson;
        try {
            affectedJson = objectMapper.writeValueAsString(affected);
        } catch (Exception e) {
            affectedJson = "[]";
        }

        UUID rootEventId = null;
        try {
            rootEventId = UUID.fromString(rootEvent.getEventId());
        } catch (Exception e) {
            log.warn("Could not parse rootEventId as UUID: {}", rootEvent.getEventId());
        }

        return CorrelatedFault.builder()
                .rootCauseDeviceId(rootEvent.getDeviceId())
                .rootCauseEventType(rootEvent.getEventType().name())
                .rootCauseEventId(rootEventId)
                .affectedDevices(affectedJson)
                .suppressedAlarmCount((int) count)
                .correlationWindowMs(getWindowMillis())
                .detectedAt(Instant.now())
                .status("ACTIVE")
                .build();
    }

    public int getAlarmThreshold() {
        return alarmThreshold;
    }

    public long getWindowMillis() {
        return Duration.ofSeconds(windowSeconds).toMillis();
    }
}