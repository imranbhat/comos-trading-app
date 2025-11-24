package com.trading.executionsim.service;

import com.trading.executionsim.model.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@Service
public class ExecutionService {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);
    private static final String EVENTS_TOPIC = "orders.events";
    private final Random random = new Random();

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public ExecutionService(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @org.springframework.kafka.annotation.KafkaListener(
        topics = "orders.events",
        groupId = "execution-sim-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processOrderEvent(
            @Payload OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        
        try {
            // Only process ACCEPTED orders
            if (!"ACCEPTED".equals(event.getStatus())) {
                logger.debug("Skipping event with status: {}", event.getStatus());
                return;
            }

            logger.info("Processing ACCEPTED order for execution: {}", event.getOrderId());

            // Simulate execution delay (300-500ms)
            int delay = 300 + random.nextInt(200); // 300-500ms
            Thread.sleep(delay);

            // Create FILLED event
            OrderEvent filledEvent = createFilledEvent(event);
            kafkaTemplate.send(EVENTS_TOPIC, event.getOrderId(), filledEvent);
            logger.info("Order filled and event published: {}", event.getOrderId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Execution interrupted for order: {}", event.getOrderId());
        } catch (Exception e) {
            logger.error("Error processing order event: {}", event.getOrderId(), e);
        }
    }

    private OrderEvent createFilledEvent(OrderEvent acceptedEvent) {
        OrderEvent filledEvent = new OrderEvent();
        filledEvent.setEventId(UUID.randomUUID().toString());
        filledEvent.setOrderId(acceptedEvent.getOrderId());
        filledEvent.setStatus("FILLED");
        filledEvent.setSymbol(acceptedEvent.getSymbol());
        filledEvent.setSide(acceptedEvent.getSide());
        filledEvent.setQuantity(acceptedEvent.getQuantity());
        filledEvent.setFilledQuantity(acceptedEvent.getQuantity());
        filledEvent.setPrice(acceptedEvent.getPrice());
        
        // Simulate fill price (slight variation from order price)
        double priceVariation = (random.nextDouble() - 0.5) * 0.02; // ±1%
        double fillPrice = acceptedEvent.getPrice() * (1 + priceVariation);
        filledEvent.setFillPrice(fillPrice);
        
        filledEvent.setTimestamp(Instant.now());
        filledEvent.setMessage("Order filled successfully");
        return filledEvent;
    }
}

