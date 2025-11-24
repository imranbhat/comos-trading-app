package com.trading.marketsim.service;

import com.trading.marketsim.model.Tick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class TickPublisher {
    private static final Logger logger = LoggerFactory.getLogger(TickPublisher.class);
    private static final String TOPIC = "market-data";
    
    private final KafkaTemplate<String, Tick> kafkaTemplate;
    private final Random random = new Random();
    
    private final List<String> symbols = Arrays.asList("AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "NFLX");
    
    // Base prices for each symbol
    private final double[] basePrices = {150.0, 350.0, 140.0, 120.0, 250.0, 300.0, 450.0, 400.0};
    
    public TickPublisher(KafkaTemplate<String, Tick> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedRate = 100) // Every 100ms
    public void publishTick() {
        int symbolIndex = random.nextInt(symbols.size());
        String symbol = symbols.get(symbolIndex);
        double basePrice = basePrices[symbolIndex];
        
        // Generate random price variation (±2%)
        double variation = (random.nextDouble() - 0.5) * 0.04; // -2% to +2%
        double price = basePrice * (1 + variation);
        
        // Update base price for next tick (random walk)
        basePrices[symbolIndex] = price;
        
        BigDecimal priceDecimal = BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
        BigDecimal bid = priceDecimal.subtract(BigDecimal.valueOf(0.01));
        BigDecimal ask = priceDecimal.add(BigDecimal.valueOf(0.01));
        Long volume = (long) (random.nextInt(10000) + 100);
        
        Tick tick = new Tick(
            symbol,
            priceDecimal,
            volume,
            Instant.now(),
            bid,
            ask
        );
        
        kafkaTemplate.send(TOPIC, symbol, tick);
        logger.debug("Published tick: {}", tick);
    }
}

