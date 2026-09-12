package com.cloudide.cloudide.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class ExecutionInputRequest {

    @JsonAlias({"stdin", "data"})
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
