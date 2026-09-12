package com.cloudide.cloudide.dto;

import com.cloudide.cloudide.enums.ProgrammingLanguage;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ExecutionRequest {

    @NotNull(message = "Programming language is required")
    private ProgrammingLanguage language;

    @NotBlank(message = "Source code cannot be empty")
    private String code;

    @JsonAlias({"input", "stdinInput"})
    private String stdin;

    private Long projectId;

    private Long fileId;

    private Integer timeoutSeconds;

    public ExecutionRequest() {
    }

    public ExecutionRequest(ProgrammingLanguage language, String code, String stdin, Long projectId, Long fileId) {
        this.language = language;
        this.code = code;
        this.stdin = stdin;
        this.projectId = projectId;
        this.fileId = fileId;
    }

    public ExecutionRequest(ProgrammingLanguage language, String code, String stdin, Long projectId, Long fileId, Integer timeoutSeconds) {
        this.language = language;
        this.code = code;
        this.stdin = stdin;
        this.projectId = projectId;
        this.fileId = fileId;
        this.timeoutSeconds = timeoutSeconds;
    }

    public ProgrammingLanguage getLanguage() {
        return language;
    }

    public void setLanguage(ProgrammingLanguage language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStdin() {
        return stdin;
    }

    public void setStdin(String stdin) {
        this.stdin = stdin;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getInput() {
        return stdin;
    }

    public void setInput(String input) {
        this.stdin = input;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ProgrammingLanguage language;
        private String code;
        private String stdin;
        private Long projectId;
        private Long fileId;
        private Integer timeoutSeconds;

        public Builder language(ProgrammingLanguage language) {
            this.language = language;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder stdin(String stdin) {
            this.stdin = stdin;
            return this;
        }

        public Builder input(String input) {
            this.stdin = input;
            return this;
        }

        public Builder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder fileId(Long fileId) {
            this.fileId = fileId;
            return this;
        }

        public Builder timeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public ExecutionRequest build() {
            return new ExecutionRequest(language, code, stdin, projectId, fileId, timeoutSeconds);
        }
    }
}
