package com.cloudide.cloudide.service;

import com.cloudide.cloudide.dto.FileRequest;
import com.cloudide.cloudide.dto.FileResponse;
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
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileRepository fileRepository;
    private final ProjectRepository projectRepository;
    private final UserService userService;

    public FileService(FileRepository fileRepository, ProjectRepository projectRepository, UserService userService) {
        this.fileRepository = fileRepository;
        this.projectRepository = projectRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<FileResponse> getFilesByProjectId(Long projectId) {
        getProjectWithOwnershipCheck(projectId);
        return fileRepository.findByProjectIdOrderByIdAsc(projectId).stream()
                .map(DtoMapper::toFileResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FileResponse getFileById(Long projectId, Long fileId) {
        getProjectWithOwnershipCheck(projectId);
        File file = fileRepository.findByIdAndProjectId(fileId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId + " in project id: " + projectId));
        return DtoMapper.toFileResponse(file);
    }

    @Transactional
    public FileResponse createFile(Long projectId, FileRequest request) {
        Project project = getProjectWithOwnershipCheck(projectId);

        ProgrammingLanguage language = request.getLanguage() != null
                ? request.getLanguage()
                : detectLanguageFromFilename(request.getFilename(), project.getLanguage());

        File file = File.builder()
                .filename(request.getFilename().trim())
                .content(request.getContent() != null ? request.getContent() : "")
                .language(language)
                .project(project)
                .build();

        File saved = fileRepository.save(file);
        log.info("Created file id: {} ({}) in project id: {}", saved.getId(), saved.getFilename(), projectId);
        return DtoMapper.toFileResponse(saved);
    }

    @Transactional
    public FileResponse updateFile(Long projectId, Long fileId, FileRequest request) {
        getProjectWithOwnershipCheck(projectId);
        File file = fileRepository.findByIdAndProjectId(fileId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId + " in project id: " + projectId));

        if (request.getFilename() != null && !request.getFilename().isBlank()) {
            file.setFilename(request.getFilename().trim());
        }
        if (request.getContent() != null) {
            file.setContent(request.getContent());
        }
        if (request.getLanguage() != null) {
            file.setLanguage(request.getLanguage());
        }

        File updated = fileRepository.save(file);
        log.info("Updated file id: {} in project id: {}", updated.getId(), projectId);
        return DtoMapper.toFileResponse(updated);
    }

    @Transactional
    public void deleteFile(Long projectId, Long fileId) {
        getProjectWithOwnershipCheck(projectId);
        File file = fileRepository.findByIdAndProjectId(fileId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId + " in project id: " + projectId));

        fileRepository.delete(file);
        log.info("Deleted file id: {} from project id: {}", fileId, projectId);
    }

    private Project getProjectWithOwnershipCheck(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        User currentUser = userService.getCurrentAuthenticatedUser();
        if (project.getUser() != null && !project.getUser().getId().equals(currentUser.getId())) {
            log.warn("Security violation: User {} attempted to access files in project {} owned by user {}",
                    currentUser.getId(), projectId, project.getUser().getId());
            throw new ForbiddenException("Access denied: You do not own this project");
        }

        return project;
    }

    private ProgrammingLanguage detectLanguageFromFilename(String filename, ProgrammingLanguage defaultLang) {
        if (filename == null) return defaultLang;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".java")) return ProgrammingLanguage.JAVA;
        if (lower.endsWith(".py")) return ProgrammingLanguage.PYTHON;
        if (lower.endsWith(".c")) return ProgrammingLanguage.C;
        if (lower.endsWith(".cpp") || lower.endsWith(".cc") || lower.endsWith(".cxx")) return ProgrammingLanguage.CPP;
        return defaultLang;
    }
}
