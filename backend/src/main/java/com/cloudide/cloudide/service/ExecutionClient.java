package com.cloudide.cloudide.service;

import com.cloudide.cloudide.dto.ExecutionHealthResponse;
import com.cloudide.cloudide.dto.ExecutionRequest;
import com.cloudide.cloudide.dto.ExecutionResponse;

public interface ExecutionClient {

    ExecutionResponse startExecution(String executionId, ExecutionRequest request);

    boolean sendInput(String executionId, String input);

    ExecutionResponse getExecutionStatus(String executionId);

    ExecutionResponse stopExecution(String executionId);

    ExecutionHealthResponse getHealth();
}
