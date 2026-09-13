package com.cloudide.execution.model;

import com.cloudide.execution.dto.ExecutionResponse;
import com.cloudide.execution.enums.ExecutionStatus;
import com.cloudide.execution.enums.ProgrammingLanguage;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActiveExecutionSession {

    private final String executionId;
    private final ProgrammingLanguage language;
    private final String containerName;
    private final Path tempDir;
    private final Process process;
    private final OutputStream stdinStream;
    private final int timeoutSeconds;
    private final int maxOutputSize;
    private final StringBuilder stdoutBuffer = new StringBuilder();
    private final StringBuilder stderrBuffer = new StringBuilder();
    private final AtomicBoolean outputLimitExceeded = new AtomicBoolean(false);
    private final long startTime = System.currentTimeMillis();
    private final CompletableFuture<ExecutionResponse> completionFuture = new CompletableFuture<>();

    private volatile ExecutionStatus status = ExecutionStatus.RUNNING;
    private volatile Integer exitCode = null;
    private volatile Long executionTimeMs = null;
    private volatile ScheduledFuture<?> watchdogFuture = null;

    public ActiveExecutionSession(
            String executionId,
            ProgrammingLanguage language,
            String containerName,
            Path tempDir,
            Process process,
            OutputStream stdinStream,
            int timeoutSeconds,
            int maxOutputSize
    ) {
        this.executionId = executionId;
        this.language = language;
        this.containerName = containerName;
        this.tempDir = tempDir;
        this.process = process;
        this.stdinStream = stdinStream;
        this.timeoutSeconds = timeoutSeconds;
        this.maxOutputSize = maxOutputSize;
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    public String getExecutionId() {
        return executionId;
    }

    public ProgrammingLanguage getLanguage() {
        return language;
    }

    public String getContainerName() {
        return containerName;
    }

    public Path getTempDir() {
        return tempDir;
    }

    public Process getProcess() {
        return process;
    }

    public OutputStream getStdinStream() {
        return stdinStream;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getMaxOutputSize() {
        return maxOutputSize;
    }

    public StringBuilder getStdoutBuffer() {
        return stdoutBuffer;
    }

    public StringBuilder getStderrBuffer() {
        return stderrBuffer;
    }

    public AtomicBoolean getOutputLimitExceeded() {
        return outputLimitExceeded;
    }

    public long getStartTime() {
        return startTime;
    }

    public CompletableFuture<ExecutionResponse> getCompletionFuture() {
        return completionFuture;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
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

    public ScheduledFuture<?> getWatchdogFuture() {
        return watchdogFuture;
    }

    public void setWatchdogFuture(ScheduledFuture<?> watchdogFuture) {
        this.watchdogFuture = watchdogFuture;
    }

    public String getStdout() {
        synchronized (stdoutBuffer) {
            return stdoutBuffer.toString();
        }
    }

    public String getStderr() {
        synchronized (stderrBuffer) {
            return stderrBuffer.toString();
        }
    }
}
