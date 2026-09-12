package com.cloudide.cloudide.service;

import com.cloudide.cloudide.dto.ProjectRequest;
import com.cloudide.cloudide.dto.ProjectResponse;
import com.cloudide.cloudide.entity.File;
import com.cloudide.cloudide.entity.Project;
import com.cloudide.cloudide.entity.User;
import com.cloudide.cloudide.enums.ProgrammingLanguage;
import com.cloudide.cloudide.exception.ForbiddenException;
import com.cloudide.cloudide.exception.ResourceNotFoundException;
import com.cloudide.cloudide.mapper.DtoMapper;
import com.cloudide.cloudide.repository.FileRepository;
import com.cloudide.cloudide.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final FileRepository fileRepository;
    private final UserService userService;

    public ProjectService(ProjectRepository projectRepository, FileRepository fileRepository, UserService userService) {
        this.projectRepository = projectRepository;
        this.fileRepository = fileRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        User currentUser = userService.getCurrentAuthenticatedUser();
        return projectRepository.findByUserIdOrderByUpdatedAtDesc(currentUser.getId()).stream()
                .map(p -> {
                    List<File> files = fileRepository.findByProjectIdOrderByIdAsc(p.getId());
                    return DtoMapper.toProjectResponse(p, files);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = getProjectWithOwnershipCheck(id);
        List<File> files = fileRepository.findByProjectIdOrderByIdAsc(project.getId());
        return DtoMapper.toProjectResponse(project, files);
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        User currentUser = userService.getCurrentAuthenticatedUser();

        Project project = Project.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .language(request.getLanguage() != null ? request.getLanguage() : ProgrammingLanguage.JAVA)
                .user(currentUser)
                .build();

        Project savedProject = projectRepository.save(project);

        String defaultFilename = getDefaultFilename(savedProject.getLanguage());
        String defaultCode = getDefaultStarterCode(savedProject.getLanguage());

        File initialFile = File.builder()
                .filename(defaultFilename)
                .content(defaultCode)
                .language(savedProject.getLanguage())
                .project(savedProject)
                .build();

        fileRepository.save(initialFile);
        log.info("Created project id: {} for user: {} with initial file: {}", savedProject.getId(), currentUser.getEmail(), defaultFilename);

        List<File> files = fileRepository.findByProjectIdOrderByIdAsc(savedProject.getId());
        return DtoMapper.toProjectResponse(savedProject, files);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = getProjectWithOwnershipCheck(id);

        project.setName(request.getName().trim());
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getLanguage() != null) {
            project.setLanguage(request.getLanguage());
        }

        Project updated = projectRepository.save(project);
        List<File> files = fileRepository.findByProjectIdOrderByIdAsc(updated.getId());
        log.info("Updated project id: {}", updated.getId());
        return DtoMapper.toProjectResponse(updated, files);
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = getProjectWithOwnershipCheck(id);

        fileRepository.deleteByProjectId(project.getId());
        projectRepository.delete(project);
        log.info("Deleted project id: {}", id);
    }

    private Project getProjectWithOwnershipCheck(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        User currentUser = userService.getCurrentAuthenticatedUser();
        if (project.getUser() != null && !project.getUser().getId().equals(currentUser.getId())) {
            log.warn("Security violation: User {} attempted to access project {} owned by user {}",
                    currentUser.getId(), id, project.getUser().getId());
            throw new ForbiddenException("Access denied: You do not own this project");
        }

        return project;
    }

    private String getDefaultFilename(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA -> "Main.java";
            case PYTHON -> "main.py";
            case C -> "main.c";
            case CPP -> "main.cpp";
        };
    }

    private String getDefaultStarterCode(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA -> """
                    // Cloud IDE Fast-Runner Sandbox (JDK 21 LTS)
                    import java.util.Scanner;

                    public class Main {
                        public static void main(String[] args) {
                            System.out.println("Hello Cloud IDE!");
                            Scanner sc = new Scanner(System.in);
                            int a = sc.hasNextInt() ? sc.nextInt() : 10;
                            int b = sc.hasNextInt() ? sc.nextInt() : 32;
                            System.out.printf("Sum = %d%n", (a + b));
                            sc.close();
                        }
                    }
                    """;
            case PYTHON -> """
                    # Cloud IDE Python 3.12 Sandbox
                    import sys

                    def main():
                        print("🚀 Cloud IDE Python Environment v3.12")
                        print("Hello Cloud IDE!")

                    if __name__ == "__main__":
                        main()
                    """;
            case C -> """
                    #include <stdio.h>

                    int main(void) {
                        printf("=== Cloud IDE GCC 13.2 Runner ===\\n");
                        printf("Hello Cloud IDE!\\n");
                        return 0;
                    }
                    """;
            case CPP -> """
                    #include <iostream>

                    int main() {
                        std::cout << "🚀 Cloud IDE C++20 Sandbox\\n";
                        std::cout << "Hello Cloud IDE!\\n";
                        return 0;
                    }
                    """;
        };
    }
}
