package com.orderflow.matching_engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.orderflow.matching_engine.engine.MatchingEngine;
import com.orderflow.matching_engine.engine.OrderEvent;
import com.orderflow.matching_engine.engine.OrderEventFactory;
import com.orderflow.matching_engine.service.BookPublisher;
import com.orderflow.matching_engine.service.TradePublisher;

import java.util.concurrent.Executors;

@SpringBootApplication
@EnableKafka
public class MatchingengineApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchingengineApplication.class, args);
        System.out.println("\n=== ORDERFLOW API SERVER STARTED ===\n");
    }

    // --- Disruptor Bean (Kept here as the core engine entry point) ---

    @Bean
    public RingBuffer<OrderEvent> ringBuffer(TradePublisher tradePublisher, BookPublisher bookPublisher) {
        int bufferSize = 1024 * 64;
        Disruptor<OrderEvent> disruptor = new Disruptor<>(
                new OrderEventFactory(),
                bufferSize,
                Executors.defaultThreadFactory(),
                ProducerType.SINGLE,
                new YieldingWaitStrategy());
        disruptor.handleEventsWith(new MatchingEngine(tradePublisher, bookPublisher));
        return disruptor.start();
    }
}