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
                if (!Objects.equals(project.getUser().getId(), currentUser.getId())) {
                    log.warn("User {} attempted to execute code under project {} owned by {}",
                            currentUser.getEmail(), project.getId(), project.getUser().getEmail());
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
        verifyExecutionOwnership(executionId, currentUser.getId());

        log.info("User {} sending stdin input to execution {}", currentUser.getEmail(), executionId);
        boolean sent = executionDispatcher.sendInput(executionId, input);
        if (sent) {
            return true;
        }
        try {
            return executionClient.sendInput(executionId, input);
        } catch (Exception e) {
            log.warn("Failed to forward stdin to worker for execution {}: {}", executionId, e.getMessage());
            return false;
        }
    }

    public ExecutionResponse getExecutionStatus(String executionId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        verifyExecutionOwnership(executionId, currentUser.getId());

        ExecutionRecord record = executionDispatcher.getRecord(executionId);
        if (record != null) {
            return record.toResponse();
        }

        try {
            ExecutionResponse workerRes = executionClient.getExecutionStatus(executionId);
            if (workerRes != null && workerRes.getStatus() != null) {
                return workerRes;
            }
        } catch (Exception e) {
            log.debug("Worker status query for {} produced: {}", executionId, e.getMessage());
        }

        return null;
    }

    public ExecutionResponse stopExecution(String executionId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        verifyExecutionOwnership(executionId, currentUser.getId());

        log.info("User {} requesting stop for execution {}", currentUser.getEmail(), executionId);
        ExecutionResponse response = executionDispatcher.stopExecution(executionId);
        if (response != null) {
            return response;
        }

        try {
            return executionClient.stopExecution(executionId);
        } catch (Exception e) {
            log.warn("Failed to forward stop to worker for execution {}: {}", executionId, e.getMessage());
            return null;
        }
    }

    public ExecutionHealthResponse getHealth() {
        return executionClient.getHealth();
    }

    private void verifyExecutionOwnership(String executionId, Long userId) {
        ExecutionRecord record = executionDispatcher.getRecord(executionId);
        if (record != null && !Objects.equals(record.getUserId(), userId)) {
            log.warn("Security violation: User {} attempted to access execution {} owned by User {}",
                    userId, executionId, record.getUserId());
            throw new ForbiddenException("You do not have permission to access this execution session");
        }
    }
}
