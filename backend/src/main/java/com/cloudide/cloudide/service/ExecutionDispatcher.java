package com.cloudide.cloudide.service;

import com.cloudide.cloudide.config.ExecutionProperties;
import com.cloudide.cloudide.dto.ExecutionRequest;
import com.cloudide.cloudide.dto.ExecutionResponse;
import com.cloudide.cloudide.enums.ExecutionStatus;
import com.cloudide.cloudide.exception.QueueFullException;
import com.cloudide.cloudide.metrics.ExecutionMetrics;
import com.cloudide.cloudide.model.ExecutionRecord;
import com.cloudide.cloudide.util.ExpiringBoundedCache;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ExecutionDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ExecutionDispatcher.class);

    private final ExecutionProperties properties;
    private final ExecutionMetrics metrics;
    private final ExecutionClient executionClient;

    private BlockingQueue<ExecutionRecord> executionQueue;
    private final ConcurrentHashMap<String, ExecutionRecord> activeRegistry = new ConcurrentHashMap<>();
    private ExpiringBoundedCache<String, ExecutionRecord> completedRegistry;
    private ScheduledExecutorService cacheCleanerExecutor;
    private Semaphore concurrencySemaphore;
    private ExecutorService dispatcherPool;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public ExecutionDispatcher(ExecutionProperties properties,
                               ExecutionMetrics metrics,
                               ExecutionClient executionClient) {
        this.properties = properties;
        this.metrics = metrics;
        this.executionClient = executionClient;
    }

    @PostConstruct
    public void init() {
        int queueCapacity = Math.max(1, properties.getQueueCapacity());
        int workerConcurrency = Math.max(1, properties.getWorkerConcurrency());
        int cacheCapacity = Math.max(1, properties.getCacheCapacity());
        int cacheTtlMinutes = Math.max(1, properties.getCacheTtlMinutes());

        log.info("Initializing ExecutionDispatcher with queue capacity: {}, worker concurrency: {}, cache capacity: {}, cache TTL: {}m",
                queueCapacity, workerConcurrency, cacheCapacity, cacheTtlMinutes);

        this.executionQueue = new LinkedBlockingQueue<>(queueCapacity);
        this.concurrencySemaphore = new Semaphore(workerConcurrency, true);
        this.dispatcherPool = Executors.newFixedThreadPool(workerConcurrency + 2);
        this.completedRegistry = new ExpiringBoundedCache<>(cacheCapacity, Duration.ofMinutes(cacheTtlMinutes));
        this.cacheCleanerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "execution-cache-cleaner");
            t.setDaemon(true);
            return t;
        });
        this.cacheCleanerExecutor.scheduleAtFixedRate(completedRegistry::cleanUp, 1, 1, TimeUnit.MINUTES);

        // Start dispatcher consumer thread
        dispatcherPool.submit(this::dispatchLoop);
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        if (cacheCleanerExecutor != null) {
            cacheCleanerExecutor.shutdownNow();
        }
        if (dispatcherPool != null) {
            dispatcherPool.shutdownNow();
        }
    }

    public ExecutionResponse submit(ExecutionRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("ExecutionRecord cannot be null");
        }

        activeRegistry.put(record.getExecutionId(), record);

        boolean enqueued = executionQueue.offer(record);
        if (!enqueued) {
            activeRegistry.remove(record.getExecutionId());
            String lang = record.getLanguage() != null ? record.getLanguage().name() : "UNKNOWN";
            metrics.recordExecutionQueueRejected(lang);
            log.warn("Execution queue is full ({}/{}). Rejecting execution {}",
                    executionQueue.size(), properties.getQueueCapacity(), record.getExecutionId());
            throw new QueueFullException("Execution queue is full (capacity: " + properties.getQueueCapacity() + "). Please try again later.");
        }

        metrics.updateQueueSize(executionQueue.size());
        String lang = record.getLanguage() != null ? record.getLanguage().name() : "UNKNOWN";
        metrics.recordExecutionQueued(lang);

        log.info("Execution {} enqueued for user {} (queue size: {}/{})",
                record.getExecutionId(), record.getUserEmail(), executionQueue.size(), properties.getQueueCapacity());

        return record.toResponse();
    }

    private void dispatchLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // Acquire concurrency permit for available worker slot
                concurrencySemaphore.acquire();

                ExecutionRecord record = null;
                while (running.get() && !Thread.currentThread().isInterrupted() && record == null) {
                    record = executionQueue.poll(200, TimeUnit.MILLISECONDS);
                }

                if (record == null) {
                    concurrencySemaphore.release();
                    continue;
                }

                metrics.updateQueueSize(executionQueue.size());

                if (record.isCancelled()) {
                    log.info("Execution {} was cancelled while in queue. Skipping worker dispatch.", record.getExecutionId());
                    activeRegistry.remove(record.getExecutionId());
                    completedRegistry.put(record.getExecutionId(), record);
                    concurrencySemaphore.release();
                    continue;
                }

                final ExecutionRecord taskRecord = record;
                // Dispatch worker task asynchronously
                dispatcherPool.submit(() -> executeOnWorker(taskRecord));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in ExecutionDispatcher dispatch loop: {}", e.getMessage(), e);
            }
        }
    }

    private void executeOnWorker(ExecutionRecord record) {
        String lang = record.getLanguage() != null ? record.getLanguage().name() : "UNKNOWN";
        try {
            if (record.isCancelled()) {
                log.info("Execution {} cancelled prior to worker invocation.", record.getExecutionId());
                return;
            }

            long waitDurationMs = System.currentTimeMillis() - record.getCreatedAt();
            record.transitionTo(ExecutionStatus.RUNNING);
            metrics.recordExecutionStarted(lang, waitDurationMs);

            log.info("Starting execution {} on worker (waited {}ms in queue)", record.getExecutionId(), waitDurationMs);

            ExecutionRequest req = new ExecutionRequest(
                    record.getLanguage(),
                    record.getCode(),
                    record.getStdin(),
                    record.getProjectId(),
                    null,
                    record.getTimeoutSeconds()
            );

            // Forward to execution worker
            ExecutionResponse startResponse = executionClient.startExecution(record.getExecutionId(), req);
            record.clearCode(); // Clear source code from memory once submitted
            record.updateFromResponse(startResponse);

            // If running, poll until terminal state or client finishes
            if (record.getStatus() == ExecutionStatus.RUNNING || record.getStatus() == ExecutionStatus.WAITING_FOR_INPUT) {
                long deadline = System.currentTimeMillis() + ((record.getTimeoutSeconds() + 4) * 1000L);
                while (!record.isTerminal() && System.currentTimeMillis() < deadline) {
                    Thread.sleep(80);
                    if (record.isCancelled()) {
                        break;
                    }
                    ExecutionResponse status = executionClient.getExecutionStatus(record.getExecutionId());
                    if (status != null) {
                        record.updateFromResponse(status);
                        if (record.isTerminal()) {
                            break;
                        }
                    }
                }

                if (!record.isTerminal() && System.currentTimeMillis() >= deadline) {
                    record.transitionTo(ExecutionStatus.TIMEOUT);
                    record.setExitCode(124);
                    record.setStderr("Execution timed out after " + record.getTimeoutSeconds() + " seconds.");
                }
            }

            if (record.isTerminal()) {
                long duration = record.getExecutionTimeMs() != null ? record.getExecutionTimeMs() : (System.currentTimeMillis() - record.getStartedAt());
                metrics.recordExecutionCompleted(lang, record.getStatus().name(), duration);
                log.info("Execution {} completed with status {} in {}ms", record.getExecutionId(), record.getStatus(), duration);
            }

        } catch (Exception e) {
            log.error("Execution failed for {}: {}", record.getExecutionId(), e.getMessage());
            record.transitionTo(ExecutionStatus.FAILED);
            record.setStderr("Execution engine failure: " + e.getMessage());
            metrics.recordExecutionFailure(lang, "DISPATCHER_ERROR");
        } finally {
            if (record.isTerminal()) {
                activeRegistry.remove(record.getExecutionId());
                completedRegistry.put(record.getExecutionId(), record);
            }
            concurrencySemaphore.release();
        }
    }

    public ExecutionRecord getRecord(String executionId) {
        if (executionId == null) {
            return null;
        }
        ExecutionRecord active = activeRegistry.get(executionId);
        if (active != null) {
            return active;
        }
        return completedRegistry != null ? completedRegistry.get(executionId) : null;
    }

    public ExecutionResponse stopExecution(String executionId) {
        if (executionId == null) {
            return null;
        }
        ExecutionRecord record = activeRegistry.get(executionId);
        if (record == null) {
            ExecutionRecord completed = completedRegistry != null ? completedRegistry.get(executionId) : null;
            return completed != null ? completed.toResponse() : null;
        }

        if (record.isTerminal()) {
            activeRegistry.remove(executionId);
            completedRegistry.put(executionId, record);
            return record.toResponse();
        }

        if (record.getStatus() == ExecutionStatus.QUEUED) {
            record.markCancelled();
            executionQueue.remove(record);
            activeRegistry.remove(executionId);
            completedRegistry.put(executionId, record);
            metrics.updateQueueSize(executionQueue.size());
            metrics.recordExecutionCompleted(record.getLanguage() != null ? record.getLanguage().name() : "UNKNOWN", ExecutionStatus.STOPPED.name(), 0);
            log.info("Execution {} cancelled while queued.", executionId);
            return record.toResponse();
        }

        // If running or waiting
        record.markCancelled();
        ExecutionResponse workerStopRes = executionClient.stopExecution(executionId);
        if (workerStopRes != null) {
            record.updateFromResponse(workerStopRes);
        }
        long duration = record.getExecutionTimeMs() != null ? record.getExecutionTimeMs() : 0;
        metrics.recordExecutionCompleted(record.getLanguage() != null ? record.getLanguage().name() : "UNKNOWN", ExecutionStatus.STOPPED.name(), duration);
        log.info("Execution {} stopped on worker.", executionId);
        activeRegistry.remove(executionId);
        completedRegistry.put(executionId, record);
        return record.toResponse();
    }

    public boolean sendInput(String executionId, String input) {
        if (executionId == null) {
            return false;
        }
        ExecutionRecord record = activeRegistry.get(executionId);
        if (record == null) {
            return false;
        }

        if (record.getStatus() == ExecutionStatus.RUNNING || record.getStatus() == ExecutionStatus.WAITING_FOR_INPUT) {
            return executionClient.sendInput(executionId, input);
        }

        log.warn("Cannot send input to execution {} in status {}", executionId, record.getStatus());
        return false;
    }

    public int getActiveCount() {
        return activeRegistry.size();
    }

    public int getCompletedCount() {
        return completedRegistry != null ? completedRegistry.size() : 0;
    }

    public int getQueueSize() {
        return executionQueue != null ? executionQueue.size() : 0;
    }

    public int getAvailableConcurrency() {
        return concurrencySemaphore != null ? concurrencySemaphore.availablePermits() : 0;
    }
}
