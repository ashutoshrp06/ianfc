package com.acp.cw3.ianfc.repository;

import com.acp.cw3.ianfc.model.IntentViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IntentViolationRepository extends JpaRepository<IntentViolation, UUID> {

    List<IntentViolation> findByStatus(String status);

    List<IntentViolation> findByStatusOrderByViolatedAtDesc(String status);

    List<IntentViolation> findByIntent_IntentIdAndStatus(UUID faultId, String status);

    List<IntentViolation> findByFault_FaultIdAndStatus(UUID faultId, String status);
}
