package com.cloudide.execution.service;

import com.cloudide.execution.config.ExecutionWorkerProperties;
import com.cloudide.execution.dto.ExecutionHealthResponse;
import com.cloudide.execution.dto.ExecutionStartRequest;
import com.cloudide.execution.dto.ExecutionResponse;
import com.cloudide.execution.enums.ExecutionStatus;
import com.cloudide.execution.enums.ProgrammingLanguage;
import com.cloudide.execution.model.ActiveExecutionSession;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DockerSandboxService {

    private static final Logger log = LoggerFactory.getLogger(DockerSandboxService.class);

    private final ExecutionWorkerProperties properties;
    private final ConcurrentHashMap<String, ActiveExecutionSession> activeExecutions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ExecutionResponse> completedExecutions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService watchdogExecutor = Executors.newScheduledThreadPool(4);
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    public DockerSandboxService(ExecutionWorkerProperties properties) {
        this.properties = properties;
    }

    /**
     * Checks if Docker daemon is running and reachable on the host.
     */
    public boolean isDockerAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "info")
                    .redirectErrorStream(true)
                    .start();
            boolean completed = process.waitFor(4, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("Docker daemon check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns health status of Docker Engine, sandbox images, and current worker load.
     */
    public ExecutionHealthResponse getHealth() {
        boolean available = isDockerAvailable();
        String version = "Unknown";
        Map<String, Boolean> images = new HashMap<>();

        if (available) {
            try {
                Process process = new ProcessBuilder("docker", "version", "--format", "{{.Server.Version}}")
                        .redirectErrorStream(true)
                        .start();
                if (process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line = reader.readLine();
                        if (line != null && !line.isBlank()) {
                            version = line.trim();
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to read Docker version string: {}", e.getMessage());
            }

            // Check sandbox images
            images.put(properties.getJavaImage(), checkImageExists(properties.getJavaImage()));
            images.put(properties.getPythonImage(), checkImageExists(properties.getPythonImage()));
            images.put(properties.getCImage(), checkImageExists(properties.getCImage()));
            images.put(properties.getCppImage(), checkImageExists(properties.getCppImage()));
        }

        return ExecutionHealthResponse.builder()
                .status(available ? "UP" : "DOWN")
                .docker(available ? "CONNECTED" : "DISCONNECTED")
                .dockerAvailable(available)
                .dockerVersion(version)
                .message(available ? "Execution Worker & Docker Engine operational" : "Docker daemon unreachable")
                .imagesStatus(images)
                .activeExecutions(activeExecutions.size())
                .maxConcurrent(properties.getMaxConcurrent())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private boolean checkImageExists(String imageName) {
        try {
            Process process = new ProcessBuilder("docker", "image", "inspect", imageName)
                    .redirectErrorStream(true)
                    .start();
            boolean completed = process.waitFor(3, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Starts an asynchronous interactive code execution session inside an isolated ephemeral Docker container.
     */
    public ExecutionResponse startExecution(ExecutionStartRequest request) {
        String executionId = request.getExecutionId() != null && !request.getExecutionId().isBlank()
                ? request.getExecutionId()
                : UUID.randomUUID().toString();

        // 1. Concurrency limit check
        if (activeExecutions.size() >= properties.getMaxConcurrent()) {
            log.warn("Execution {} rejected: Concurrency limit reached ({}/{})",
                    executionId, activeExecutions.size(), properties.getMaxConcurrent());
            return ExecutionResponse.builder()
                    .executionId(executionId)
                    .status(ExecutionStatus.QUEUE_FULL)
                    .stdout("")
                    .stderr("Execution queue is full. Maximum concurrent executions (" + properties.getMaxConcurrent() + ") reached. Please try again shortly.")
                    .exitCode(-1)
                    .executionTimeMs(0L)
                    .errorDetails("Max concurrent limit reached")
                    .build();
        }

        // 2. Docker availability check
        if (!isDockerAvailable()) {
            log.error("Execution {} aborted: Docker daemon unreachable", executionId);
            return ExecutionResponse.builder()
                    .executionId(executionId)
                    .status(ExecutionStatus.SYSTEM_ERROR)
                    .stdout("")
                    .stderr("Docker execution engine is currently unreachable on worker.")
                    .exitCode(-1)
                    .executionTimeMs(0L)
                    .errorDetails("Docker daemon unreachable")
                    .build();
        }

        ProgrammingLanguage language = request.getLanguage();
        String code = request.getCode();
        String initialStdin = request.getStdin();

        String filename = getSourceFilename(language);
        String image = getImageName(language);
        String containerName = "cloudide-sandbox-" + executionId.substring(0, Math.min(executionId.length(), 8));

        Path tempDir = null;
        Process process = null;

        try {
            // Create temporary host directory
            tempDir = Files.createTempDirectory("cloudide-exec-" + executionId.substring(0, Math.min(executionId.length(), 8)));
            Path sourceFile = tempDir.resolve(filename);
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

            String containerScript = getContainerScript(language);

            List<String> command = new ArrayList<>();
            command.add("docker");
            command.add("run");
            command.add("--rm");
            command.add("-i");
            command.add("--name");
            command.add(containerName);

            if (properties.isNetworkDisabled()) {
                command.add("--network");
                command.add("none");
            }

            command.add("--memory");
            command.add(properties.getMemoryLimit());

            command.add("--cpus");
            command.add(properties.getCpuLimit());

            command.add("--pids-limit");
            command.add(String.valueOf(properties.getPidsLimit()));

            // Mount temporary directory as /workspace
            String hostPath = tempDir.toAbsolutePath().toString().replace('\\', '/');
            command.add("-v");
            command.add(hostPath + ":/workspace:rw");

            command.add("-w");
            command.add("/workspace");

            command.add(image);
            command.add("sh");
            command.add("-c");
            command.add(containerScript);

            log.info("Worker launching container {} (id: {}) for {} execution...", containerName, executionId, language);

            ProcessBuilder pb = new ProcessBuilder(command);
            process = pb.start();

            OutputStream stdinStream = process.getOutputStream();

            // Pipe initial stdin if provided
            if (initialStdin != null && !initialStdin.isEmpty()) {
                try {
                    stdinStream.write(initialStdin.getBytes(StandardCharsets.UTF_8));
                    stdinStream.flush();
                } catch (IOException e) {
                    log.debug("Initial stdin write note: {}", e.getMessage());
                }
            }

            int timeoutSeconds = request.getTimeoutSeconds() != null && request.getTimeoutSeconds() > 0
                    ? request.getTimeoutSeconds()
                    : properties.getTimeoutSeconds();

            ActiveExecutionSession session = new ActiveExecutionSession(
                    executionId,
                    language,
                    containerName,
                    tempDir,
                    process,
                    stdinStream,
                    timeoutSeconds,
                    properties.getMaxOutputSize()
            );

            activeExecutions.put(executionId, session);

            // Start asynchronous stream capture
            streamExecutor.submit(() -> captureStream(session.getProcess().getInputStream(), session.getStdoutBuffer(), session.getOutputLimitExceeded(), session.getMaxOutputSize()));
            streamExecutor.submit(() -> captureStream(session.getProcess().getErrorStream(), session.getStderrBuffer(), session.getOutputLimitExceeded(), session.getMaxOutputSize()));

            // Schedule timeout watchdog
            ScheduledFuture<?> watchdog = watchdogExecutor.schedule(() -> handleTimeout(session), timeoutSeconds, TimeUnit.SECONDS);
            session.setWatchdogFuture(watchdog);

            // Start completion monitor
            streamExecutor.submit(() -> monitorProcessCompletion(session));

            // Brief initial wait (40ms) for instant compilation errors or trivial runs
            try {
                session.getCompletionFuture().get(40, TimeUnit.MILLISECONDS);
                ExecutionResponse completed = completedExecutions.get(executionId);
                if (completed != null) {
                    return completed;
                }
            } catch (TimeoutException ignored) {
                // Active running as expected
            } catch (Exception e) {
                log.debug("Initial sync wait note: {}", e.getMessage());
            }

            return ExecutionResponse.builder()
                    .executionId(executionId)
                    .status(ExecutionStatus.RUNNING)
                    .stdout(session.getStdout())
                    .stderr(session.getStderr())
                    .exitCode(null)
                    .executionTimeMs(0L)
                    .build();

        } catch (Exception e) {
            log.error("Internal worker execution launch error: {}", e.getMessage(), e);
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            if (containerName != null) {
                killContainer(containerName);
            }
            if (tempDir != null) {
                deleteDirectoryRecursively(tempDir);
            }

            return ExecutionResponse.builder()
                    .executionId(executionId)
                    .status(ExecutionStatus.SYSTEM_ERROR)
                    .stdout("")
                    .stderr("Execution launch failed due to an internal system error.")
                    .exitCode(-1)
                    .executionTimeMs(0L)
                    .errorDetails(e.getMessage())
                    .build();
        }
    }

    /**
     * Submits interactive stdin input to an actively running execution session.
     */
    public boolean sendInput(String executionId, String input) {
        ActiveExecutionSession session = activeExecutions.get(executionId);
        if (session == null || !session.isAlive()) {
            return false;
        }

        try {
            String toSend = input != null ? input : "";
            if (!toSend.endsWith("\n")) {
                toSend += "\n";
            }
            OutputStream os = session.getStdinStream();
            os.write(toSend.getBytes(StandardCharsets.UTF_8));
            os.flush();
            log.info("Sent interactive stdin ({} bytes) to execution {}", toSend.length(), executionId);
            return true;
        } catch (IOException e) {
            log.warn("Failed to write interactive stdin to execution {}: {}", executionId, e.getMessage());
            return false;
        }
    }

    /**
     * Gets current live or completed execution status and outputs.
     */
    public ExecutionResponse getExecutionStatus(String executionId) {
        ActiveExecutionSession session = activeExecutions.get(executionId);
        if (session != null) {
            long elapsed = System.currentTimeMillis() - session.getStartTime();
            return ExecutionResponse.builder()
                    .executionId(executionId)
                    .status(session.isAlive() ? ExecutionStatus.RUNNING : session.getStatus())
                    .stdout(session.getStdout())
                    .stderr(session.getStderr())
                    .exitCode(session.getExitCode())
                    .executionTimeMs(elapsed)
                    .build();
        }

        return completedExecutions.get(executionId);
    }

    /**
     * Stops an active execution session immediately and kills container.
     */
    public ExecutionResponse stopExecution(String executionId) {
        ActiveExecutionSession session = activeExecutions.get(executionId);
        if (session != null) {
            session.setStatus(ExecutionStatus.STOPPED);
            if (session.getWatchdogFuture() != null) {
                session.getWatchdogFuture().cancel(true);
            }
            killContainer(session.getContainerName());
            if (session.getProcess().isAlive()) {
                session.getProcess().destroyForcibly();
            }
            long elapsed = System.currentTimeMillis() - session.getStartTime();
            ExecutionResponse response = ExecutionResponse.builder()
                    .executionId(executionId)
                    .status(ExecutionStatus.STOPPED)
                    .stdout(session.getStdout())
                    .stderr(session.getStderr() + "\nExecution stopped by user.")
                    .exitCode(130)
                    .executionTimeMs(elapsed)
                    .build();

            activeExecutions.remove(executionId);
            completedExecutions.put(executionId, response);
            session.getCompletionFuture().complete(response);
            cleanupSession(session);
            return response;
        }

        return completedExecutions.get(executionId);
    }

    private void monitorProcessCompletion(ActiveExecutionSession session) {
        try {
            int exitCode = session.getProcess().waitFor();
            // Allow up to 80ms for final stream chunks to flush into buffer
            Thread.sleep(80);

            long executionTimeMs = System.currentTimeMillis() - session.getStartTime();

            if (session.getWatchdogFuture() != null) {
                session.getWatchdogFuture().cancel(true);
            }

            if (session.getStatus() == ExecutionStatus.TIMEOUT || session.getStatus() == ExecutionStatus.STOPPED) {
                return;
            }

            String stdout = session.getStdout();
            String stderr = session.getStderr();

            ExecutionStatus status;
            if (session.getOutputLimitExceeded().get()) {
                status = ExecutionStatus.OUTPUT_LIMIT_EXCEEDED;
            } else if (exitCode == 0) {
                status = ExecutionStatus.SUCCESS;
            } else {
                status = classifyErrorStatus(session.getLanguage(), stderr, exitCode);
            }

            ExecutionResponse result = ExecutionResponse.builder()
                    .executionId(session.getExecutionId())
                    .status(status)
                    .stdout(stdout)
                    .stderr(stderr)
                    .exitCode(exitCode)
                    .executionTimeMs(executionTimeMs)
                    .build();

            session.setStatus(status);
            session.setExitCode(exitCode);
            session.setExecutionTimeMs(executionTimeMs);

            activeExecutions.remove(session.getExecutionId());
            completedExecutions.put(session.getExecutionId(), result);
            session.getCompletionFuture().complete(result);

            log.info("Execution {} finished: status={}, exitCode={}, time={}ms",
                    session.getExecutionId(), status, exitCode, executionTimeMs);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            cleanupSession(session);
        }
    }

    private void handleTimeout(ActiveExecutionSession session) {
        if (!session.isAlive() || session.getStatus() == ExecutionStatus.STOPPED) {
            return;
        }

        log.warn("Session {} (container: {}) timed out after {}s. Terminating container...",
                session.getExecutionId(), session.getContainerName(), session.getTimeoutSeconds());

        session.setStatus(ExecutionStatus.TIMEOUT);
        session.setExitCode(124);

        killContainer(session.getContainerName());
        if (session.getProcess().isAlive()) {
            session.getProcess().destroyForcibly();
        }

        long executionTimeMs = System.currentTimeMillis() - session.getStartTime();
        String stdout = session.getStdout();
        String stderr = session.getStderr();
        if (stderr.isEmpty()) {
            stderr = "Execution timed out after " + session.getTimeoutSeconds() + " seconds.";
        } else {
            stderr += "\nExecution timed out after " + session.getTimeoutSeconds() + " seconds.";
        }

        ExecutionResponse result = ExecutionResponse.builder()
                .executionId(session.getExecutionId())
                .status(ExecutionStatus.TIMEOUT)
                .stdout(stdout)
                .stderr(stderr)
                .exitCode(124)
                .executionTimeMs(executionTimeMs)
                .build();

        activeExecutions.remove(session.getExecutionId());
        completedExecutions.put(session.getExecutionId(), result);
        session.getCompletionFuture().complete(result);

        cleanupSession(session);
    }

    private void captureStream(InputStream is, StringBuilder buffer, AtomicBoolean outputExceeded, int maxOutput) {
        byte[] chunk = new byte[1024];
        int read;
        try (is) {
            while ((read = is.read(chunk)) != -1) {
                synchronized (buffer) {
                    if (buffer.length() + read > maxOutput) {
                        int remaining = Math.max(0, maxOutput - buffer.length());
                        if (remaining > 0) {
                            buffer.append(new String(chunk, 0, remaining, StandardCharsets.UTF_8));
                        }
                        outputExceeded.set(true);
                        break;
                    } else {
                        buffer.append(new String(chunk, 0, read, StandardCharsets.UTF_8));
                    }
                }
            }
        } catch (IOException ignored) {
            // Stream closed when process finished
        }
    }

    private void cleanupSession(ActiveExecutionSession session) {
        try {
            if (session.getStdinStream() != null) {
                session.getStdinStream().close();
            }
        } catch (Exception ignored) {}

        killContainer(session.getContainerName());

        if (session.getTempDir() != null) {
            deleteDirectoryRecursively(session.getTempDir());
        }
    }

    private String getSourceFilename(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA -> "Main.java";
            case PYTHON -> "main.py";
            case C -> "main.c";
            case CPP -> "main.cpp";
        };
    }

    private String getImageName(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA -> properties.getJavaImage();
            case PYTHON -> properties.getPythonImage();
            case C -> properties.getCImage();
            case CPP -> properties.getCppImage();
        };
    }

    private String getContainerScript(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA -> "javac -encoding UTF-8 Main.java && java -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xms32m -Xmx256m Main";
            case PYTHON -> "PYTHONUNBUFFERED=1 python3 -u main.py";
            case C -> "gcc -O2 main.c -o main && ./main";
            case CPP -> "g++ -O2 main.cpp -o main && ./main";
        };
    }

    private ExecutionStatus classifyErrorStatus(ProgrammingLanguage language, String stderr, int exitCode) {
        if (language == ProgrammingLanguage.PYTHON) {
            return ExecutionStatus.RUNTIME_ERROR;
        }

        String lowerErr = stderr.toLowerCase();
        if (lowerErr.contains("error:") || lowerErr.contains("syntax error") || lowerErr.contains("undefined reference")
                || lowerErr.contains("javac") || lowerErr.contains("fatal error")) {
            if (language == ProgrammingLanguage.JAVA && lowerErr.contains("exception in thread")) {
                return ExecutionStatus.RUNTIME_ERROR;
            }
            return ExecutionStatus.COMPILATION_ERROR;
        }

        return ExecutionStatus.RUNTIME_ERROR;
    }

    private void killContainer(String containerName) {
        try {
            new ProcessBuilder("docker", "rm", "-f", containerName)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor(3, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
    }

    private void deleteDirectoryRecursively(Path path) {
        try {
            if (Files.exists(path)) {
                try (var stream = Files.walk(path)) {
                    stream.sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException ignored) {}
                            });
                }
            }
        } catch (Exception e) {
            log.debug("Failed to delete temp dir {}: {}", path, e.getMessage());
        }
    }

    @PreDestroy
    public void cleanupAll() {
        log.info("Shutting down worker. Cleaning up {} active executions...", activeExecutions.size());
        for (ActiveExecutionSession session : activeExecutions.values()) {
            try {
                killContainer(session.getContainerName());
                if (session.getProcess().isAlive()) {
                    session.getProcess().destroyForcibly();
                }
                if (session.getTempDir() != null) {
                    deleteDirectoryRecursively(session.getTempDir());
                }
            } catch (Exception ignored) {}
        }
        watchdogExecutor.shutdownNow();
        streamExecutor.shutdownNow();
    }
}
