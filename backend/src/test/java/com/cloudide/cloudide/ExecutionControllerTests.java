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

import java.util.Map;

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
        Thread.sleep(150);

        mockMvc.perform(get("/api/execute/" + execId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", anyOf(is("SYSTEM_ERROR"), is("FAILED"), is("RUNNING"))));
    }
}
