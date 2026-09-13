package com.cloudide.cloudide.controller;

import com.cloudide.cloudide.dto.ExecutionHealthResponse;
import com.cloudide.cloudide.dto.ExecutionInputRequest;
import com.cloudide.cloudide.dto.ExecutionRequest;
import com.cloudide.cloudide.dto.ExecutionResponse;
import com.cloudide.cloudide.service.ExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/execute")
    public ResponseEntity<ExecutionResponse> execute(@Valid @RequestBody ExecutionRequest request) {
        ExecutionResponse response = executionService.startExecution(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/execute/{executionId}/input", "/executions/{executionId}/input"})
    public ResponseEntity<?> sendInput(
            @PathVariable String executionId,
            @RequestBody ExecutionInputRequest inputRequest
    ) {
        String input = inputRequest != null ? inputRequest.getInput() : "";
        boolean sent = executionService.sendInput(executionId, input);
        if (sent) {
            return ResponseEntity.ok(Map.of("success", true, "executionId", executionId, "message", "Input sent to running process"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Execution session is not active or container has finished"));
        }
    }

    @GetMapping({"/execute/{executionId}", "/executions/{executionId}"})
    public ResponseEntity<ExecutionResponse> getExecutionStatus(@PathVariable String executionId) {
        ExecutionResponse response = executionService.getExecutionStatus(executionId);
        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping({"/execute/{executionId}/stop", "/executions/{executionId}/stop"})
    public ResponseEntity<ExecutionResponse> stopExecution(@PathVariable String executionId) {
        ExecutionResponse response = executionService.stopExecution(executionId);
        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/execution/health")
    public ResponseEntity<ExecutionHealthResponse> getHealth() {
        ExecutionHealthResponse health = executionService.getHealth();
        return ResponseEntity.ok(health);
    }
}

