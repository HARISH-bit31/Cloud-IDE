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
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    public ExecutionMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.registry.gauge("cloud_ide_active_executions", activeExecutions);
    }

    public void incrementExecutionStarted(String language) {
        activeExecutions.incrementAndGet();
        getOrCreateCounter("cloud_ide_executions_total", "language", sanitize(language), "status", "STARTED").increment();
    }

    public void recordExecutionCompleted(String language, String status, long durationMs) {
        activeExecutions.decrementAndGet();
        String safeLanguage = sanitize(language);
        String safeStatus = sanitize(status);

        getOrCreateCounter("cloud_ide_executions_total", "language", safeLanguage, "status", safeStatus).increment();

        Timer.builder("cloud_ide_execution_duration_seconds")
                .description("Duration of code execution in seconds")
                .tag("language", safeLanguage)
                .tag("status", safeStatus)
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordExecutionFailure(String language, String reason) {
        getOrCreateCounter("cloud_ide_execution_failures_total", "language", sanitize(language), "reason", sanitize(reason)).increment();
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
