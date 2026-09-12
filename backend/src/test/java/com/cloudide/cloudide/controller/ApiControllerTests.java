package com.cloudide.cloudide.controller;

import com.cloudide.cloudide.dto.FileRequest;
import com.cloudide.cloudide.dto.LoginRequest;
import com.cloudide.cloudide.dto.ProjectRequest;
import com.cloudide.cloudide.dto.RegisterRequest;
import com.cloudide.cloudide.enums.ProgrammingLanguage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
        "spring.datasource.url=jdbc:h2:mem:apicontrollertestdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ApiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("Test Dev", "test.dev@cloud-ide.io", "password123");
        try {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerReq)));
        } catch (Exception ignored) {
        }

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("test.dev@cloud-ide.io", "password123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        this.authToken = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.token");
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("Cloud IDE Backend")));
    }

    @Test
    void testProjectCrudFlow() throws Exception {
        // 1. List projects
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        // 2. Create project
        ProjectRequest createReq = ProjectRequest.builder()
                .name("Spring Boot REST Lab")
                .description("Automated backend integration project")
                .language(ProgrammingLanguage.JAVA)
                .build();

        String createResponse = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Spring Boot REST Lab")))
                .andExpect(jsonPath("$.language", is("JAVA")))
                .andExpect(jsonPath("$.files", hasSize(greaterThanOrEqualTo(1))))
                .andReturn().getResponse().getContentAsString();

        Number projectIdNum = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");
        Long projectId = projectIdNum.longValue();

        // 3. Get project by ID
        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(projectId.intValue())))
                .andExpect(jsonPath("$.name", is("Spring Boot REST Lab")));

        // 4. Create new file in project
        FileRequest fileReq = FileRequest.builder()
                .filename("Service.java")
                .content("public class Service {}")
                .language(ProgrammingLanguage.JAVA)
                .build();

        String fileResponse = mockMvc.perform(post("/api/projects/" + projectId + "/files")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fileReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename", is("Service.java")))
                .andReturn().getResponse().getContentAsString();

        Number fileIdNum = com.jayway.jsonpath.JsonPath.read(fileResponse, "$.id");
        Long fileId = fileIdNum.longValue();

        // 5. Update file
        FileRequest updateFileReq = FileRequest.builder()
                .filename("Service.java")
                .content("public class Service { public void run() {} }")
                .language(ProgrammingLanguage.JAVA)
                .build();

        mockMvc.perform(put("/api/projects/" + projectId + "/files/" + fileId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateFileReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", containsString("public void run()")));

        // 6. Delete file
        mockMvc.perform(delete("/api/projects/" + projectId + "/files/" + fileId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        // 7. Delete project
        mockMvc.perform(delete("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        // 8. Verify project 404 after delete
        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }
}
