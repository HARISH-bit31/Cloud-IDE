package com.cloudide.execution.dto;

import com.cloudide.execution.enums.ProgrammingLanguage;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ExecutionStartRequest {

    private String executionId;

    @NotNull(message = "Programming language is required")
    private ProgrammingLanguage language;

    @NotBlank(message = "Source code cannot be empty")
    private String code;

    @JsonAlias({"input", "stdinInput"})
    private String stdin;

    private Integer timeoutSeconds;

    public ExecutionStartRequest() {
    }

    public ExecutionStartRequest(String executionId, ProgrammingLanguage language, String code, String stdin, Integer timeoutSeconds) {
        this.executionId = executionId;
        this.language = language;
        this.code = code;
        this.stdin = stdin;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
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

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
