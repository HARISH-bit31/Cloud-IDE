package com.cloudide.cloudide.service;

import com.cloudide.cloudide.dto.ExecutionHealthResponse;
import com.cloudide.cloudide.dto.ExecutionRequest;
import com.cloudide.cloudide.dto.ExecutionResponse;
import com.cloudide.cloudide.entity.User;
import com.cloudide.cloudide.enums.ExecutionStatus;
import com.cloudide.cloudide.exception.ForbiddenException;
import com.cloudide.cloudide.model.ExecutionRecord;
import com.cloudide.cloudide.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);

    private final ExecutionDispatcher executionDispatcher;
    private final ExecutionClient executionClient;
    private final UserService userService;
    private final ProjectRepository projectRepository;

    public ExecutionService(ExecutionDispatcher executionDispatcher,
                            ExecutionClient executionClient,
                            UserService userService,
                            ProjectRepository projectRepository) {
        this.executionDispatcher = executionDispatcher;
        this.executionClient = executionClient;
        this.userService = userService;
        this.projectRepository = projectRepository;
    }

    public ExecutionResponse startExecution(ExecutionRequest request) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        if (request.getProjectId() != null) {
            projectRepository.findById(request.getProjectId()).ifPresent(project -> {
                if (currentUser == null || project.getUser() == null || !Objects.equals(project.getUser().getId(), currentUser.getId())) {
                    log.warn("User {} attempted to execute code under project {} with owner {}",
                            currentUser != null ? currentUser.getEmail() : "null",
                            project.getId(),
                            project.getUser() != null ? project.getUser().getEmail() : "null");
                    throw new ForbiddenException("You do not have permission to execute code in this project");
                }
            });
        }

        String executionId = UUID.randomUUID().toString();

        ExecutionRecord record = new ExecutionRecord(
                executionId,
                currentUser.getId(),
                currentUser.getEmail(),
                request.getProjectId(),
                request.getLanguage(),
                request.getCode(),
                request.getStdin(),
                request.getTimeoutSeconds()
        );

        log.info("Submitting {} execution request (id: {}) for user {}",
                request.getLanguage(), executionId, currentUser.getEmail());

        return executionDispatcher.submit(record);
    }

    public ExecutionResponse execute(ExecutionRequest request) {
        long start = System.currentTimeMillis();
        ExecutionResponse response = startExecution(request);
        String executionId = response.getExecutionId();

        int timeoutSec = request.getTimeoutSeconds() != null && request.getTimeoutSeconds() > 0 ? request.getTimeoutSeconds() + 4 : 14;
        long deadline = System.currentTimeMillis() + (timeoutSec * 1000L);

        while (System.currentTimeMillis() < deadline) {
            ExecutionResponse current = getExecutionStatus(executionId);
            if (current != null && current.getStatus() != null &&
                    current.getStatus() != ExecutionStatus.QUEUED &&
                    current.getStatus() != ExecutionStatus.RUNNING &&
                    current.getStatus() != ExecutionStatus.WAITING_FOR_INPUT) {
                return current;
            }
            try {
                Thread.sleep(60);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return getExecutionStatus(executionId);
    }

    public boolean sendInput(String executionId, String input) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required to send execution input");
        }

        ExecutionRecord record = getVerifiedRecord(executionId, currentUserId);
        if (record == null) {
            // Fail closed: missing local record cannot establish ownership. Never forward to worker.
            log.warn("User {} attempted to send stdin to unknown or unverified execution session {}",
                    currentUser.getEmail(), executionId);
            return false;
        }

        log.info("User {} sending stdin input to execution {}", currentUser.getEmail(), executionId);
        return executionDispatcher.sendInput(executionId, input);
    }

    public ExecutionResponse getExecutionStatus(String executionId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required to access execution status");
        }

        ExecutionRecord record = getVerifiedRecord(executionId, currentUserId);
        if (record == null) {
            // Fail closed: missing local record cannot establish ownership. Never fall back to worker.
            log.debug("Execution status lookup failed: no verified local record for {}", executionId);
            return null;
        }

        return record.toResponse();
    }

    public ExecutionResponse stopExecution(String executionId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        if (currentUserId == null) {
            throw new ForbiddenException("Authentication required to stop execution");
        }

        ExecutionRecord record = getVerifiedRecord(executionId, currentUserId);
        if (record == null) {
            // Fail closed: missing local record cannot establish ownership. Never forward stop to worker.
            log.warn("User {} attempted to stop unknown or unverified execution session {}",
                    currentUser.getEmail(), executionId);
            return null;
        }

        log.info("User {} requesting stop for execution {}", currentUser.getEmail(), executionId);
        return executionDispatcher.stopExecution(executionId);
    }

    public ExecutionHealthResponse getHealth() {
        try {
            ExecutionHealthResponse health = executionClient.getHealth();
            if (health != null) {
                return health;
            }
        } catch (Exception e) {
            log.warn("Error checking execution worker health: {}", e.getMessage());
        }

        return ExecutionHealthResponse.builder()
                .status("DOWN")
                .docker("DISCONNECTED")
                .workerAvailable(false)
                .dockerAvailable(false)
                .dockerVersion("Unknown")
                .message("Execution Worker unreachable")
                .timestamp(System.currentTimeMillis())
                .imagesStatus(java.util.Map.of())
                .build();
    }

    private ExecutionRecord getVerifiedRecord(String executionId, Long userId) {
        if (executionId == null || executionId.isBlank() || userId == null) {
            return null;
        }
        ExecutionRecord record = executionDispatcher.getRecord(executionId);
        if (record != null && !Objects.equals(record.getUserId(), userId)) {
            log.warn("Security violation: User {} attempted to access execution {} owned by User {}",
                    userId, executionId, record.getUserId());
            throw new ForbiddenException("You do not have permission to access this execution session");
        }
        return record;
    }
}
