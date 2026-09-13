package com.cloudide.execution.controller;

import com.cloudide.execution.dto.ExecutionHealthResponse;
import com.cloudide.execution.dto.ExecutionInputRequest;
import com.cloudide.execution.dto.ExecutionStartRequest;
import com.cloudide.execution.dto.ExecutionResponse;
import com.cloudide.execution.service.DockerSandboxService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal")
public class InternalExecutionController {

    private final DockerSandboxService sandboxService;

    public InternalExecutionController(DockerSandboxService sandboxService) {
        this.sandboxService = sandboxService;
    }

    @PostMapping("/executions")
    public ResponseEntity<ExecutionResponse> startExecution(@Valid @RequestBody ExecutionStartRequest request) {
        ExecutionResponse response = sandboxService.startExecution(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/executions/{executionId}/input")
    public ResponseEntity<?> sendInput(
            @PathVariable String executionId,
            @RequestBody ExecutionInputRequest inputRequest
    ) {
        String input = inputRequest != null ? inputRequest.getInput() : "";
        boolean sent = sandboxService.sendInput(executionId, input);
        if (sent) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "executionId", executionId,
                    "message", "Input piped to container stdin"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Execution session is not active or container has already exited"
            ));
        }
    }

    @GetMapping("/executions/{executionId}")
    public ResponseEntity<ExecutionResponse> getExecutionStatus(@PathVariable String executionId) {
        ExecutionResponse response = sandboxService.getExecutionStatus(executionId);
        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/executions/{executionId}/stop")
    public ResponseEntity<ExecutionResponse> stopExecution(@PathVariable String executionId) {
        ExecutionResponse response = sandboxService.stopExecution(executionId);
        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ExecutionHealthResponse> getHealth() {
        ExecutionHealthResponse health = sandboxService.getHealth();
        return ResponseEntity.ok(health);
    }
}
