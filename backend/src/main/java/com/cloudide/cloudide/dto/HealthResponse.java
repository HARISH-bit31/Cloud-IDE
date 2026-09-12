package com.cloudide.cloudide.dto;

import java.time.LocalDateTime;

public class HealthResponse {
    private String status;
    private String service;
    private LocalDateTime timestamp;

    public HealthResponse() {
    }

    public HealthResponse(String status, String service, LocalDateTime timestamp) {
        this.status = status;
        this.service = service;
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String status;
        private String service;
        private LocalDateTime timestamp;

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder service(String service) {
            this.service = service;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public HealthResponse build() {
            return new HealthResponse(status, service, timestamp);
        }
    }
}
