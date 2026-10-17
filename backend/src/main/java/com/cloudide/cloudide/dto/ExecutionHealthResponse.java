package com.cloudide.cloudide.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutionHealthResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("docker")
    private String docker;

    @JsonProperty("workerAvailable")
    private boolean workerAvailable;

    @JsonProperty("dockerAvailable")
    private boolean dockerAvailable;

    @JsonProperty("dockerVersion")
    private String dockerVersion;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("imagesStatus")
    private Map<String, Boolean> imagesStatus;

    @JsonProperty("activeExecutions")
    private int activeExecutions;

    @JsonProperty("maxConcurrent")
    private int maxConcurrent;

    public ExecutionHealthResponse() {
    }

    public ExecutionHealthResponse(String status, String docker, boolean workerAvailable, boolean dockerAvailable,
                                   String dockerVersion, String message, Long timestamp, Map<String, Boolean> imagesStatus) {
        this(status, docker, workerAvailable, dockerAvailable, dockerVersion, message, timestamp, imagesStatus, 0, 0);
    }

    public ExecutionHealthResponse(String status, String docker, boolean workerAvailable, boolean dockerAvailable,
                                   String dockerVersion, String message, Long timestamp, Map<String, Boolean> imagesStatus,
                                   int activeExecutions, int maxConcurrent) {
        this.status = status;
        this.docker = docker;
        this.workerAvailable = workerAvailable;
        this.dockerAvailable = dockerAvailable;
        this.dockerVersion = dockerVersion;
        this.message = message;
        this.timestamp = timestamp;
        this.imagesStatus = imagesStatus;
        this.activeExecutions = activeExecutions;
        this.maxConcurrent = maxConcurrent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDocker() {
        return docker;
    }

    public void setDocker(String docker) {
        this.docker = docker;
    }

    @JsonProperty("workerAvailable")
    public boolean isWorkerAvailable() {
        return workerAvailable;
    }

    @JsonProperty("workerAvailable")
    public boolean getWorkerAvailable() {
        return workerAvailable;
    }

    @JsonProperty("workerAvailable")
    public void setWorkerAvailable(boolean workerAvailable) {
        this.workerAvailable = workerAvailable;
    }

    @JsonProperty("dockerAvailable")
    public boolean isDockerAvailable() {
        return dockerAvailable;
    }

    @JsonProperty("dockerAvailable")
    public boolean getDockerAvailable() {
        return dockerAvailable;
    }

    @JsonProperty("dockerAvailable")
    public void setDockerAvailable(boolean dockerAvailable) {
        this.dockerAvailable = dockerAvailable;
    }

    public String getDockerVersion() {
        return dockerVersion;
    }

    public void setDockerVersion(String dockerVersion) {
        this.dockerVersion = dockerVersion;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Boolean> getImagesStatus() {
        return imagesStatus;
    }

    public void setImagesStatus(Map<String, Boolean> imagesStatus) {
        this.imagesStatus = imagesStatus;
    }

    public int getActiveExecutions() {
        return activeExecutions;
    }

    public void setActiveExecutions(int activeExecutions) {
        this.activeExecutions = activeExecutions;
    }

    public int getMaxConcurrent() {
        return maxConcurrent;
    }

    public void setMaxConcurrent(int maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String status;
        private String docker;
        private boolean workerAvailable;
        private boolean dockerAvailable;
        private String dockerVersion;
        private String message;
        private Long timestamp;
        private Map<String, Boolean> imagesStatus;
        private int activeExecutions;
        private int maxConcurrent;

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder docker(String docker) {
            this.docker = docker;
            return this;
        }

        public Builder workerAvailable(boolean workerAvailable) {
            this.workerAvailable = workerAvailable;
            return this;
        }

        public Builder dockerAvailable(boolean dockerAvailable) {
            this.dockerAvailable = dockerAvailable;
            return this;
        }

        public Builder dockerVersion(String dockerVersion) {
            this.dockerVersion = dockerVersion;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder imagesStatus(Map<String, Boolean> imagesStatus) {
            this.imagesStatus = imagesStatus;
            return this;
        }

        public Builder activeExecutions(int activeExecutions) {
            this.activeExecutions = activeExecutions;
            return this;
        }

        public Builder maxConcurrent(int maxConcurrent) {
            this.maxConcurrent = maxConcurrent;
            return this;
        }

        public ExecutionHealthResponse build() {
            return new ExecutionHealthResponse(status, docker, workerAvailable, dockerAvailable, dockerVersion, message, timestamp, imagesStatus, activeExecutions, maxConcurrent);
        }
    }
}
