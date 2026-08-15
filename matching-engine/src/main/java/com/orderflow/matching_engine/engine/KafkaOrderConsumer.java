package com.orderflow.matching_engine.engine;

import com.lmax.disruptor.RingBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderConsumer.class);
    private final RingBuffer<OrderEvent> ringBuffer;

    public KafkaOrderConsumer(RingBuffer<OrderEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    @KafkaListener(topics = "validated-orders", groupId = "matching-engine-group")
    public void consumeOrder(OrderEvent event) {
        long sequence = ringBuffer.next();
        try {
            OrderEvent inMemoryEvent = ringBuffer.get(sequence);

            // 1. Copy the standard fields
            inMemoryEvent.set(event.orderId, event.symbol, event.price, event.quantity, event.side);

            // 2. CRITICAL FIX: Copy the cancel flag so the engine knows to delete it!
            inMemoryEvent.isCancel = event.isCancel;

            log.debug("Ingested Order {} (Cancel: {}) from Kafka -> Handed to Disruptor",
                    event.orderId, event.isCancel);
        } finally {
            ringBuffer.publish(sequence);
        }
    }
}