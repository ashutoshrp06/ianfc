package com.acp.cw3.ianfc.repository;

import com.acp.cw3.ianfc.model.Intent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IntentRepository extends JpaRepository<Intent, UUID> {

    List<Intent> findByActiveTrue();

    List<Intent> findByActiveTrueAndTargetEntity(String targetEntity);

    List<Intent> findByActiveTrueAndTargetRegion(String targetRegion);
}
