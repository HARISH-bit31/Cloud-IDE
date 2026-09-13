package com.cloudide.execution.dto;

import jakarta.validation.constraints.NotNull;

public class ExecutionInputRequest {

    @NotNull(message = "Input cannot be null")
    private String input;

    public ExecutionInputRequest() {
    }

    public ExecutionInputRequest(String input) {
        this.input = input;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }
}
