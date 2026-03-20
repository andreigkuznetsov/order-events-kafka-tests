package com.example.kafkaorders.repository;

import com.example.kafkaorders.entity.ProcessedOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessedOrderRepository extends JpaRepository<ProcessedOrderEntity, Long> {
    Optional<ProcessedOrderEntity> findByOrderId(String orderId);
    boolean existsByEventId(String eventId);
    boolean existsByOrderId(String orderId);
}