package com.cloudide.cloudide.model;

import com.cloudide.cloudide.dto.ExecutionResponse;
import com.cloudide.cloudide.enums.ExecutionStatus;
import com.cloudide.cloudide.enums.ProgrammingLanguage;

import java.util.concurrent.atomic.AtomicBoolean;

public class ExecutionRecord {

    private final String executionId;
    private final Long userId;
    private final String userEmail;
    private final Long projectId;
    private final ProgrammingLanguage language;
    private String code;
    private String stdin;
    private final Integer timeoutSeconds;

    private volatile ExecutionStatus status;
    private final long createdAt;
    private volatile Long startedAt;
    private volatile Long completedAt;

    private volatile Integer exitCode;
    private volatile String stdout;
    private volatile String stderr;
    private volatile Long executionTimeMs;
    private volatile Double memoryUsedMb;
    private volatile String errorDetails;

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public ExecutionRecord(String executionId,
                           Long userId,
                           String userEmail,
                           Long projectId,
                           ProgrammingLanguage language,
                           String code,
                           String stdin,
                           Integer timeoutSeconds) {
        this.executionId = executionId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.projectId = projectId;
        this.language = language;
        this.code = code;
        this.stdin = stdin;
        this.timeoutSeconds = timeoutSeconds != null && timeoutSeconds > 0 ? timeoutSeconds : 10;
        this.status = ExecutionStatus.QUEUED;
        this.createdAt = System.currentTimeMillis();
        this.stdout = "";
        this.stderr = "";
    }

    public static boolean isTerminalStatus(ExecutionStatus status) {
        if (status == null) return false;
        return switch (status) {
            case SUCCESS, COMPILATION_ERROR, RUNTIME_ERROR, TIMEOUT,
                 OUTPUT_LIMIT_EXCEEDED, STOPPED, FAILED, SYSTEM_ERROR -> true;
            default -> false;
        };
    }

    public boolean isTerminal() {
        return isTerminalStatus(this.status);
    }

    public synchronized boolean transitionTo(ExecutionStatus newStatus) {
        if (newStatus == null) {
            return false;
        }
        if (isTerminal()) {
            return false; // Terminal state is immutable
        }
        if (newStatus == ExecutionStatus.RUNNING && this.startedAt == null) {
            this.startedAt = System.currentTimeMillis();
        }
        if (isTerminalStatus(newStatus) && this.completedAt == null) {
            this.completedAt = System.currentTimeMillis();
        }
        this.status = newStatus;
        return true;
    }

    public synchronized void updateFromResponse(ExecutionResponse response) {
        if (response == null) return;
        if (isTerminal() && !isTerminalStatus(response.getStatus())) {
            return;
        }

        if (response.getStatus() != null) {
            transitionTo(response.getStatus());
        }
        if (response.getStdout() != null) {
            this.stdout = response.getStdout();
        }
        if (response.getStderr() != null) {
            this.stderr = response.getStderr();
        }
        if (response.getExitCode() != null) {
            this.exitCode = response.getExitCode();
        }
        if (response.getExecutionTimeMs() != null) {
            this.executionTimeMs = response.getExecutionTimeMs();
        } else if (this.startedAt != null) {
            long end = this.completedAt != null ? this.completedAt : System.currentTimeMillis();
            this.executionTimeMs = end - this.startedAt;
        }
        if (response.getMemoryUsedMb() != null) {
            this.memoryUsedMb = response.getMemoryUsedMb();
        }
        if (response.getErrorDetails() != null) {
            this.errorDetails = response.getErrorDetails();
        }
    }

    public void markCancelled() {
        this.cancelled.set(true);
        transitionTo(ExecutionStatus.STOPPED);
        if (this.exitCode == null) {
            this.exitCode = 130;
        }
        if (this.stderr == null || this.stderr.isEmpty()) {
            this.stderr = "Execution stopped by user signal.";
        }
    }

    public boolean isCancelled() {
        return this.cancelled.get();
    }

    public ExecutionResponse toResponse() {
        long duration = 0L;
        if (this.executionTimeMs != null) {
            duration = this.executionTimeMs;
        } else if (this.startedAt != null) {
            long end = this.completedAt != null ? this.completedAt : System.currentTimeMillis();
            duration = end - this.startedAt;
        }

        return ExecutionResponse.builder()
                .executionId(this.executionId)
                .status(this.status)
                .exitCode(this.exitCode)
                .stdout(this.stdout != null ? this.stdout : "")
                .stderr(this.stderr != null ? this.stderr : "")
                .executionTimeMs(duration)
                .memoryUsedMb(this.memoryUsedMb)
                .errorDetails(this.errorDetails)
                .build();
    }

    // Getters and helper setters
    public String getExecutionId() {
        return executionId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public Long getProjectId() {
        return projectId;
    }

    public ProgrammingLanguage getLanguage() {
        return language;
    }

    public String getCode() {
        return code;
    }

    public void clearCode() {
        this.code = null;
    }

    public String getStdin() {
        return stdin;
    }

    public void setStdin(String stdin) {
        this.stdin = stdin;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Long getStartedAt() {
        return startedAt;
    }

    public Long getCompletedAt() {
        return completedAt;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
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

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public Double getMemoryUsedMb() {
        return memoryUsedMb;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
}
