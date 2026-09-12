package com.cloudide.cloudide.service;

import com.cloudide.cloudide.dto.ExecutionHealthResponse;
import com.cloudide.cloudide.dto.ExecutionRequest;
import com.cloudide.cloudide.dto.ExecutionResponse;
import com.cloudide.cloudide.entity.User;
import com.cloudide.cloudide.exception.ForbiddenException;
import com.cloudide.cloudide.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);

    private final DockerExecutionService dockerExecutionService;
    private final UserService userService;
    private final ProjectRepository projectRepository;

    public ExecutionService(DockerExecutionService dockerExecutionService,
                            UserService userService,
                            ProjectRepository projectRepository) {
        this.dockerExecutionService = dockerExecutionService;
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

        log.info("Starting interactive {} execution for user {}", request.getLanguage(), currentUser.getEmail());
        return dockerExecutionService.startExecution(request, currentUser.getEmail());
    }

    public ExecutionResponse execute(ExecutionRequest request) {
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

        log.info("Executing {} code for user {}", request.getLanguage(), currentUser.getEmail());
        return dockerExecutionService.execute(request);
    }

    public boolean sendInput(String executionId, String input) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        log.info("User {} sending stdin input to execution {}", currentUser.getEmail(), executionId);
        return dockerExecutionService.sendInput(executionId, input, currentUser.getEmail());
    }

    public ExecutionResponse getExecutionStatus(String executionId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        return dockerExecutionService.getExecutionStatus(executionId, currentUser.getEmail());
    }

    public ExecutionResponse stopExecution(String executionId) {
        User currentUser = userService.getCurrentAuthenticatedUser();
        log.info("User {} requesting stop for execution {}", currentUser.getEmail(), executionId);
        return dockerExecutionService.stopExecution(executionId, currentUser.getEmail());
    }

    public ExecutionHealthResponse getHealth() {
        return dockerExecutionService.getHealth();
    }
}
