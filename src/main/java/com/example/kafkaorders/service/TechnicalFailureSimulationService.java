package com.example.kafkaorders.service;

import com.example.kafkaorders.dto.OrderCreatedEvent;
import com.example.kafkaorders.exception.TransientProcessingException;
import org.springframework.stereotype.Service;

@Service
public class TechnicalFailureSimulationService {

    public void check(OrderCreatedEvent event) {
        if ("FORCE_RETRY".equals(event.userId())) {
            throw new TransientProcessingException("Simulated transient failure");
        }
    }
}
