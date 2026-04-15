CREATE TABLE IF NOT EXISTS intents (
    intent_id       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    intent_type     VARCHAR(50)  NOT NULL,
    target_entity   VARCHAR(255) NOT NULL,
    target_region   VARCHAR(100),
    threshold_value NUMERIC(10, 2) NOT NULL,
    threshold_unit  VARCHAR(20)  NOT NULL,
    severity        VARCHAR(20)  NOT NULL DEFAULT 'MAJOR',
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS correlated_incidents (
    fault_id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    root_cause_device_id    VARCHAR(255) NOT NULL,
    root_cause_event_type   VARCHAR(50)  NOT NULL,
    root_cause_event_id     UUID,
    affected_devices        JSONB,
    suppressed_alarm_count  INTEGER      NOT NULL DEFAULT 0,
    correlation_window_ms   BIGINT,
    detected_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    resolved_at             TIMESTAMPTZ,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS intent_violations (
    violation_id    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    intent_id       UUID        NOT NULL REFERENCES intents (intent_id),
    fault_id        UUID        REFERENCES correlated_incidents (fault_id),
    intent_name     VARCHAR(255),
    observed_value  NUMERIC(10, 2),
    threshold_value NUMERIC(10, 2),
    threshold_unit  VARCHAR(20),
    violated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMPTZ,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
);

CREATE INDEX IF NOT EXISTS idx_intents_active_entity
    ON intents (active, target_entity);

CREATE INDEX IF NOT EXISTS idx_incidents_status_detected
    ON correlated_incidents (status, detected_at DESC);

CREATE INDEX IF NOT EXISTS idx_violations_status_violated
    ON intent_violations (status, violated_at DESC);

CREATE INDEX IF NOT EXISTS idx_violations_intent_id
    ON intent_violations (intent_id);
