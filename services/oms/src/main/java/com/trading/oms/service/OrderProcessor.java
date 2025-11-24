package com.trading.oms.service;

import com.trading.oms.model.Order;
import com.trading.oms.model.OrderCommand;
import com.trading.oms.model.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderProcessor {
    private static final Logger logger = LoggerFactory.getLogger(OrderProcessor.class);
    private static final String EVENTS_TOPIC = "orders.events";

    private final OrderStore orderStore;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderProcessor(OrderStore orderStore, KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.orderStore = orderStore;
        this.kafkaTemplate = kafkaTemplate;
    }

    @org.springframework.kafka.annotation.KafkaListener(
        topics = "orders.commands",
        groupId = "oms-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processOrderCommand(
            @Payload OrderCommand command,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            logger.info("Received order command: {}", command.getOrderId());

            // Idempotency check
            if (orderStore.exists(command.getOrderId())) {
                logger.warn("Duplicate order command detected: {}", command.getOrderId());
                acknowledgment.acknowledge();
                return;
            }

            // Validate command
            if (!isValid(command)) {
                logger.error("Invalid order command: {}", command.getOrderId());
                OrderEvent rejectionEvent = createRejectionEvent(command, "Invalid order command");
                kafkaTemplate.send(EVENTS_TOPIC, command.getOrderId(), rejectionEvent);
                acknowledgment.acknowledge();
                return;
            }

            // Create and store order
            Order order = new Order(command);
            orderStore.save(order);

            // Publish ACCEPTED event
            OrderEvent acceptedEvent = createAcceptedEvent(order);
            kafkaTemplate.send(EVENTS_TOPIC, command.getOrderId(), acceptedEvent);
            logger.info("Order accepted and event published: {}", command.getOrderId());

            // Manual acknowledgment
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing order command: {}", command.getOrderId(), e);
            // In production, you might want to send to a dead letter queue
            acknowledgment.acknowledge();
        }
    }

    private boolean isValid(OrderCommand command) {
        return command.getOrderId() != null &&
               command.getSymbol() != null &&
               command.getSide() != null &&
               command.getQuantity() != null && command.getQuantity() > 0 &&
               command.getPrice() != null && command.getPrice() > 0 &&
               command.getOrderType() != null;
    }

    private OrderEvent createAcceptedEvent(Order order) {
        OrderEvent event = new OrderEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setOrderId(order.getOrderId());
        event.setStatus("ACCEPTED");
        event.setSymbol(order.getSymbol());
        event.setSide(order.getSide());
        event.setQuantity(order.getQuantity());
        event.setPrice(order.getPrice());
        event.setTimestamp(Instant.now());
        event.setMessage("Order accepted");
        return event;
    }

    private OrderEvent createRejectionEvent(OrderCommand command, String reason) {
        OrderEvent event = new OrderEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setOrderId(command.getOrderId());
        event.setStatus("REJECTED");
        event.setSymbol(command.getSymbol());
        event.setSide(command.getSide());
        event.setQuantity(command.getQuantity());
        event.setPrice(command.getPrice());
        event.setTimestamp(Instant.now());
        event.setMessage(reason);
        return event;
    }
}

