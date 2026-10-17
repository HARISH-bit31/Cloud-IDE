package com.cloudide.cloudide.service;

import com.cloudide.cloudide.config.ExecutionProperties;
import com.cloudide.cloudide.dto.ExecutionHealthResponse;
import com.cloudide.cloudide.dto.ExecutionRequest;
import com.cloudide.cloudide.dto.ExecutionResponse;
import com.cloudide.cloudide.enums.ExecutionStatus;
import com.cloudide.cloudide.enums.ProgrammingLanguage;
import com.cloudide.cloudide.metrics.ExecutionMetrics;
import com.cloudide.cloudide.model.ExecutionRecord;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionDispatcherCacheTest {

    private ExecutionProperties properties;
    private ExecutionMetrics metrics;
    private ExecutionDispatcher dispatcher;
    private AtomicReference<ExecutionStatus> returnStatus;
    private CountDownLatch workerBlocker;

    @BeforeEach
    void setUp() {
        properties = new ExecutionProperties();
        properties.setWorkerSecret("test-secret-value-1234");
        properties.setQueueCapacity(10);
        properties.setWorkerConcurrency(2);
        properties.setCacheCapacity(3);
        properties.setCacheTtlMinutes(5);

        metrics = new ExecutionMetrics(new SimpleMeterRegistry());
        returnStatus = new AtomicReference<>(ExecutionStatus.SUCCESS);
        workerBlocker = new CountDownLatch(0);

        ExecutionClient mockClient = new ExecutionClient() {
            @Override
            public ExecutionResponse startExecution(String executionId, ExecutionRequest request) {
                try {
                    workerBlocker.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}

                return new ExecutionResponse(
                        executionId,
                        returnStatus.get(),
                        "output",
                        "",
                        0,
                        50L,
                        null,
                        null
                );
            }

            @Override
            public boolean sendInput(String executionId, String input) {
                return true;
            }

            @Override
            public ExecutionResponse getExecutionStatus(String executionId) {
                return new ExecutionResponse(
                        executionId,
                        returnStatus.get(),
                        "output",
                        "",
                        0,
                        50L,
                        null,
                        null
                );
            }

            @Override
            public ExecutionResponse stopExecution(String executionId) {
                return new ExecutionResponse(
                        executionId,
                        ExecutionStatus.STOPPED,
                        "",
                        "Execution stopped by user.",
                        130,
                        20L,
                        null,
                        null
                );
            }

            @Override
            public ExecutionHealthResponse getHealth() {
                return null;
            }
        };

        dispatcher = new ExecutionDispatcher(properties, metrics, mockClient);
        dispatcher.init();
    }

    @AfterEach
    void tearDown() {
        if (dispatcher != null) {
            dispatcher.shutdown();
        }
    }

    @Test
    @DisplayName("Active execution is retained in active registry while running")
    void testActiveExecutionRetention() throws InterruptedException {
        workerBlocker = new CountDownLatch(1); // Block worker to keep execution active

        String execId = "active-" + UUID.randomUUID();
        ExecutionRecord record = new ExecutionRecord(execId, 1L, "user@test.com", 1L, ProgrammingLanguage.PYTHON, "print(1)", null, 10);

        dispatcher.submit(record);

        // Allow dispatch loop to pick it up and enter worker
        Thread.sleep(100);

        assertEquals(1, dispatcher.getActiveCount(), "Active execution must remain in active registry");
        assertNotNull(dispatcher.getRecord(execId));
        assertEquals(ExecutionStatus.RUNNING, dispatcher.getRecord(execId).getStatus());

        // Unblock worker
        workerBlocker.countDown();

        // Wait for completion
        long deadline = System.currentTimeMillis() + 2000;
        while (dispatcher.getActiveCount() > 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }

        assertEquals(0, dispatcher.getActiveCount(), "Active registry must be cleared once finished");
        assertEquals(1, dispatcher.getCompletedCount(), "Completed registry must hold finished record");
        assertNotNull(dispatcher.getRecord(execId), "getRecord must resolve completed records from cache");
    }

    @Test
    @DisplayName("Completed executions cache bounds size by evicting oldest terminal records")
    void testCompletedCacheBoundedEviction() throws InterruptedException {
        // cacheCapacity is set to 3
        String[] ids = new String[5];
        for (int i = 0; i < 5; i++) {
            ids[i] = "exec-" + i;
            ExecutionRecord record = new ExecutionRecord(ids[i], 1L, "user@test.com", 1L, ProgrammingLanguage.PYTHON, "print(" + i + ")", null, 10);
            dispatcher.submit(record);
            Thread.sleep(150); // allow execution to complete
        }

        long deadline = System.currentTimeMillis() + 3000;
        while (dispatcher.getActiveCount() > 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }

        // Cache capacity was 3, so size must not exceed 3
        assertTrue(dispatcher.getCompletedCount() <= 3, "Completed registry size must not exceed capacity bound: " + dispatcher.getCompletedCount());

        // Oldest executions (ids[0], ids[1]) should be evicted
        assertNull(dispatcher.getRecord(ids[0]), "Oldest execution id[0] should be evicted");
        assertNull(dispatcher.getRecord(ids[1]), "Oldest execution id[1] should be evicted");

        // Most recent executions (ids[2], ids[3], ids[4]) must be retained
        assertNotNull(dispatcher.getRecord(ids[3]), "Recent execution id[3] must be retained");
        assertNotNull(dispatcher.getRecord(ids[4]), "Recent execution id[4] must be retained");
    }

    @Test
    @DisplayName("Stopping queued or active execution moves it to completed registry with STOPPED status")
    void testStopExecutionTransitionsToCompleted() throws InterruptedException {
        workerBlocker = new CountDownLatch(1);

        String execId = "stop-test-" + UUID.randomUUID();
        ExecutionRecord record = new ExecutionRecord(execId, 1L, "user@test.com", 1L, ProgrammingLanguage.PYTHON, "import time; time.sleep(5)", null, 10);
        dispatcher.submit(record);

        Thread.sleep(100);

        ExecutionResponse stopRes = dispatcher.stopExecution(execId);
        assertNotNull(stopRes);
        assertEquals(ExecutionStatus.STOPPED, stopRes.getStatus());

        workerBlocker.countDown();

        assertEquals(0, dispatcher.getActiveCount());
        ExecutionRecord fetched = dispatcher.getRecord(execId);
        assertNotNull(fetched);
        assertEquals(ExecutionStatus.STOPPED, fetched.getStatus());
    }
}
