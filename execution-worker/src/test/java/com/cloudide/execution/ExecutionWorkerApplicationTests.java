package com.cloudide.execution;

import com.cloudide.execution.config.WorkerSecretInterceptor;
import com.cloudide.execution.dto.ExecutionInputRequest;
import com.cloudide.execution.dto.ExecutionStartRequest;
import com.cloudide.execution.enums.ProgrammingLanguage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExecutionWorkerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SECRET_HEADER = WorkerSecretInterceptor.SECRET_HEADER;
    private static final String VALID_SECRET = "test_worker_secret_12345";

    @Test
    @DisplayName("1. Request without secret header is rejected with 401 Unauthorized")
    void testRequestWithoutSecretRejected() throws Exception {
        mockMvc.perform(get("/internal/health"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("2. Request with invalid secret header is rejected with 401 Unauthorized")
    void testRequestWithInvalidSecretRejected() throws Exception {
        mockMvc.perform(get("/internal/health")
                        .header(SECRET_HEADER, "wrong-secret"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. Health endpoint with valid secret returns UP or DOWN and Docker status")
    void testHealthEndpointWithValidSecret() throws Exception {
        mockMvc.perform(get("/internal/health")
                        .header(SECRET_HEADER, VALID_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", notNullValue()))
                .andExpect(jsonPath("$.maxConcurrent", is(4)));
    }

    @Test
    @DisplayName("4. Start execution request with invalid body returns 400 Bad Request")
    void testInvalidExecutionRequestValidation() throws Exception {
        ExecutionStartRequest req = new ExecutionStartRequest(null, null, "", null, null);

        mockMvc.perform(post("/internal/executions")
                        .header(SECRET_HEADER, VALID_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("5. Non-existent execution status query returns 404 Not Found")
    void testNonExistentExecutionQuery() throws Exception {
        mockMvc.perform(get("/internal/executions/non-existent-uuid")
                        .header(SECRET_HEADER, VALID_SECRET))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("6. Non-existent execution input request returns 400 Bad Request")
    void testNonExistentExecutionInput() throws Exception {
        ExecutionInputRequest req = new ExecutionInputRequest("25\n");
        mockMvc.perform(post("/internal/executions/non-existent-uuid/input")
                        .header(SECRET_HEADER, VALID_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
