package com.acp.cw3.ianfc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "correlated_incidents")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CorrelatedFault {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "fault_id")
    private UUID faultId;

    @Column(name = "root_cause_device_id", nullable = false)
    private String rootCauseDeviceId;

    @Column(name = "root_cause_event_type", nullable = false)
    private String rootCauseEventType;

    @Column(name = "root_cause_event_id")
    private UUID rootCauseEventId;

    // Stored as JSON string, maps to JSONB column in PostgreSQL.
    // Use Jackson to serialize/deserialize List<String> before persisting.
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "affected_devices", columnDefinition = "jsonb")
    private String affectedDevices;

    @Column(name = "suppressed_alarm_count")
    private Integer suppressedAlarmCount;

    @Column(name = "correlation_window_ms")
    private Long correlationWindowMs;

    @Column(name = "detected_at")
    private Instant detectedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "status")
    private String status;
}
