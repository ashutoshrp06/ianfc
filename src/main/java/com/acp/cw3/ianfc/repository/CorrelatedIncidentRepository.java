package com.acp.cw3.ianfc.repository;

import com.acp.cw3.ianfc.model.CorrelatedFault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CorrelatedIncidentRepository extends JpaRepository<CorrelatedFault, UUID> {

    List<CorrelatedFault> findByStatus(String status);

    List<CorrelatedFault> findByRootCauseDeviceIdAndStatus(String rootCauseDeviceId, String status);

    List<CorrelatedFault> findByStatusOrderByDetectedAtDesc(String status);
}

