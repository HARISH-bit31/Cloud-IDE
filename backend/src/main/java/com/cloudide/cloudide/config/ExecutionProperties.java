package com.cloudide.cloudide.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "execution")
public class ExecutionProperties {

    @org.springframework.beans.factory.annotation.Value("${execution.worker.url:${execution.worker-url:http://localhost:8090}}")
    private String workerUrl = "http://localhost:8090";

    @org.springframework.beans.factory.annotation.Value("${execution.worker.secret:${execution.worker-secret:${EXECUTION_WORKER_SECRET:}}}")
    private String workerSecret;

    @org.springframework.beans.factory.annotation.Value("${execution.timeout-seconds:10}")
    private int timeoutSeconds = 10;

    @org.springframework.beans.factory.annotation.Value("${execution.queue.capacity:${EXECUTION_QUEUE_CAPACITY:20}}")
    private int queueCapacity = 20;

    @org.springframework.beans.factory.annotation.Value("${execution.worker.concurrency:${EXECUTION_WORKER_CONCURRENCY:4}}")
    private int workerConcurrency = 4;

    @PostConstruct
    public void validateConfiguration() {
        if (workerSecret == null || workerSecret.isBlank() || "replace-with-a-long-random-secret".equalsIgnoreCase(workerSecret)) {
            throw new IllegalStateException("FATAL: EXECUTION_WORKER_SECRET is not configured for backend. Execution worker authentication requires a valid secret.");
        }
    }

    public String getWorkerUrl() {
        return workerUrl;
    }

    public void setWorkerUrl(String workerUrl) {
        this.workerUrl = workerUrl;
    }

    public String getWorkerSecret() {
        return workerSecret;
    }

    public void setWorkerSecret(String workerSecret) {
        this.workerSecret = workerSecret;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getWorkerConcurrency() {
        return workerConcurrency;
    }

    public void setWorkerConcurrency(int workerConcurrency) {
        this.workerConcurrency = workerConcurrency;
    }
}
