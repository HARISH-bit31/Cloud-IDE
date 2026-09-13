package com.cloudide.cloudide.service;

import com.cloudide.cloudide.config.ExecutionProperties;
import com.cloudide.cloudide.dto.ExecutionHealthResponse;
import com.cloudide.cloudide.dto.ExecutionRequest;
import com.cloudide.cloudide.dto.ExecutionResponse;
import com.cloudide.cloudide.enums.ExecutionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExecutionWorkerClient implements ExecutionClient {

    private static final Logger log = LoggerFactory.getLogger(ExecutionWorkerClient.class);
    private static final String SECRET_HEADER = "X-Execution-Worker-Secret";

    private final ExecutionProperties executionProperties;
    private final RestClient restClient;

    public ExecutionWorkerClient(ExecutionProperties executionProperties) {
        this.executionProperties = executionProperties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private String getWorkerBaseUrl() {
        String url = executionProperties.getWorkerUrl();
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    @Override
    public ExecutionResponse startExecution(String executionId, ExecutionRequest request) {
        String url = getWorkerBaseUrl() + "/internal/executions";

        Map<String, Object> body = new HashMap<>();
        body.put("executionId", executionId);
        body.put("language", request.getLanguage());
        body.put("code", request.getCode());
        body.put("stdin", request.getStdin());
        body.put("timeoutSeconds", request.getTimeoutSeconds());

        try {
            return restClient.post()
                    .uri(url)
                    .header(SECRET_HEADER, executionProperties.getWorkerSecret())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("Worker returned error status: {}", res.getStatusCode());
                    })
                    .body(ExecutionResponse.class);
        } catch (Exception e) {
            log.error("Failed to communicate with Execution Worker at {}: {}", url, e.getMessage());
            return ExecutionResponse.builder()
                    .executionId(executionId)
                    .status(ExecutionStatus.SYSTEM_ERROR)
                    .stdout("")
                    .stderr("Code execution service is temporarily unavailable.")
                    .exitCode(-1)
                    .executionTimeMs(0L)
                    .errorDetails("Worker communication failure")
                    .build();
        }
    }

    @Override
    public boolean sendInput(String executionId, String input) {
        String url = getWorkerBaseUrl() + "/internal/executions/" + executionId + "/input";

        Map<String, Object> body = new HashMap<>();
        body.put("input", input);

        try {
            var response = restClient.post()
                    .uri(url)
                    .header(SECRET_HEADER, executionProperties.getWorkerSecret())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Failed to send input to execution worker for session {}: {}", executionId, e.getMessage());
            return false;
        }
    }

    @Override
    public ExecutionResponse getExecutionStatus(String executionId) {
        String url = getWorkerBaseUrl() + "/internal/executions/" + executionId;

        try {
            return restClient.get()
                    .uri(url)
                    .header(SECRET_HEADER, executionProperties.getWorkerSecret())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(ExecutionResponse.class);
        } catch (Exception e) {
            log.debug("Worker status query note for {}: {}", executionId, e.getMessage());
            return null;
        }
    }

    @Override
    public ExecutionResponse stopExecution(String executionId) {
        String url = getWorkerBaseUrl() + "/internal/executions/" + executionId + "/stop";

        try {
            return restClient.post()
                    .uri(url)
                    .header(SECRET_HEADER, executionProperties.getWorkerSecret())
                    .retrieve()
                    .body(ExecutionResponse.class);
        } catch (Exception e) {
            log.warn("Failed to stop execution {} on worker: {}", executionId, e.getMessage());
            return null;
        }
    }

    @Override
    public ExecutionHealthResponse getHealth() {
        String url = getWorkerBaseUrl() + "/internal/health";

        try {
            ExecutionHealthResponse workerHealth = restClient.get()
                    .uri(url)
                    .header(SECRET_HEADER, executionProperties.getWorkerSecret())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(ExecutionHealthResponse.class);

            if (workerHealth != null) {
                workerHealth.setWorkerAvailable(true);
                return workerHealth;
            }
        } catch (Exception e) {
            log.debug("Execution Worker health check failed: {}", e.getMessage());
        }

        return ExecutionHealthResponse.builder()
                .status("DOWN")
                .docker("DISCONNECTED")
                .workerAvailable(false)
                .dockerAvailable(false)
                .dockerVersion("Unknown")
                .message("Execution Worker unreachable")
                .timestamp(System.currentTimeMillis())
                .imagesStatus(Map.of())
                .build();
    }
}
