package com.acp.cw3.ianfc.service;

import com.acp.cw3.ianfc.model.CorrelatedFault;
import com.acp.cw3.ianfc.model.Intent;
import com.acp.cw3.ianfc.model.IntentViolation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViolationService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String VIOLATIONS_ACTIVE_KEY = "violations:active";

    // TODO Phase 3: build IntentViolation from intent + fault + observed value
    public IntentViolation create(Intent intent, CorrelatedFault fault, double observedValue) {
        return IntentViolation.builder()
                .intent(intent)
                .fault(fault)
                .intentName(intent.getName())
                .observedValue(observedValue)
                .thresholdValue(intent.getThresholdValue())
                .thresholdUnit(intent.getThresholdUnit())
                .violatedAt(Instant.now())
                .status("ACTIVE")
                .build();
    }

    public void indexActiveViolation(String intentId, String violationId) {
        redisTemplate.opsForHash().put(VIOLATIONS_ACTIVE_KEY, intentId, violationId);
        log.debug("Indexed active violation: intentId={} violationId={}", intentId, violationId);
    }

    public void removeActiveViolation(String intentId) {
        redisTemplate.opsForHash().delete(VIOLATIONS_ACTIVE_KEY, intentId);
    }
}
