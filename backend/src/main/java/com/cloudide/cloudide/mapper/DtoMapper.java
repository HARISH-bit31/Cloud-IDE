package com.cloudide.cloudide.mapper;

import com.cloudide.cloudide.dto.AuthUserResponse;
import com.cloudide.cloudide.dto.FileResponse;
import com.cloudide.cloudide.dto.ProjectResponse;
import com.cloudide.cloudide.dto.UserResponse;
import com.cloudide.cloudide.entity.File;
import com.cloudide.cloudide.entity.Project;
import com.cloudide.cloudide.entity.User;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DtoMapper {

    public static UserResponse toUserResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static AuthUserResponse toAuthUserResponse(User user) {
        if (user == null) return null;
        return AuthUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public static ProjectResponse toProjectResponse(Project project, List<File> files) {
        if (project == null) return null;
        List<FileResponse> fileResponses = files != null
                ? files.stream().map(DtoMapper::toFileResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .language(project.getLanguage())
                .userId(project.getUser() != null ? project.getUser().getId() : null)
                .userName(project.getUser() != null ? project.getUser().getName() : null)
                .fileCount(fileResponses.size())
                .files(fileResponses)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    public static FileResponse toFileResponse(File file) {
        if (file == null) return null;
        return FileResponse.builder()
                .id(file.getId())
                .filename(file.getFilename())
                .content(file.getContent())
                .language(file.getLanguage())
                .projectId(file.getProject() != null ? file.getProject().getId() : null)
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }
}
