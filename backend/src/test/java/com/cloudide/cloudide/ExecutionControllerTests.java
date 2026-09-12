package com.cloudide.cloudide;

import com.cloudide.cloudide.dto.ExecutionInputRequest;
import com.cloudide.cloudide.dto.ExecutionRequest;
import com.cloudide.cloudide.dto.LoginRequest;
import com.cloudide.cloudide.dto.RegisterRequest;
import com.cloudide.cloudide.enums.ProgrammingLanguage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:executiontestdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.execution.timeout-seconds=4"
})
@AutoConfigureMockMvc
class ExecutionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        String userEmail = "execuser_" + System.currentTimeMillis() + "_" + Math.random() + "@example.com";
        RegisterRequest registerReq = new RegisterRequest("ExecUser", userEmail, "password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)));

        LoginRequest loginReq = new LoginRequest(userEmail, "password123");
        String loginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(loginRes);
        jwtToken = root.get("token").asText();
    }

    private JsonNode waitForExecutionCompletion(String executionId, int maxWaitSeconds) throws Exception {
        long deadline = System.currentTimeMillis() + (maxWaitSeconds * 1000L);
        while (System.currentTimeMillis() < deadline) {
            String res = mockMvc.perform(get("/api/execute/" + executionId)
                            .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode node = objectMapper.readTree(res);
            String status = node.get("status").asText();
            if (!"RUNNING".equals(status) && !"WAITING_FOR_INPUT".equals(status) && !"QUEUED".equals(status)) {
                return node;
            }
            Thread.sleep(80);
        }
        throw new TimeoutException("Execution " + executionId + " did not complete within " + maxWaitSeconds + "s");
    }

    @Test
    @DisplayName("1. Public endpoint /api/execution/health returns Docker availability")
    void testExecutionHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/execution/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dockerAvailable", is(true)))
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.imagesStatus", notNullValue()));
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
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. Execute Python code with stdin input")
    void testPythonExecutionWithStdin() throws Exception {
        String pyCode = "name = input()\nage = int(input())\nprint(f'Hello {name}, you are {age + 5} in 5 years!')";
        String stdin = "Alice\n25\n";
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.PYTHON, pyCode, stdin, null, null);

        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode postNode = objectMapper.readTree(postRes);
        String executionId = postNode.get("executionId").asText();

        JsonNode finalRes = waitForExecutionCompletion(executionId, 10);
        assertThat(finalRes.get("status").asText(), is("SUCCESS"));
        assertThat(finalRes.get("exitCode").asInt(), is(0));
        assertThat(finalRes.get("stdout").asText(), containsString("Hello Alice, you are 30 in 5 years!"));
    }

    @Test
    @DisplayName("5. Execute Java 21 LTS code in Docker container")
    void testJavaExecution() throws Exception {
        String javaCode = """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Java 21 Online Sandbox Working!");
                    }
                }
                """;
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.JAVA, javaCode, null, null, null);

        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode postNode = objectMapper.readTree(postRes);
        String executionId = postNode.get("executionId").asText();

        JsonNode finalRes = waitForExecutionCompletion(executionId, 10);
        assertThat(finalRes.get("status").asText(), is("SUCCESS"));
        assertThat(finalRes.get("exitCode").asInt(), is(0));
        assertThat(finalRes.get("stdout").asText(), containsString("Java 21 Online Sandbox Working!"));
    }

    @Test
    @DisplayName("6. Execute C23 code in Docker container")
    void testCExecution() throws Exception {
        String cCode = """
                #include <stdio.h>
                int main() {
                    printf("Hello from C Sandbox!\\n");
                    return 0;
                }
                """;
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.C, cCode, null, null, null);

        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode postNode = objectMapper.readTree(postRes);
        String executionId = postNode.get("executionId").asText();

        JsonNode finalRes = waitForExecutionCompletion(executionId, 10);
        assertThat(finalRes.get("status").asText(), is("SUCCESS"));
        assertThat(finalRes.get("exitCode").asInt(), is(0));
        assertThat(finalRes.get("stdout").asText(), containsString("Hello from C Sandbox!"));
    }

    @Test
    @DisplayName("7. Execute C++20 code in Docker container")
    void testCppExecution() throws Exception {
        String cppCode = """
                #include <iostream>
                #include <vector>
                int main() {
                    std::vector<int> nums = {10, 20, 30};
                    int sum = 0;
                    for (int n : nums) sum += n;
                    std::cout << "Sum = " << sum << std::endl;
                    return 0;
                }
                """;
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.CPP, cppCode, null, null, null);

        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode postNode = objectMapper.readTree(postRes);
        String executionId = postNode.get("executionId").asText();

        JsonNode finalRes = waitForExecutionCompletion(executionId, 10);
        assertThat(finalRes.get("status").asText(), is("SUCCESS"));
        assertThat(finalRes.get("exitCode").asInt(), is(0));
        assertThat(finalRes.get("stdout").asText(), containsString("Sum = 60"));
    }

    @Test
    @DisplayName("8. Java Compilation Error captures compiler stderr (COMPILATION_ERROR)")
    void testJavaCompilationError() throws Exception {
        String invalidJava = """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Missing semicolon")
                    }
                }
                """;
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.JAVA, invalidJava, null, null, null);

        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode postNode = objectMapper.readTree(postRes);
        String executionId = postNode.get("executionId").asText();

        JsonNode finalRes = waitForExecutionCompletion(executionId, 10);
        assertThat(finalRes.get("status").asText(), is("COMPILATION_ERROR"));
        assertThat(finalRes.get("stderr").asText(), containsString("error: ';' expected"));
    }

    @Test
    @DisplayName("9. Python Runtime Error captures traceback (RUNTIME_ERROR)")
    void testPythonRuntimeError() throws Exception {
        String brokenPy = "x = 10 / 0";
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.PYTHON, brokenPy, null, null, null);

        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode postNode = objectMapper.readTree(postRes);
        String executionId = postNode.get("executionId").asText();

        JsonNode finalRes = waitForExecutionCompletion(executionId, 10);
        assertThat(finalRes.get("status").asText(), is("RUNTIME_ERROR"));
        assertThat(finalRes.get("stderr").asText(), containsString("ZeroDivisionError: division by zero"));
    }

    @Test
    @DisplayName("10. Python infinite loop triggers TIMEOUT (124) and kills container")
    void testPythonTimeout() throws Exception {
        String infinitePy = "while True: pass";
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.PYTHON, infinitePy, null, null, null, 2);

        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode postNode = objectMapper.readTree(postRes);
        String executionId = postNode.get("executionId").asText();

        JsonNode finalRes = waitForExecutionCompletion(executionId, 6);
        assertThat(finalRes.get("status").asText(), is("TIMEOUT"));
        assertThat(finalRes.get("exitCode").asInt(), is(124));
    }

    @Test
    @DisplayName("11. Interactive Stdin: Java Scanner reading multiple inputs sequentially via POST /api/execute/{id}/input")
    void testJavaInteractiveMultipleInputs() throws Exception {
        String javaCode = """
                import java.util.Scanner;
                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int a = sc.nextInt();
                        int b = sc.nextInt();
                        System.out.println("Sum = " + (a + b));
                    }
                }
                """;
        // Start without stdin
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.JAVA, javaCode, null, null, null, 6);

        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode postNode = objectMapper.readTree(postRes);
        String executionId = postNode.get("executionId").asText();

        // Give process brief moment to reach sc.nextInt()
        Thread.sleep(600);

        // Send first input: 10
        mockMvc.perform(post("/api/execute/" + executionId + "/input")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExecutionInputRequest("10"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Send second input: 32
        mockMvc.perform(post("/api/execute/" + executionId + "/input")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExecutionInputRequest("32"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        JsonNode finalRes = waitForExecutionCompletion(executionId, 6);
        assertThat(finalRes.get("status").asText(), is("SUCCESS"));
        assertThat(finalRes.get("exitCode").asInt(), is(0));
        assertThat(finalRes.get("stdout").asText(), containsString("Sum = 42"));
    }

    @Test
    @DisplayName("12. Interactive Stdin: Python input() prompted and answered interactively")
    void testPythonInteractiveInput() throws Exception {
        String pyCode = """
                name = input("Enter name: ")
                print(f"Hello {name}!")
                """;
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.PYTHON, pyCode, null, null, null, 6);

        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode postNode = objectMapper.readTree(postRes);
        String executionId = postNode.get("executionId").asText();

        Thread.sleep(400);

        mockMvc.perform(post("/api/execute/" + executionId + "/input")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExecutionInputRequest("Harish"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        JsonNode finalRes = waitForExecutionCompletion(executionId, 6);
        assertThat(finalRes.get("status").asText(), is("SUCCESS"));
        assertThat(finalRes.get("stdout").asText(), containsString("Hello Harish!"));
    }

    @Test
    @DisplayName("13. Java Runtime Error (ArithmeticException) returns RUNTIME_ERROR")
    void testJavaRuntimeError() throws Exception {
        String javaCode = """
                public class Main {
                    public static void main(String[] args) {
                        int x = 10 / 0;
                    }
                }
                """;
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.JAVA, javaCode, null, null, null);

        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode postNode = objectMapper.readTree(postRes);
        String executionId = postNode.get("executionId").asText();

        JsonNode finalRes = waitForExecutionCompletion(executionId, 10);
        assertThat(finalRes.get("status").asText(), is("RUNTIME_ERROR"));
        assertThat(finalRes.get("stderr").asText(), containsString("ArithmeticException: / by zero"));
    }

    @Test
    @DisplayName("14. Java infinite loop triggers TIMEOUT (124) and kills container")
    void testJavaTimeout() throws Exception {
        String infiniteJava = """
                public class Main {
                    public static void main(String[] args) {
                        while (true) {
                        }
                    }
                }
                """;
        ExecutionRequest req = new ExecutionRequest(ProgrammingLanguage.JAVA, infiniteJava, null, null, null, 2);

        String postRes = mockMvc.perform(post("/api/execute")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode postNode = objectMapper.readTree(postRes);
        String executionId = postNode.get("executionId").asText();

        JsonNode finalRes = waitForExecutionCompletion(executionId, 6);
        assertThat(finalRes.get("status").asText(), is("TIMEOUT"));
        assertThat(finalRes.get("exitCode").asInt(), is(124));
    }
}
