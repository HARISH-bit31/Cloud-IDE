package com.cloudide.cloudide.dto;

import java.util.Map;

public class ExecutionHealthResponse {

    private String status;
    private String docker;
    private boolean dockerAvailable;
    private String dockerVersion;
    private String message;
    private Long timestamp;
    private Map<String, Boolean> imagesStatus;

    public ExecutionHealthResponse() {
    }

    public ExecutionHealthResponse(String status, String docker, boolean dockerAvailable, String dockerVersion,
                                   String message, Long timestamp, Map<String, Boolean> imagesStatus) {
        this.status = status;
        this.docker = docker;
        this.dockerAvailable = dockerAvailable;
        this.dockerVersion = dockerVersion;
        this.message = message;
        this.timestamp = timestamp;
        this.imagesStatus = imagesStatus;
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

    public boolean isDockerAvailable() {
        return dockerAvailable;
    }

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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String status;
        private String docker;
        private boolean dockerAvailable;
        private String dockerVersion;
        private String message;
        private Long timestamp;
        private Map<String, Boolean> imagesStatus;

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder docker(String docker) {
            this.docker = docker;
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

        public ExecutionHealthResponse build() {
            return new ExecutionHealthResponse(status, docker, dockerAvailable, dockerVersion, message, timestamp, imagesStatus);
        }
    }
}
