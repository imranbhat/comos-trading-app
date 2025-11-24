package com.trading.orderapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.orderapi.model.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventPublisher.class);
    private static final String TOPIC = "orders.events";
    private static final String WS_DESTINATION = "/topic/orders";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public WebSocketEventPublisher(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = TOPIC, groupId = "order-api-group")
    public void consumeOrderEvent(OrderEvent event) {
        try {
            logger.info("Received order event: {}", event);
            messagingTemplate.convertAndSend(WS_DESTINATION, event);
            logger.debug("Published order event to WebSocket: {}", event.getOrderId());
        } catch (Exception e) {
            logger.error("Error publishing event to WebSocket", e);
        }
    }
}

