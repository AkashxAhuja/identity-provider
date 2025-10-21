package com.example.identity.repository;

import com.example.identity.model.DeviceMasterHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceMasterHistoryRepository extends JpaRepository<DeviceMasterHistory, Long> {
}
