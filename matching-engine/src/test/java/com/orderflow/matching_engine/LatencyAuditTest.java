package com.orderflow.matching_engine;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.orderflow.matching_engine.engine.OrderEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Random;

public class LatencyAuditTest {

    @Test
    public void runMidProjectLatencyAudit() {
        // Initialize a standalone Disruptor ring buffer for pure performance
        // benchmarking
        Disruptor<OrderEvent> disruptor = new Disruptor<>(
                OrderEvent::new,
                1024 * 1024,
                DaemonThreadFactory.INSTANCE);

        // Dummy event handler to consume events during the benchmark
        disruptor.handleEventsWith((event, sequence, endOfBatch) -> {
            // Simulates core matching engine consumption
        });

        disruptor.start();
        RingBuffer<OrderEvent> ringBuffer = disruptor.getRingBuffer();

        int totalOrders = 100000;
        long[] latencies = new long[totalOrders];
        Random random = new Random();

        System.out.println("\n==================================================");
        System.out.println("⚡ ORDERFLOW LMAX DISRUPTOR: LATENCY AUDIT ⚡");
        System.out.println("==================================================");

        // --- JVM WARMUP PHASE ---
        System.out.println("Warming up JVM JIT Compiler (10,000 blank iterations)...");
        for (int i = 0; i < 10000; i++) {
            long sequence = ringBuffer.next();
            try {
                OrderEvent event = ringBuffer.get(sequence);
                event.set(-1, "WARMUP", BigDecimal.ZERO, BigDecimal.ZERO, "BUY");
            } finally {
                ringBuffer.publish(sequence);
            }
        }

        // --- ACTUAL BENCHMARK ---
        System.out.println("Firing " + totalOrders + " orders into the Ring Buffer...");
        for (int i = 0; i < totalOrders; i++) {
            double randomPrice = 60000 + (5000 * random.nextDouble());
            long randomQty = 1L + random.nextInt(10);
            String side = random.nextBoolean() ? "BUY" : "SELL";

            long startNano = System.nanoTime();

            long sequence = ringBuffer.next();
            try {
                OrderEvent event = ringBuffer.get(sequence);
                // Wrap the randomized primitive values in BigDecimal
                event.set(i, "BTC-USD", BigDecimal.valueOf(randomPrice), BigDecimal.valueOf(randomQty), side);
            } finally {
                ringBuffer.publish(sequence);
            }

            long endNano = System.nanoTime();
            latencies[i] = (endNano - startNano);
        }

        Arrays.sort(latencies);
        long p99 = latencies[(int) (totalOrders * 0.99)] / 1000; // Convert nanoseconds to microseconds

        System.out.println("\n[ AUDIT RESULTS ]");
        System.out.println("99th Percentile Latency: " + p99 + " μs");
        System.out.println("==================================================\n");

        disruptor.shutdown();
    }
}