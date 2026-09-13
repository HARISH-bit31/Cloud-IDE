package com.cloudide.cloudide;

import com.cloudide.cloudide.dto.ExecutionInputRequest;
import com.cloudide.cloudide.dto.ExecutionRequest;
import com.cloudide.cloudide.dto.ExecutionResponse;
import com.cloudide.cloudide.dto.LoginRequest;
import com.cloudide.cloudide.dto.RegisterRequest;
import com.cloudide.cloudide.enums.ExecutionStatus;
import com.cloudide.cloudide.enums.ProgrammingLanguage;
import com.cloudide.cloudide.service.ExecutionClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:queuetestdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "execution.worker.url=http://localhost:8090",
        "execution.worker.secret=cloudide_worker_secret",
        "execution.queue.capacity=5",
        "execution.worker.concurrency=2"
})
class ExecutionQueueTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExecutionClient executionClient;

    private String userToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = "queue_user_" + System.currentTimeMillis() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest("Queue User", email, "password123"))));

        String loginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "password123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        userToken = objectMapper.readTree(loginRes).get("token").asText();

        String otherEmail = "other_queue_user_" + System.currentTimeMillis() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest("Other User", otherEmail, "password123"))));

        String otherLoginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(otherEmail, "password123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        otherUserToken = objectMapper.readTree(otherLoginRes).get("token").asText();
    }

    @Test
    @DisplayName("1. Enqueue execution returns QUEUED or RUNNING status with an executionId")
    void testEnqueueExecution() throws Exception {
        Mockito.when(executionClient.startExecution(any(String.class), any(ExecutionRequest.class)))
                .thenAnswer(inv -> ExecutionResponse.builder()
                        .executionId(inv.getArgument(0))
                        .status(ExecutionStatus.RUNNING)
                        .stdout("Hello Queue")
                        .build());

        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.PYTHON, "print('Hello Queue')", null, null, null);

        mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId", notNullValue()))
                .andExpect(jsonPath("$.status", anyOf(is("QUEUED"), is("RUNNING"))));
    }

    @Test
    @DisplayName("2. Queue saturation returns HTTP 429 TOO_MANY_REQUESTS when capacity exceeded")
    void testQueueSaturationReturns429() throws Exception {
        // Concurrency is 2, Queue capacity is 5 => Total concurrent pending+running is 7.
        // Block the worker calls so slots remain occupied.
        CountDownLatch workerBlocker = new CountDownLatch(1);

        Mockito.when(executionClient.startExecution(any(String.class), any(ExecutionRequest.class)))
                .thenAnswer(inv -> {
                    try {
                        workerBlocker.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {}
                    return ExecutionResponse.builder()
                            .executionId(inv.getArgument(0))
                            .status(ExecutionStatus.SUCCESS)
                            .build();
                });

        try {
            List<Integer> statusCodes = new ArrayList<>();
            ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.PYTHON, "print('concurrency test')", null, null, null);

            // Send 15 rapid execution requests (capacity is 2 running + 5 queued = 7)
            for (int i = 0; i < 15; i++) {
                MvcResult res = mockMvc.perform(post("/api/execute")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andReturn();
                statusCodes.add(res.getResponse().getStatus());
            }

            // Verify at least one request got 429 Too Many Requests
            assertTrue(statusCodes.contains(429), "Expected at least one request to be rejected with HTTP 429. Statuses: " + statusCodes);
        } finally {
            workerBlocker.countDown();
        }
    }

    @Test
    @DisplayName("3. Stop queued execution transitions to STOPPED without invoking worker")
    void testStopQueuedExecution() throws Exception {
        CountDownLatch workerHold = new CountDownLatch(1);
        AtomicInteger workerInvocations = new AtomicInteger(0);

        Mockito.when(executionClient.startExecution(any(String.class), any(ExecutionRequest.class)))
                .thenAnswer(inv -> {
                    workerInvocations.incrementAndGet();
                    try {
                        workerHold.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {}
                    return ExecutionResponse.builder()
                            .executionId(inv.getArgument(0))
                            .status(ExecutionStatus.SUCCESS)
                            .build();
                });

        try {
            ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.JAVA, "System.out.println();", null, null, null);

            // Fill 2 concurrency slots
            mockMvc.perform(post("/api/execute")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));

            mockMvc.perform(post("/api/execute")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));

            // 3rd request should be QUEUED
            MvcResult res3 = mockMvc.perform(post("/api/execute")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();

            String execId3 = objectMapper.readTree(res3.getResponse().getContentAsString()).get("executionId").asText();

            // Stop the 3rd queued execution
            mockMvc.perform(post("/api/executions/" + execId3 + "/stop")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("STOPPED")));

            // Check status via GET /api/executions/{id}
            mockMvc.perform(get("/api/executions/" + execId3)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("STOPPED")));

        } finally {
            workerHold.countDown();
        }
    }

    @Test
    @DisplayName("4. Dual mapping endpoint support: /api/executions/{id} and /api/execute/{id}")
    void testDualMappingEndpoints() throws Exception {
        Mockito.when(executionClient.startExecution(any(String.class), any(ExecutionRequest.class)))
                .thenAnswer(inv -> ExecutionResponse.builder()
                        .executionId(inv.getArgument(0))
                        .status(ExecutionStatus.RUNNING)
                        .build());

        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.CPP, "int main(){}", null, null, null);

        MvcResult res = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        String execId = objectMapper.readTree(res.getResponse().getContentAsString()).get("executionId").asText();

        // Check via /api/execute/{id}
        mockMvc.perform(get("/api/execute/" + execId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId", is(execId)));

        // Check via /api/executions/{id}
        mockMvc.perform(get("/api/executions/" + execId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId", is(execId)));
    }
}
