package com.cloudide.cloudide.dto;

import com.cloudide.cloudide.enums.ExecutionStatus;

public class ExecutionResponse {

    private String executionId;
    private ExecutionStatus status;
    private String stdout;
    private String stderr;
    private Integer exitCode;
    private Long executionTimeMs;
    private String errorDetails;

    public ExecutionResponse() {
    }

    public ExecutionResponse(
            String executionId,
            ExecutionStatus status,
            String stdout,
            String stderr,
            Integer exitCode,
            Long executionTimeMs,
            String errorDetails
    ) {
        this.executionId = executionId;
        this.status = status;
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitCode = exitCode;
        this.executionTimeMs = executionTimeMs;
        this.errorDetails = errorDetails;
    }

    public ExecutionResponse(
            ExecutionStatus status,
            String stdout,
            String stderr,
            Integer exitCode,
            Long executionTimeMs,
            String errorDetails
    ) {
        this(null, status, stdout, stderr, exitCode, executionTimeMs, errorDetails);
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String executionId;
        private ExecutionStatus status;
        private String stdout;
        private String stderr;
        private Integer exitCode;
        private Long executionTimeMs;
        private String errorDetails;

        public Builder executionId(String executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder status(ExecutionStatus status) {
            this.status = status;
            return this;
        }

        public Builder stdout(String stdout) {
            this.stdout = stdout;
            return this;
        }

        public Builder stderr(String stderr) {
            this.stderr = stderr;
            return this;
        }

        public Builder exitCode(Integer exitCode) {
            this.exitCode = exitCode;
            return this;
        }

        public Builder executionTimeMs(Long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }

        public Builder errorDetails(String errorDetails) {
            this.errorDetails = errorDetails;
            return this;
        }

        public ExecutionResponse build() {
            return new ExecutionResponse(executionId, status, stdout, stderr, exitCode, executionTimeMs, errorDetails);
        }
    }
}

