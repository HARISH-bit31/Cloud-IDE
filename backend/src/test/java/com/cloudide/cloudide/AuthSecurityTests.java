package com.cloudide.cloudide;

import com.cloudide.cloudide.dto.FileRequest;
import com.cloudide.cloudide.dto.LoginRequest;
import com.cloudide.cloudide.dto.ProjectRequest;
import com.cloudide.cloudide.dto.RegisterRequest;
import com.cloudide.cloudide.enums.ProgrammingLanguage;
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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:authsecuritydb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "execution.worker.secret=test_worker_secret_12345"
})
@AutoConfigureMockMvc
class AuthSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Public endpoints like /api/health should be accessible without authentication")
    void testPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("Registration, duplicate rejection, login, and /api/auth/me flow")
    void testRegistrationAndLoginFlow() throws Exception {
        // 1. Register User Harish
        RegisterRequest registerReq = new RegisterRequest("Harish", "harish@example.com", "password123");

        String registerJson = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Harish")))
                .andExpect(jsonPath("$.email", is("harish@example.com")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        // 2. Reject duplicate email with 409 Conflict
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Email already registered")));

        // 3. Validation failure for invalid email and short password
        RegisterRequest invalidReq = new RegisterRequest("", "not-an-email", "123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(1))));

        // 4. Login with correct credentials
        LoginRequest loginReq = new LoginRequest("harish@example.com", "password123");
        String loginJson = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.name", is("Harish")))
                .andExpect(jsonPath("$.user.email", is("harish@example.com")))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String token = com.jayway.jsonpath.JsonPath.read(loginJson, "$.token");

        // 5. Login with wrong password returns 401 Unauthorized
        LoginRequest wrongPassReq = new LoginRequest("harish@example.com", "wrongpassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPassReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));

        // 6. Login with unknown email returns 401 Unauthorized
        LoginRequest unknownEmailReq = new LoginRequest("unknown@example.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownEmailReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));

        // 7. GET /api/auth/me with valid Bearer token
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Harish")))
                .andExpect(jsonPath("$.email", is("harish@example.com")))
                .andExpect(jsonPath("$.password").doesNotExist());

        // 8. GET /api/auth/me without token returns 401
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        // 9. GET /api/auth/me with invalid token returns 401
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("Protected endpoints must reject unauthenticated requests with 401")
    void testProtectedEndpointsRejectUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("Strict ownership isolation: User A cannot access, modify, or delete User B's projects or files")
    void testProjectAndFileOwnershipIsolation() throws Exception {
        // Register & Login User A (Alice)
        RegisterRequest regAlice = new RegisterRequest("Alice", "alice@example.com", "password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regAlice)))
                .andExpect(status().isCreated());

        String loginAliceJson = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("alice@example.com", "password123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String tokenAlice = com.jayway.jsonpath.JsonPath.read(loginAliceJson, "$.token");

        // Alice creates Project A
        ProjectRequest createAliceProj = ProjectRequest.builder()
                .name("Alice Secret Project")
                .description("Private repository for Alice")
                .language(ProgrammingLanguage.JAVA)
                .build();

        String aliceProjJson = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAliceProj)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Alice Secret Project")))
                .andReturn().getResponse().getContentAsString();

        Number aliceProjIdNum = com.jayway.jsonpath.JsonPath.read(aliceProjJson, "$.id");
        Long aliceProjId = aliceProjIdNum.longValue();

        // Alice creates File in Project A
        FileRequest aliceFileReq = FileRequest.builder()
                .filename("AliceSecret.java")
                .content("public class AliceSecret {}")
                .language(ProgrammingLanguage.JAVA)
                .build();

        String aliceFileJson = mockMvc.perform(post("/api/projects/" + aliceProjId + "/files")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aliceFileReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Number aliceFileIdNum = com.jayway.jsonpath.JsonPath.read(aliceFileJson, "$.id");
        Long aliceFileId = aliceFileIdNum.longValue();

        // Register & Login User B (Bob)
        RegisterRequest regBob = new RegisterRequest("Bob", "bob@example.com", "password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regBob)))
                .andExpect(status().isCreated());

        String loginBobJson = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("bob@example.com", "password123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String tokenBob = com.jayway.jsonpath.JsonPath.read(loginBobJson, "$.token");

        // 1. Bob lists projects -> Should NOT see Alice's project
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 2. Bob attempts to GET Alice's project -> 403 Forbidden
        mockMvc.perform(get("/api/projects/" + aliceProjId)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Access denied")));

        // 3. Bob attempts to PUT / update Alice's project -> 403 Forbidden
        ProjectRequest hackReq = ProjectRequest.builder()
                .name("Hacked by Bob")
                .language(ProgrammingLanguage.JAVA)
                .build();

        mockMvc.perform(put("/api/projects/" + aliceProjId)
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hackReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // 4. Bob attempts to DELETE Alice's project -> 403 Forbidden
        mockMvc.perform(delete("/api/projects/" + aliceProjId)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // 5. Bob attempts to GET Alice's files -> 403 Forbidden
        mockMvc.perform(get("/api/projects/" + aliceProjId + "/files")
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // 6. Bob attempts to POST a file into Alice's project -> 403 Forbidden
        mockMvc.perform(post("/api/projects/" + aliceProjId + "/files")
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aliceFileReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // 7. Bob attempts to DELETE Alice's file -> 403 Forbidden
        mockMvc.perform(delete("/api/projects/" + aliceProjId + "/files/" + aliceFileId)
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // 8. Verify Alice can still access and manage her project
        mockMvc.perform(get("/api/projects/" + aliceProjId)
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alice Secret Project")));

        mockMvc.perform(get("/api/projects/" + aliceProjId + "/files")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }
}
