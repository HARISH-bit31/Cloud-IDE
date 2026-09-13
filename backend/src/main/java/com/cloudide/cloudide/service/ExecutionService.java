package com.cloudide.cloudide.service;

import com.cloudide.cloudide.dto.ExecutionHealthResponse;
import com.cloudide.cloudide.dto.ExecutionRequest;
import com.cloudide.cloudide.dto.ExecutionResponse;
import com.cloudide.cloudide.entity.User;
import com.cloudide.cloudide.enums.ExecutionStatus;
import com.cloudide.cloudide.exception.ForbiddenException;
import com.cloudide.cloudide.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);

    private final ExecutionClient executionClient;
    private final UserService userService;
    private final ProjectRepository projectRepository;
    private final com.cloudide.cloudide.metrics.ExecutionMetrics executionMetrics;

    // Track execution ownership: executionId -> userId
    private final ConcurrentHashMap<String, Long> executionOwnership = new ConcurrentHashMap<>();

    public ExecutionService(ExecutionClient executionClient,
                            UserService userService,
                            ProjectRepository projectRepository,
                            com.cloudide.cloudide.metrics.ExecutionMetrics executionMetrics) {
        this.executionClient = executionClient;
        this.userService = userService;
        this.projectRepository = projectRepository;
        this.executionMetrics = executionMetrics;
    }

    public ExecutionResponse startExecution(ExecutionRequest request) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        if (request.getProjectId() != null) {
            projectRepository.findById(request.getProjectId()).ifPresent(project -> {
                if (!Objects.equals(project.getUser().getId(), currentUser.getId())) {
                    log.warn("User {} attempted to execute code under project {} owned by {}",
                            currentUser.getEmail(), project.getId(), project.getUser().getEmail());
                    throw new ForbiddenException("You do not have permission to execute code in this project");
                }
            });
        }

        String executionId = UUID.randomUUID().toString();
        executionOwnership.put(executionId, currentUser.getId());

        String lang = request.getLanguage() != null ? request.getLanguage().name() : "UNKNOWN";
        executionMetrics.incrementExecutionStarted(lang);

        log.info("Starting interactive {} execution (id: {}) for user {}", lang, executionId, currentUser.getEmail());
        try {
            ExecutionResponse response = executionClient.startExecution(executionId, request);
            if (response.getStatus() != null && response.getStatus() != ExecutionStatus.RUNNING && response.getStatus() != ExecutionStatus.QUEUED) {
                executionMetrics.recordExecutionCompleted(lang, response.getStatus().name(), response.getExecutionTimeMs() != null ? response.getExecutionTimeMs() : 0);
            }
            return response;
        } catch (Exception e) {
            executionMetrics.recordExecutionFailure(lang, "START_FAILED");
            throw e;
        }
    }

    public ExecutionResponse execute(ExecutionRequest request) {
        String lang = request.getLanguage() != null ? request.getLanguage().name() : "UNKNOWN";
        long start = System.currentTimeMillis();
        ExecutionResponse response = startExecution(request);
        if (response.getStatus() != ExecutionStatus.RUNNING) {
            return response;
        }

        String executionId = response.getExecutionId();
        int timeoutSec = request.getTimeoutSeconds() != null && request.getTimeoutSeconds() > 0 ? request.getTimeoutSeconds() + 2 : 12;
        long deadline = System.currentTimeMillis() + (timeoutSec * 1000L);

        while (System.currentTimeMillis() < deadline) {
            ExecutionResponse current = getExecutionStatus(executionId);
            if (current != null && current.getStatus() != ExecutionStatus.RUNNING && current.getStatus() != ExecutionStatus.WAITING_FOR_INPUT && current.getStatus() != ExecutionStatus.QUEUED) {
                long duration = System.currentTimeMillis() - start;
                executionMetrics.recordExecutionCompleted(lang, current.getStatus().name(), current.getExecutionTimeMs() != null ? current.getExecutionTimeMs() : duration);
                return current;
            }
            try {
                Thread.sleep(80);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        ExecutionResponse finalStatus = getExecutionStatus(executionId);
        if (finalStatus != null && finalStatus.getStatus() != null) {
            long duration = System.currentTimeMillis() - start;
            executionMetrics.recordExecutionCompleted(lang, finalStatus.getStatus().name(), finalStatus.getExecutionTimeMs() != null ? finalStatus.getExecutionTimeMs() : duration);
        }
        return finalStatus;
    }

    public boolean sendInput(String executionId, String input) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        verifyExecutionOwnership(executionId, currentUser.getId());

        log.info("User {} sending stdin input to execution {}", currentUser.getEmail(), executionId);
        return executionClient.sendInput(executionId, input);
    }

    public ExecutionResponse getExecutionStatus(String executionId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        verifyExecutionOwnership(executionId, currentUser.getId());

        return executionClient.getExecutionStatus(executionId);
    }

    public ExecutionResponse stopExecution(String executionId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        verifyExecutionOwnership(executionId, currentUser.getId());

        log.info("User {} requesting stop for execution {}", currentUser.getEmail(), executionId);
        ExecutionResponse response = executionClient.stopExecution(executionId);
        if (response != null && response.getStatus() != null) {
            executionMetrics.recordExecutionCompleted("UNKNOWN", response.getStatus().name(), 0);
        }
        return response;
    }

    public ExecutionHealthResponse getHealth() {
        return executionClient.getHealth();
    }

    private void verifyExecutionOwnership(String executionId, Long userId) {
        Long ownerId = executionOwnership.get(executionId);
        if (ownerId != null && !ownerId.equals(userId)) {
            log.warn("Security violation: User {} attempted to access execution {} owned by User {}", userId, executionId, ownerId);
            throw new ForbiddenException("You do not have permission to access this execution session");
        }
    }
}
