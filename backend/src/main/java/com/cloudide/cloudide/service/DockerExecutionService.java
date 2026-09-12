package com.cloudide.cloudide.service;

import com.cloudide.cloudide.config.ExecutionProperties;
import com.cloudide.cloudide.dto.ExecutionHealthResponse;
import com.cloudide.cloudide.dto.ExecutionRequest;
import com.cloudide.cloudide.dto.ExecutionResponse;
import com.cloudide.cloudide.enums.ExecutionStatus;
import com.cloudide.cloudide.enums.ProgrammingLanguage;
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
public class DockerExecutionService {

    private static final Logger log = LoggerFactory.getLogger(DockerExecutionService.class);

    private final ExecutionProperties executionProperties;
    private final ConcurrentHashMap<String, ActiveExecution> activeExecutions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ExecutionResponse> completedExecutions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService watchdogExecutor = Executors.newScheduledThreadPool(4);
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    public DockerExecutionService(ExecutionProperties executionProperties) {
        this.executionProperties = executionProperties;
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
     * Returns health status of Docker Engine and execution sandbox images.
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
            images.put(executionProperties.getJavaImage(), checkImageExists(executionProperties.getJavaImage()));
            images.put(executionProperties.getPythonImage(), checkImageExists(executionProperties.getPythonImage()));
            images.put(executionProperties.getCImage(), checkImageExists(executionProperties.getCImage()));
            images.put(executionProperties.getCppImage(), checkImageExists(executionProperties.getCppImage()));
        }

