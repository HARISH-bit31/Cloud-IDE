package com.cloudide.cloudide.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ExecutionMetrics {

    private final MeterRegistry registry;
    private final AtomicInteger activeExecutions = new AtomicInteger(0);
    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    public ExecutionMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.registry.gauge("cloud_ide_active_executions", activeExecutions);
        this.registry.gauge("cloud_ide_execution_active", activeExecutions);
        this.registry.gauge("cloud_ide_execution_queue_size", queueSize);
    }

    public void updateQueueSize(int size) {
        queueSize.set(size);
    }

    public void recordExecutionQueued(String language) {
        getOrCreateCounter("cloud_ide_execution_queued_total", "language", sanitize(language)).increment();
    }

    public void recordExecutionQueueRejected(String language) {
        getOrCreateCounter("cloud_ide_execution_queue_rejected_total", "language", sanitize(language)).increment();
    }

    public void recordExecutionStarted(String language, long waitDurationMs) {
        activeExecutions.incrementAndGet();
        String safeLang = sanitize(language);
        getOrCreateCounter("cloud_ide_execution_started_total", "language", safeLang).increment();
        getOrCreateCounter("cloud_ide_executions_total", "language", safeLang, "status", "STARTED").increment();

        if (waitDurationMs >= 0) {
            Timer.builder("cloud_ide_execution_wait_duration")
                    .description("Wait duration in queue before execution starts")
                    .tag("language", safeLang)
                    .register(registry)
                    .record(waitDurationMs, TimeUnit.MILLISECONDS);
        }
    }

    public void recordExecutionCompleted(String language, String status, long durationMs) {
        activeExecutions.decrementAndGet();
        String safeLanguage = sanitize(language);
        String safeStatus = sanitize(status);

        getOrCreateCounter("cloud_ide_execution_completed_total", "language", safeLanguage, "status", safeStatus).increment();
        getOrCreateCounter("cloud_ide_executions_total", "language", safeLanguage, "status", safeStatus).increment();

        Timer.builder("cloud_ide_execution_duration_seconds")
                .description("Duration of code execution in seconds")
                .tag("language", safeLanguage)
                .tag("status", safeStatus)
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);

        Timer.builder("cloud_ide_execution_duration")
                .description("Duration of code execution in seconds")
                .tag("language", safeLanguage)
                .tag("status", safeStatus)
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordExecutionFailure(String language, String reason) {
        String safeLang = sanitize(language);
        String safeReason = sanitize(reason);
        getOrCreateCounter("cloud_ide_execution_failed_total", "language", safeLang, "reason", safeReason).increment();
        getOrCreateCounter("cloud_ide_execution_failures_total", "language", safeLang, "reason", safeReason).increment();
    }

    private Counter getOrCreateCounter(String name, String... tags) {
        String key = name + "_" + String.join("_", tags);
        return counters.computeIfAbsent(key, k -> Counter.builder(name)
                .tags(tags)
                .register(registry));
    }

    private String sanitize(String val) {
        if (val == null || val.isBlank()) {
            return "UNKNOWN";
        }
        return val.trim().toUpperCase();
    }
}
