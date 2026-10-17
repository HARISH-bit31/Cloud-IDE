package com.cloudide.cloudide;

import com.cloudide.cloudide.dto.ExecutionRequest;
import com.cloudide.cloudide.dto.FileRequest;
import com.cloudide.cloudide.dto.LoginRequest;
import com.cloudide.cloudide.dto.ProjectRequest;
import com.cloudide.cloudide.dto.RegisterRequest;
import com.cloudide.cloudide.entity.Project;
import com.cloudide.cloudide.entity.User;
import com.cloudide.cloudide.enums.ProgrammingLanguage;
import com.cloudide.cloudide.exception.ForbiddenException;
import com.cloudide.cloudide.repository.FileRepository;
import com.cloudide.cloudide.repository.ProjectRepository;
import com.cloudide.cloudide.repository.UserRepository;
import com.cloudide.cloudide.service.ExecutionDispatcher;
import com.cloudide.cloudide.service.ExecutionService;
import com.cloudide.cloudide.service.FileService;
import com.cloudide.cloudide.service.ProjectService;
import com.cloudide.cloudide.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ownershiptestdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "execution.worker.secret=test_worker_secret_12345"
})
@AutoConfigureMockMvc
class ProjectOwnershipSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    private String user1Token;
    private String user2Token;
    private Long user1ProjectId;
    private Long user1FileId;

    @BeforeEach
    void setUp() throws Exception {
        fileRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Register User 1
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest("User One", "user1@cloud-ide.io", "password123"))))
                .andExpect(status().isCreated());

        String login1 = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user1@cloud-ide.io", "password123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        user1Token = com.jayway.jsonpath.JsonPath.read(login1, "$.token");

        // 2. Register User 2
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RegisterRequest("User Two", "user2@cloud-ide.io", "password123"))))
                .andExpect(status().isCreated());

        String login2 = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user2@cloud-ide.io", "password123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        user2Token = com.jayway.jsonpath.JsonPath.read(login2, "$.token");

        // 3. User 1 creates a project
        String createProjJson = mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ProjectRequest("User 1 Project", "Description", ProgrammingLanguage.JAVA, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        user1ProjectId = ((Number) com.jayway.jsonpath.JsonPath.read(createProjJson, "$.id")).longValue();
        user1FileId = ((Number) com.jayway.jsonpath.JsonPath.read(createProjJson, "$.files[0].id")).longValue();
    }

    @Test
    @DisplayName("User accessing their own project succeeds")
    void testUserAccessOwnProject() throws Exception {
        mockMvc.perform(get("/api/projects/" + user1ProjectId)
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(user1ProjectId.intValue())))
                .andExpect(jsonPath("$.name", is("User 1 Project")));
    }

    @Test
    @DisplayName("User attempting to access another user's project is rejected with 403 Forbidden")
    void testUserAccessOtherUserProjectRejected() throws Exception {
        mockMvc.perform(get("/api/projects/" + user1ProjectId)
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User attempting to update another user's project is rejected with 403 Forbidden")
    void testUserUpdateOtherUserProjectRejected() throws Exception {
        ProjectRequest updateReq = new ProjectRequest("Hacked Project", "Hacked", ProgrammingLanguage.JAVA, null);
        mockMvc.perform(put("/api/projects/" + user1ProjectId)
                .header("Authorization", "Bearer " + user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User attempting to delete another user's project is rejected with 403 Forbidden")
    void testUserDeleteOtherUserProjectRejected() throws Exception {
        mockMvc.perform(delete("/api/projects/" + user1ProjectId)
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("File operations on unauthorized project are rejected with 403 Forbidden")
    void testFileOperationsOnUnauthorizedProjectRejected() throws Exception {
        // GET files
        mockMvc.perform(get("/api/projects/" + user1ProjectId + "/files")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());

        // GET single file
        mockMvc.perform(get("/api/projects/" + user1ProjectId + "/files/" + user1FileId)
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());

        // POST create file
        FileRequest newFile = new FileRequest("Test.java", "public class Test {}", ProgrammingLanguage.JAVA);
        mockMvc.perform(post("/api/projects/" + user1ProjectId + "/files")
                .header("Authorization", "Bearer " + user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newFile)))
                .andExpect(status().isForbidden());

        // PUT update file
        FileRequest updateFile = new FileRequest("Main.java", "// Hacked", ProgrammingLanguage.JAVA);
        mockMvc.perform(put("/api/projects/" + user1ProjectId + "/files/" + user1FileId)
                .header("Authorization", "Bearer " + user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateFile)))
                .andExpect(status().isForbidden());

        // DELETE file
        mockMvc.perform(delete("/api/projects/" + user1ProjectId + "/files/" + user1FileId)
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ProjectService strictly rejects access, update, and delete on orphan project (null owner)")
    void testProjectServiceRejectsOrphanProject() {
        ProjectRepository mockProjRepo = Mockito.mock(ProjectRepository.class);
        FileRepository mockFileRepo = Mockito.mock(FileRepository.class);

        User currentUser = User.builder().id(42L).email("user@cloud-ide.io").build();
        UserService testUserSvc = new UserService(null, null, null, null) {
            @Override
            public User getCurrentAuthenticatedUser() {
                return currentUser;
            }
        };

        Project orphanProject = Project.builder()
                .id(999L)
                .name("Orphan Project")
                .language(ProgrammingLanguage.JAVA)
                .user(null) // Owner is null
                .build();
        when(mockProjRepo.findById(999L)).thenReturn(Optional.of(orphanProject));

        ProjectService service = new ProjectService(mockProjRepo, mockFileRepo, testUserSvc);

        // Access check
        assertThrows(ForbiddenException.class, () -> service.getProjectById(999L),
                "Should reject access to orphan project");

        // Update check
        ProjectRequest updateReq = new ProjectRequest("New Name", "Desc", ProgrammingLanguage.JAVA, null);
        assertThrows(ForbiddenException.class, () -> service.updateProject(999L, updateReq),
                "Should reject update to orphan project");

        // Delete check
        assertThrows(ForbiddenException.class, () -> service.deleteProject(999L),
                "Should reject delete of orphan project");
    }

    @Test
    @DisplayName("FileService strictly rejects file operations on orphan project (null owner)")
    void testFileServiceRejectsOrphanProject() {
        ProjectRepository mockProjRepo = Mockito.mock(ProjectRepository.class);
        FileRepository mockFileRepo = Mockito.mock(FileRepository.class);

        User currentUser = User.builder().id(42L).email("user@cloud-ide.io").build();
        UserService testUserSvc = new UserService(null, null, null, null) {
            @Override
            public User getCurrentAuthenticatedUser() {
                return currentUser;
            }
        };

        Project orphanProject = Project.builder()
                .id(999L)
                .name("Orphan Project")
                .language(ProgrammingLanguage.PYTHON)
                .user(null) // Owner is null
                .build();
        when(mockProjRepo.findById(999L)).thenReturn(Optional.of(orphanProject));

        FileService service = new FileService(mockFileRepo, mockProjRepo, testUserSvc);

        // Get files
        assertThrows(ForbiddenException.class, () -> service.getFilesByProjectId(999L),
                "Should reject getFiles on orphan project");

        // Get single file
        assertThrows(ForbiddenException.class, () -> service.getFileById(999L, 1L),
                "Should reject getFileById on orphan project");

        // Create file
        FileRequest fileReq = new FileRequest("test.py", "print(1)", ProgrammingLanguage.PYTHON);
        assertThrows(ForbiddenException.class, () -> service.createFile(999L, fileReq),
                "Should reject createFile on orphan project");

        // Update file
        assertThrows(ForbiddenException.class, () -> service.updateFile(999L, 1L, fileReq),
                "Should reject updateFile on orphan project");

        // Delete file
        assertThrows(ForbiddenException.class, () -> service.deleteFile(999L, 1L),
                "Should reject deleteFile on orphan project");
    }

    @Test
    @DisplayName("ExecutionService strictly rejects execution under orphan project (null owner)")
    void testExecutionServiceRejectsOrphanProject() {
        ProjectRepository mockProjRepo = Mockito.mock(ProjectRepository.class);

        User currentUser = User.builder().id(42L).email("user@cloud-ide.io").build();
        UserService testUserSvc = new UserService(null, null, null, null) {
            @Override
            public User getCurrentAuthenticatedUser() {
                return currentUser;
            }
        };

        Project orphanProject = Project.builder()
                .id(999L)
                .name("Orphan Project")
                .language(ProgrammingLanguage.JAVA)
                .user(null) // Owner is null
                .build();
        when(mockProjRepo.findById(999L)).thenReturn(Optional.of(orphanProject));

        ExecutionService executionService = new ExecutionService(null, null, testUserSvc, mockProjRepo);

        ExecutionRequest request = new ExecutionRequest(ProgrammingLanguage.JAVA, "public class Main {}", null, 999L, null);
        assertThrows(ForbiddenException.class, () -> executionService.startExecution(request),
                "Should reject execution under orphan project");
    }

    @Test
    @DisplayName("X-Frame-Options header is set to DENY in non-dev profile")
    void testFrameOptionsHeaderIsDeny() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    @DisplayName("H2 console is not publicly accessible in non-dev profile")
    void testH2ConsoleRequiresAuthInNonDevProfile() throws Exception {
        mockMvc.perform(get("/h2-console"))
                .andExpect(status().isUnauthorized());
    }
}
