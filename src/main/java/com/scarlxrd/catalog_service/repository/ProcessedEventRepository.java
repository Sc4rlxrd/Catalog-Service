package com.scarlxrd.catalog_service.repository;

import com.scarlxrd.catalog_service.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}