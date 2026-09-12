package com.cloudide.cloudide.dto;

import com.cloudide.cloudide.enums.ProgrammingLanguage;
import java.time.LocalDateTime;

public class FileResponse {
    private Long id;
    private String filename;
    private String content;
    private ProgrammingLanguage language;
    private Long projectId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FileResponse() {
    }

    public FileResponse(Long id, String filename, String content, ProgrammingLanguage language, Long projectId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.filename = filename;
        this.content = content;
        this.language = language;
        this.projectId = projectId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ProgrammingLanguage getLanguage() {
        return language;
    }

    public void setLanguage(ProgrammingLanguage language) {
        this.language = language;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String filename;
        private String content;
        private ProgrammingLanguage language;
        private Long projectId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder language(ProgrammingLanguage language) {
            this.language = language;
            return this;
        }

        public Builder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public FileResponse build() {
            return new FileResponse(id, filename, content, language, projectId, createdAt, updatedAt);
        }
    }
}
