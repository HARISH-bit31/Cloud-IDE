package com.cloudide.cloudide;

import com.cloudide.cloudide.dto.*;
import com.cloudide.cloudide.enums.ExecutionStatus;
import com.cloudide.cloudide.enums.ProgrammingLanguage;
import com.cloudide.cloudide.service.ExecutionClient;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:executiontestdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "execution.worker.url=http://localhost:8090",
        "execution.worker.secret=cloudide_worker_secret"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ExecutionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExecutionClient executionClient;

    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() throws Exception {
        Mockito.reset(executionClient);

        // Create User 1
        String user1Email = "user1_" + System.currentTimeMillis() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest("User One", user1Email, "password123"))));

        String login1Res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(user1Email, "password123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        user1Token = objectMapper.readTree(login1Res).get("token").asText();

        // Create User 2
        String user2Email = "user2_" + System.currentTimeMillis() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest("User Two", user2Email, "password123"))));

        String login2Res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(user2Email, "password123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        user2Token = objectMapper.readTree(login2Res).get("token").asText();
    }

    @Test
    @DisplayName("1. Public endpoint /api/execution/health returns worker & Docker status")
    void testExecutionHealthEndpoint() throws Exception {
        ExecutionHealthResponse mockHealth = ExecutionHealthResponse.builder()
                .status("UP")
                .docker("CONNECTED")
                .workerAvailable(true)
                .dockerAvailable(true)
                .dockerVersion("27.0.3")
                .message("Execution Worker & Docker Engine operational")
                .imagesStatus(Map.of("cloud-ide-java:latest", true))
                .timestamp(System.currentTimeMillis())
                .build();

        Mockito.when(executionClient.getHealth()).thenReturn(mockHealth);

        mockMvc.perform(get("/api/execution/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerAvailable", is(true)))
                .andExpect(jsonPath("$.dockerAvailable", is(true)))
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("1b. Public endpoint /api/execution/health returns workerAvailable=false when worker is down")
    void testExecutionHealthEndpointWhenWorkerUnavailable() throws Exception {
        ExecutionHealthResponse mockHealth = ExecutionHealthResponse.builder()
                .status("DOWN")
                .docker("DISCONNECTED")
                .workerAvailable(false)
                .dockerAvailable(false)
                .dockerVersion("Unknown")
                .message("Execution Worker unreachable")
                .imagesStatus(Map.of())
                .timestamp(System.currentTimeMillis())
                .build();

        Mockito.when(executionClient.getHealth()).thenReturn(mockHealth);

        mockMvc.perform(get("/api/execution/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerAvailable", is(false)))
                .andExpect(jsonPath("$.dockerAvailable", is(false)))
                .andExpect(jsonPath("$.status", is("DOWN")));
    }

    @Test
    @DisplayName("1c. Public endpoint /api/execution/health safely handles null or exception from client")
    void testExecutionHealthEndpointSafelyHandlesClientNullOrException() throws Exception {
        Mockito.when(executionClient.getHealth()).thenReturn(null);

        mockMvc.perform(get("/api/execution/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workerAvailable", is(false)))
                .andExpect(jsonPath("$.dockerAvailable", is(false)))
                .andExpect(jsonPath("$.status", is("DOWN")));
    }

    @Test
    @DisplayName("2. Unauthenticated /api/execute request should be rejected with 401")
    void testUnauthenticatedExecutionRejected() throws Exception {
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.PYTHON, "print('hello')", null, null, null);

        mockMvc.perform(post("/api/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. Invalid language or empty code should fail validation with 400 Bad Request")
    void testInvalidExecutionRequestValidation() throws Exception {
        ExecutionRequest req = new ExecutionRequest(null, "", null, null, null);

        mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. Execute forwards to ExecutionClient and returns execution response")
    void testExecuteForwardsToWorker() throws Exception {
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.PYTHON, "print('Hello Worker')", null, null, null);

        ExecutionResponse mockResponse = ExecutionResponse.builder()
                .executionId("exec-12345")
                .status(ExecutionStatus.RUNNING)
                .stdout("")
                .stderr("")
                .build();

        Mockito.when(executionClient.startExecution(any(String.class), any(ExecutionRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId", notNullValue()))
                .andExpect(jsonPath("$.status", anyOf(is("QUEUED"), is("RUNNING"))));
    }

    @Test
    @DisplayName("5. Execution Ownership Security: User A can send input to their own execution")
    void testExecutionOwnershipAllowedForOwner() throws Exception {
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.JAVA, "System.out.println()", null, null, null);

        Mockito.when(executionClient.startExecution(any(String.class), any(ExecutionRequest.class)))
                .thenAnswer(inv -> ExecutionResponse.builder()
                        .executionId(inv.getArgument(0))
                        .status(ExecutionStatus.RUNNING)
                        .build());
        Mockito.when(executionClient.sendInput(any(String.class), any(String.class))).thenReturn(true);

        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String execId = objectMapper.readTree(postRes).get("executionId").asText();
        Thread.sleep(100);

        mockMvc.perform(post("/api/execute/" + execId + "/input")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExecutionInputRequest("10\n"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Stop execution to release worker concurrency permit
        mockMvc.perform(post("/api/execute/" + execId + "/stop")
                        .header("Authorization", "Bearer " + user1Token));
    }

    @Test
    @DisplayName("6. Execution Ownership Security: User B cannot send input to User A's execution (403 Forbidden)")
    void testExecutionOwnershipRejectsOtherUser() throws Exception {
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.JAVA, "System.out.println()", null, null, null);

        Mockito.when(executionClient.startExecution(any(String.class), any(ExecutionRequest.class)))
                .thenAnswer(inv -> ExecutionResponse.builder()
                        .executionId(inv.getArgument(0))
                        .status(ExecutionStatus.RUNNING)
                        .build());
        Mockito.when(executionClient.sendInput(any(String.class), any(String.class))).thenReturn(true);

        // Started by User 1
        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String execId = objectMapper.readTree(postRes).get("executionId").asText();

        // Attempted by User 2
        mockMvc.perform(post("/api/execute/" + execId + "/input")
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExecutionInputRequest("malicious input"))))
                .andExpect(status().isForbidden());

        // Stop execution to release worker concurrency permit
        mockMvc.perform(post("/api/execute/" + execId + "/stop")
                        .header("Authorization", "Bearer " + user1Token));
    }

    @Test
    @DisplayName("7. Execution Ownership Security: User B cannot stop User A's execution (403 Forbidden)")
    void testStopExecutionOwnershipRejectsOtherUser() throws Exception {
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.JAVA, "System.out.println()", null, null, null);

        Mockito.when(executionClient.startExecution(any(String.class), any(ExecutionRequest.class)))
                .thenAnswer(inv -> ExecutionResponse.builder()
                        .executionId(inv.getArgument(0))
                        .status(ExecutionStatus.RUNNING)
                        .build());

        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String execId = objectMapper.readTree(postRes).get("executionId").asText();

        // Attempted by User 2 -> 403 Forbidden
        mockMvc.perform(post("/api/execute/" + execId + "/stop")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());

        // Stop execution to release worker concurrency permit
        mockMvc.perform(post("/api/execute/" + execId + "/stop")
                        .header("Authorization", "Bearer " + user1Token));
    }

    @Test
    @DisplayName("8. Graceful failure: When worker returns SYSTEM_ERROR, status reflects the error without crashing")
    void testGracefulWorkerFailureHandling() throws Exception {
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.PYTHON, "print('hi')", null, null, null);

        Mockito.when(executionClient.startExecution(any(String.class), any(ExecutionRequest.class)))
                .thenAnswer(inv -> ExecutionResponse.builder()
                        .executionId(inv.getArgument(0))
                        .status(ExecutionStatus.SYSTEM_ERROR)
                        .stdout("")
                        .stderr("Code execution service is temporarily unavailable.")
                        .exitCode(-1)
                        .executionTimeMs(0L)
                        .build());

        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String execId = objectMapper.readTree(postRes).get("executionId").asText();
        // Wait briefly for asynchronous dispatcher loop to pick up the queued execution
        for (int i = 0; i < 20; i++) {
            Thread.sleep(50);
            String statusRes = mockMvc.perform(get("/api/execute/" + execId)
                            .header("Authorization", "Bearer " + user1Token))
                    .andReturn().getResponse().getContentAsString();
            String curStatus = objectMapper.readTree(statusRes).get("status").asText();
            if (!"QUEUED".equals(curStatus)) {
                break;
            }
        }

        mockMvc.perform(get("/api/execute/" + execId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", anyOf(is("SYSTEM_ERROR"), is("FAILED"), is("RUNNING"))));
    }

    @Test
    @DisplayName("9. Execution Ownership Security: User A can query their own execution status")
    void testGetExecutionStatusAllowedForOwner() throws Exception {
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.JAVA, "System.out.println()", null, null, null);

        Mockito.when(executionClient.startExecution(any(String.class), any(ExecutionRequest.class)))
                .thenAnswer(inv -> ExecutionResponse.builder()
                        .executionId(inv.getArgument(0))
                        .status(ExecutionStatus.SUCCESS)
                        .stdout("Secret output from user 1")
                        .exitCode(0)
                        .executionTimeMs(50L)
                        .build());

        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String execId = objectMapper.readTree(postRes).get("executionId").asText();

        // Allow dispatcher to execute and complete
        Thread.sleep(100);

        mockMvc.perform(get("/api/execute/" + execId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId", is(execId)));
    }

    @Test
    @DisplayName("10. Execution Ownership Security: User B cannot query User A's execution status (403 Forbidden)")
    void testGetExecutionStatusRejectsOtherUser() throws Exception {
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.JAVA, "System.out.println()", null, null, null);

        Mockito.when(executionClient.startExecution(any(String.class), any(ExecutionRequest.class)))
                .thenAnswer(inv -> ExecutionResponse.builder()
                        .executionId(inv.getArgument(0))
                        .status(ExecutionStatus.SUCCESS)
                        .stdout("Secret output from user 1")
                        .exitCode(0)
                        .executionTimeMs(50L)
                        .build());

        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String execId = objectMapper.readTree(postRes).get("executionId").asText();

        // Allow dispatcher to execute and complete
        Thread.sleep(100);

        // User B attempts to access User A's execution status -> 403 Forbidden
        mockMvc.perform(get("/api/execute/" + execId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11. Fail Closed: Querying unknown execution ID returns 404 without calling worker")
    void testQueryUnknownExecutionIdFailsClosed() throws Exception {
        String nonExistentId = "non-existent-" + UUID.randomUUID();

        mockMvc.perform(get("/api/execute/" + nonExistentId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNotFound());

        // Verify that executionClient.getExecutionStatus was NEVER called
        Mockito.verify(executionClient, Mockito.never()).getExecutionStatus(nonExistentId);
    }

    @Test
    @DisplayName("12. Fail Closed: Missing local record does not fall back to worker even if worker has execution data")
    void testMissingRecordDoesNotFallBackToWorker() throws Exception {
        String foreignExecId = "worker-only-" + UUID.randomUUID();

        // Even if worker mock has data for this foreign execution ID
        Mockito.when(executionClient.getExecutionStatus(foreignExecId))
                .thenReturn(ExecutionResponse.builder()
                        .executionId(foreignExecId)
                        .status(ExecutionStatus.SUCCESS)
                        .stdout("Leaked worker secret")
                        .build());

        // Requesting user has no verified local record for this execution
        mockMvc.perform(get("/api/execute/" + foreignExecId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Leaked worker secret"))));

        // Verify worker fallback was never queried
        Mockito.verify(executionClient, Mockito.never()).getExecutionStatus(foreignExecId);
    }

    @Test
    @DisplayName("13. Fail Closed: Stdin to unknown execution returns 400 Bad Request without calling worker")
    void testSendInputToUnknownExecutionFailsClosed() throws Exception {
        String nonExistentId = "non-existent-" + UUID.randomUUID();

        mockMvc.perform(post("/api/execute/" + nonExistentId + "/input")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExecutionInputRequest("input\n"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));

        Mockito.verify(executionClient, Mockito.never()).sendInput(any(), any());
    }

    @Test
    @DisplayName("14. Fail Closed: Stop unknown execution returns 404 Not Found without calling worker")
    void testStopUnknownExecutionFailsClosed() throws Exception {
        String nonExistentId = "non-existent-" + UUID.randomUUID();

        mockMvc.perform(post("/api/execute/" + nonExistentId + "/stop")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNotFound());

        Mockito.verify(executionClient, Mockito.never()).stopExecution(any());
    }
}