        return ExecutionHealthResponse.builder()
                .status(available ? "UP" : "DOWN")
                .docker(available ? "CONNECTED" : "DISCONNECTED")
                .dockerAvailable(available)
                .dockerVersion(version)
                .message(available ? "Docker Execution Engine is operational" : "Docker daemon unreachable")
                .imagesStatus(images)
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
     * Starts asynchronous interactive code execution session inside an isolated ephemeral Docker container.
     */
    public ExecutionResponse startExecution(ExecutionRequest request, String userEmail) {
        if (!isDockerAvailable()) {
            return ExecutionResponse.builder()
                    .executionId(UUID.randomUUID().toString())
                    .status(ExecutionStatus.SYSTEM_ERROR)
                    .stdout("")
                    .stderr("Docker engine is unavailable. Please verify Docker Desktop is running.")
                    .exitCode(-1)
                    .executionTimeMs(0L)
                    .errorDetails("Docker daemon unreachable")
                    .build();
        }

        String executionId = UUID.randomUUID().toString();
        ProgrammingLanguage language = request.getLanguage();
        String code = request.getCode();
        String initialStdin = request.getStdin() != null ? request.getStdin() : (request.getInput() != null ? request.getInput() : "");

        String filename = getSourceFilename(language);
        String image = getImageName(language);
        String containerName = "cloudide-sandbox-" + executionId.substring(0, 8);

        Path tempDir = null;
        Process process = null;

        try {
            // 1. Create temporary host directory
            tempDir = Files.createTempDirectory("cloudide-exec-" + executionId.substring(0, 8));
            Path sourceFile = tempDir.resolve(filename);
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

            // 2. Build execution command script based on language
            String containerScript = getContainerScript(language);

            // 3. Assemble secure Docker command line with interactive stdin (-i)
            List<String> command = new ArrayList<>();
            command.add("docker");
            command.add("run");
            command.add("--rm");
            command.add("-i");
            command.add("--name");
            command.add(containerName);

            if (executionProperties.isNetworkDisabled()) {
                command.add("--network");
                command.add("none");
            }

            command.add("--memory");
            command.add(executionProperties.getMemoryLimit());

            command.add("--cpus");
            command.add(executionProperties.getCpuLimit());

            command.add("--pids-limit");
            command.add(String.valueOf(executionProperties.getPidsLimit()));

            // Volume mount temp directory as /workspace
            String hostPath = tempDir.toAbsolutePath().toString().replace('\\', '/');
            command.add("-v");
            command.add(hostPath + ":/workspace:rw");

            command.add("-w");
            command.add("/workspace");

            command.add(image);
            command.add("sh");
            command.add("-c");
            command.add(containerScript);

            log.info("Starting interactive container {} (id: {}) for {} execution...", containerName, executionId, language);

            ProcessBuilder pb = new ProcessBuilder(command);
            process = pb.start();

            OutputStream stdinStream = process.getOutputStream();

            // 4. Pipe initial stdin if provided upfront
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
                    : executionProperties.getTimeoutSeconds();

            ActiveExecution active = new ActiveExecution(
                    executionId,
                    userEmail,
                    request.getProjectId(),
                    language,
                    containerName,
                    tempDir,
                    process,
                    stdinStream,
                    timeoutSeconds,
                    executionProperties.getMaxOutputSize()
            );

            activeExecutions.put(executionId, active);

            // Start asynchronous stream capture
            streamExecutor.submit(() -> captureStream(active.getProcess().getInputStream(), active.getStdoutBuffer(), active.getOutputLimitExceeded(), active.getMaxOutputSize()));
            streamExecutor.submit(() -> captureStream(active.getProcess().getErrorStream(), active.getStderrBuffer(), active.getOutputLimitExceeded(), active.getMaxOutputSize()));

            // Schedule timeout watchdog
            ScheduledFuture<?> watchdog = watchdogExecutor.schedule(() -> handleTimeout(active), timeoutSeconds, TimeUnit.SECONDS);
            active.setWatchdogFuture(watchdog);

            // Start completion monitor
            streamExecutor.submit(() -> monitorProcessCompletion(active));

            // Wait a brief 40ms to see if quick compilation error or fast result occurred
            try {
                active.getCompletionFuture().get(40, TimeUnit.MILLISECONDS);
                ExecutionResponse completed = completedExecutions.get(executionId);
                if (completed != null) {
                    return completed;
                }
            } catch (TimeoutException ignored) {
                // Still running (interactive or compiling)
            } catch (Exception e) {
                log.debug("Initial sync wait interrupted: {}", e.getMessage());
            }

            return ExecutionResponse.builder()
                    .executionId(executionId)
                    .status(ExecutionStatus.RUNNING)
                    .stdout(active.getStdout())
                    .stderr(active.getStderr())
                    .exitCode(null)
                    .executionTimeMs(0L)
                    .build();

        } catch (Exception e) {
            log.error("Internal execution launch error: {}", e.getMessage(), e);
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
     * Submits stdin input to an actively running execution.
     */
    public boolean sendInput(String executionId, String input, String userEmail) {
        ActiveExecution active = activeExecutions.get(executionId);
        if (active == null || !active.isAlive()) {
            return false;
        }

        try {
            String toSend = input != null ? input : "";
            if (!toSend.endsWith("\n")) {
                toSend += "\n";
            }
            OutputStream os = active.getStdinStream();
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
    public ExecutionResponse getExecutionStatus(String executionId, String userEmail) {
        ActiveExecution active = activeExecutions.get(executionId);
        if (active != null) {
            long elapsed = System.currentTimeMillis() - active.getStartTime();
            return ExecutionResponse.builder()
                    .executionId(executionId)
                    .status(active.isAlive() ? ExecutionStatus.RUNNING : active.getStatus())
                    .stdout(active.getStdout())
                    .stderr(active.getStderr())
                    .exitCode(active.getExitCode())
                    .executionTimeMs(elapsed)
                    .build();
        }

        ExecutionResponse completed = completedExecutions.get(executionId);
        if (completed != null) {
            return completed;
        }

        return null;
    }

    /**
     * Stops an active execution session immediately.
     */
    public ExecutionResponse stopExecution(String executionId, String userEmail) {
        ActiveExecution active = activeExecutions.get(executionId);
        if (active != null) {
            active.setStatus(ExecutionStatus.STOPPED);
            if (active.getWatchdogFuture() != null) {
                active.getWatchdogFuture().cancel(true);
            }
            killContainer(active.getContainerName());
            if (active.getProcess().isAlive()) {
                active.getProcess().destroyForcibly();
            }
            long elapsed = System.currentTimeMillis() - active.getStartTime();
            ExecutionResponse response = ExecutionResponse.builder()
                    .executionId(executionId)
                    .status(ExecutionStatus.STOPPED)
                    .stdout(active.getStdout())
                    .stderr(active.getStderr() + "\nExecution stopped by user.")
                    .exitCode(130)
                    .executionTimeMs(elapsed)
                    .build();

            activeExecutions.remove(executionId);
            completedExecutions.put(executionId, response);
            active.getCompletionFuture().complete(response);
            cleanupActive(active);
            return response;
        }

        return completedExecutions.get(executionId);
    }

    /**
     * Executes synchronously for compatibility with standard callers and tests.
     */
    public ExecutionResponse execute(ExecutionRequest request) {
        ExecutionResponse response = startExecution(request, "system");
        if (response.getStatus() != ExecutionStatus.RUNNING) {
            return response;
        }

        ActiveExecution active = activeExecutions.get(response.getExecutionId());
        if (active == null) {
            return completedExecutions.getOrDefault(response.getExecutionId(), response);
        }

        try {
            int timeout = request.getTimeoutSeconds() != null && request.getTimeoutSeconds() > 0
                    ? request.getTimeoutSeconds() + 2
                    : executionProperties.getTimeoutSeconds() + 2;
            return active.getCompletionFuture().get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Synchronous execution wait note for {}: {}", response.getExecutionId(), e.getMessage());
            return getExecutionStatus(response.getExecutionId(), "system");
        }
    }

    private void monitorProcessCompletion(ActiveExecution active) {
        try {
            int exitCode = active.getProcess().waitFor();
            // Allow up to 150ms for final stream chunks to flush into StringBuilder
            Thread.sleep(80);

            long executionTimeMs = System.currentTimeMillis() - active.getStartTime();

            if (active.getWatchdogFuture() != null) {
                active.getWatchdogFuture().cancel(true);
            }

            if (active.getStatus() == ExecutionStatus.TIMEOUT || active.getStatus() == ExecutionStatus.STOPPED) {
                return;
            }

            String stdout = active.getStdout();
            String stderr = active.getStderr();

            ExecutionStatus status;
            if (active.getOutputLimitExceeded().get()) {
                status = ExecutionStatus.OUTPUT_LIMIT_EXCEEDED;
            } else if (exitCode == 0) {
                status = ExecutionStatus.SUCCESS;
            } else {
                status = classifyErrorStatus(active.getLanguage(), stderr, exitCode);
            }

            ExecutionResponse result = ExecutionResponse.builder()
                    .executionId(active.getExecutionId())
                    .status(status)
                    .stdout(stdout)
                    .stderr(stderr)
                    .exitCode(exitCode)
                    .executionTimeMs(executionTimeMs)
                    .build();

            active.setStatus(status);
            active.setExitCode(exitCode);
            active.setExecutionTimeMs(executionTimeMs);

            activeExecutions.remove(active.getExecutionId());
            completedExecutions.put(active.getExecutionId(), result);
            active.getCompletionFuture().complete(result);

            log.info("Execution {} completed for {}: status={}, exitCode={}, time={}ms",
                    active.getExecutionId(), active.getLanguage(), status, exitCode, executionTimeMs);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            cleanupActive(active);
        }
    }

    private void handleTimeout(ActiveExecution active) {
        if (!active.isAlive() || active.getStatus() == ExecutionStatus.STOPPED) {
            return;
        }

        log.warn("Container {} (id: {}) timed out after {}s. Terminating container...",
                active.getContainerName(), active.getExecutionId(), active.getTimeoutSeconds());

        active.setStatus(ExecutionStatus.TIMEOUT);
        active.setExitCode(124);

        killContainer(active.getContainerName());
        if (active.getProcess().isAlive()) {
            active.getProcess().destroyForcibly();
        }

        long executionTimeMs = System.currentTimeMillis() - active.getStartTime();
        String stdout = active.getStdout();
        String stderr = active.getStderr();
        if (stderr.isEmpty()) {
            stderr = "Execution timed out after " + active.getTimeoutSeconds() + " seconds.";
        } else {
            stderr += "\nExecution timed out after " + active.getTimeoutSeconds() + " seconds.";
        }

        ExecutionResponse result = ExecutionResponse.builder()
                .executionId(active.getExecutionId())
                .status(ExecutionStatus.TIMEOUT)
                .stdout(stdout)
                .stderr(stderr)
                .exitCode(124)
                .executionTimeMs(executionTimeMs)
                .build();

        activeExecutions.remove(active.getExecutionId());
        completedExecutions.put(active.getExecutionId(), result);
        active.getCompletionFuture().complete(result);

        cleanupActive(active);
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

    private void cleanupActive(ActiveExecution active) {
        try {
            if (active.getStdinStream() != null) {
                active.getStdinStream().close();
            }
        } catch (Exception ignored) {}

        killContainer(active.getContainerName());

        if (active.getTempDir() != null) {
            deleteDirectoryRecursively(active.getTempDir());
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
            case JAVA -> executionProperties.getJavaImage();
            case PYTHON -> executionProperties.getPythonImage();
            case C -> executionProperties.getCImage();
            case CPP -> executionProperties.getCppImage();
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

    /**
     * Inner state representation of an active execution session.
     */
    private static class ActiveExecution {
        private final String executionId;
        private final String userEmail;
        private final Long projectId;
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

        public ActiveExecution(String executionId, String userEmail, Long projectId,
                               ProgrammingLanguage language, String containerName, Path tempDir,
                               Process process, OutputStream stdinStream, int timeoutSeconds, int maxOutputSize) {
            this.executionId = executionId;
            this.userEmail = userEmail;
            this.projectId = projectId;
            this.language = language;
            this.containerName = containerName;
            this.tempDir = tempDir;
            this.process = process;
            this.stdinStream = stdinStream;
            this.timeoutSeconds = timeoutSeconds;
            this.maxOutputSize = maxOutputSize;
        }

        public boolean isAlive() {
            return process.isAlive();
        }

        public String getExecutionId() { return executionId; }
        public String getUserEmail() { return userEmail; }
        public Long getProjectId() { return projectId; }
        public ProgrammingLanguage getLanguage() { return language; }
        public String getContainerName() { return containerName; }
        public Path getTempDir() { return tempDir; }
        public Process getProcess() { return process; }
        public OutputStream getStdinStream() { return stdinStream; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public int getMaxOutputSize() { return maxOutputSize; }
        public StringBuilder getStdoutBuffer() { return stdoutBuffer; }
        public StringBuilder getStderrBuffer() { return stderrBuffer; }
        public AtomicBoolean getOutputLimitExceeded() { return outputLimitExceeded; }
        public long getStartTime() { return startTime; }
        public CompletableFuture<ExecutionResponse> getCompletionFuture() { return completionFuture; }

        public ExecutionStatus getStatus() { return status; }
        public void setStatus(ExecutionStatus status) { this.status = status; }

        public Integer getExitCode() { return exitCode; }
        public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }

        public Long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

        public ScheduledFuture<?> getWatchdogFuture() { return watchdogFuture; }
        public void setWatchdogFuture(ScheduledFuture<?> watchdogFuture) { this.watchdogFuture = watchdogFuture; }

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
}
